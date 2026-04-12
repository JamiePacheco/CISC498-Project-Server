package com.aux_arena.components.lobby;

import com.aux_arena.models.enums.RoundStatus;
import com.aux_arena.models.session.*;
import com.aux_arena.models.session.round.Prompt;
import com.aux_arena.models.session.round.PromptPair;
import com.aux_arena.models.session.round.PromptSubmission;
import com.aux_arena.models.session.round.RoundSession;
import com.aux_arena.models.tables.Game;
import com.aux_arena.models.tables.User;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@Data
@Component
public class GameSessionManager {

    // TODO move all functionality from GameManager into here

    private final Map<Long, GameSession> gameSessions = new ConcurrentHashMap<>();

    private final Map<Long, ReentrantLock> gameSessionLocks = new  ConcurrentHashMap<>();

    // TODO should check for player

    private ReentrantLock getLock(Long gameSessionId) {
        return gameSessionLocks.computeIfAbsent(gameSessionId, id -> new ReentrantLock());
    }

    public <T> T modifyGameSessionAtomically(Long lobbyId, Function<GameSession, T> task) {
        ReentrantLock lock = getLock(lobbyId);
        lock.lock();
        try{
            GameSession gameSession = this.getGameSession(lobbyId);
            return task.apply(gameSession);
        } finally {
            lock.unlock();
        }
    }

    public GameSession getGameSession(Long lobbySessionId) {
        return gameSessions.get(lobbySessionId);
    }

    public GameSession loadGameSession(LobbySession lobbySession, GameSettings gameSettings) {
        GameSession gameSession = gameSessions.get(lobbySession.getId());
        if (gameSession == null) {
            // load new game session
            gameSession = new GameSession(lobbySession, gameSettings);

            gameSessions.put(lobbySession.getId(), gameSession);
        }
        return gameSession;
    }

    public PlayerState getPlayerState(Long gameLobbyId, UserSession userSession) {
        return this.gameSessions
                .get(gameLobbyId)
                .getPlayers()
                .get(userSession.getTempId());
    }

    // used for when a player joins in the middle of the match
    // should be they are always spectators unless they were disconnected and reconnected
    public PlayerState addNewUser(Long lobbyId, UserSession userSession) {
        return modifyGameSessionAtomically(lobbyId, gameSession -> {

            // in this case the user is reconnecting within a valid amount of time so use previous session
            if (gameSession.getPlayers().get(userSession.getTempId()) != null) return null;

            // otherwise connect them, BUT always make them a spectator (things will get messy otherwise
            // maybe later we can make it so upon a new round if the amount of players will get filled by the spectators...
            return gameSession.addNewPlayerState(userSession);
        });
    }

    public Boolean checkReadyStatus(Long gameLobbyId, RoundStatus roundStatus) {
        return modifyGameSessionAtomically(gameLobbyId, gameSession -> gameSession.checkReadyStatus(roundStatus));
    }

    public Prompt submitPrompt(Long gameLobbyId, Prompt prompt) {
        return modifyGameSessionAtomically(gameLobbyId, gameSession -> {

            RoundSession currentRound = this.gameSessions.get(gameLobbyId).getCurrentRound();
            Prompt submittedPrompt = currentRound.submitPrompt(prompt);

            // set the author of prompt to being ready for next game state

            gameSession.getPlayers().get(prompt.getAuthorId()).setReady(true);
            return submittedPrompt;
        });
    }

    public Prompt generatePrompt() {
        return Prompt.builder()
                .prompt("Song when you on a trip with your slimes and run over a grandma") // TODO make a database of pre-generated prompts
                .build();
    }

    public void verifyUserPromptsUnsafe(GameSession gameSession) {
        RoundSession roundSession = gameSession.getCurrentRound();

        // check if prompt amount is same amount as active players
        if (roundSession.getPromptPairs().size() == gameSession.getNonSpectatorPlayers().size()) {
            return;
        }

        // if the player is not ready then they have not submitted a prompt (thus we generate one for them)
        List<PlayerState> notReadyPlayers = gameSession
                .getNonSpectatorPlayers()
                .stream()
                .filter(p -> !p.isReady())
                .toList();

        for (PlayerState playerState : notReadyPlayers) {
            Prompt prompt = this.generatePrompt();
            prompt.setWasGenerated(true);
            prompt.setAuthorId(playerState.getUserSessionId());
            roundSession.submitPrompt(prompt);
        }
    }


    public RoundSession distributePrompts(Long gameLobbyId) {
        return modifyGameSessionAtomically(gameLobbyId, gameSession -> gameSession.distributePrompts());
    }

    public RoundSession distributePrompts(GameSession gameSession) {
        return gameSession.distributePrompts();
    }

    public PromptSubmission submitSongChoice(Long gameLobbyId, PromptSubmission promptSubmission, Principal principal) {
        return modifyGameSessionAtomically(gameLobbyId, gameSession -> {
            PromptPair promptPair = gameSession
                    .getCurrentRound()
                    .getPromptPairs()
                    .get(promptSubmission.getPromptPairId());

            if (promptPair == null) return null;

            promptPair
                    .getPromptSubmissions()
                    .put(principal.getName(), promptSubmission);

            promptSubmission.setSubmittedAt(Instant.now());

            return promptSubmission;
        });
    }

}
