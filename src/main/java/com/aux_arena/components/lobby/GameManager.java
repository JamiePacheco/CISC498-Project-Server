package com.aux_arena.components.lobby;


import com.aux_arena.components.scheduling.PhaseTimerManager;
import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.enums.RoundStatus;
import com.aux_arena.models.enums.message.MessageEvent;
import com.aux_arena.models.session.*;
import com.aux_arena.models.session.round.*;
import com.aux_arena.models.tables.Game;
import com.aux_arena.models.tables.GameLobby;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

@Data
@Component
public class GameManager {
    private final GameSessionManager gameSessionManager;
    private final LobbyManager lobbyManager;
    private final PhaseTimerManager phaseTimerManager;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public GameManager(
            GameSessionManager gameSessionManager,
            LobbyManager lobbyManager,
            PhaseTimerManager phaseTimerManager
    ) {
        this.phaseTimerManager = phaseTimerManager;
        this.gameSessionManager = gameSessionManager;
        this.lobbyManager = lobbyManager;
    }

    public UserSession getUser(Long gameLobbyId, Principal principal) {
        UserSession user = lobbyManager
                .getLobbies()
                .get(gameLobbyId)
                .getActiveUsers()
                .get(principal.getName());

        if (user == null) {
            throw new RuntimeException(
                    String.format("No user connected to lobby %d with id %s", gameLobbyId, principal.getName())
            );
        }
        return user;
    }

    // function that acquires both locks from lobby and game session to prevent race conditions and
    // dependencies between them from changing causing race conditons
    public <T> T withBothLocks(Long lobbyId, BiFunction<GameSession, LobbySession, T> actions) {

        ReentrantLock lobbyLock = this.gameSessionManager.getGameSessionLocks().get(lobbyId);
        ReentrantLock gameLock = this.lobbyManager.getLobbyLocks().get(lobbyId);

        lobbyLock.lock();
        try {
            gameLock.lock();
            try {
                return actions.apply(
                        this.gameSessionManager.getGameSession(lobbyId),
                        this.lobbyManager.getLobbies().get(lobbyId)
                );
            } finally {
                gameLock.unlock();
            }
        } finally {
            lobbyLock.unlock();
        }
    }

    public void scheduleLobbyPhase(Long lobbyId) {

        GameSession gameSession = this.gameSessionManager.getGameSession(lobbyId);
        RoundStatus roundStatus = gameSession.getCurrentRound().getRoundStatus();

        switch (roundStatus) {
            case WRITING_PROMPT:
                this.phaseTimerManager.schedulePhase(
                        lobbyId,
                        () -> this.startSelectMusicPhase(lobbyId),
                        roundStatus.defaultDuration
                );
                break;


        }
    }

    //TODO more precise logic needs to be added here (logic that manages gameSession instance)

    public UserSession connectUser(long gameLobbyId, UserSession newUserSession, Principal principal) {

        UserSession connectedUserSession = this.lobbyManager.onUserConnect(gameLobbyId, newUserSession, principal);
        if (this.lobbyManager.getLobbies().get(gameLobbyId).getStatus() == GameLobbyStatus.GAME_IN_PROGRESS) {
            PlayerState playerState = this.gameSessionManager.addNewUser(gameLobbyId, connectedUserSession);
        }


        return connectedUserSession;
    }

    //TODO more precise logic needs to be added here (logic that manages gameSession instance)
    public UserSession disconnectUser(Long gameLobbyId, Principal principal) {
        UserSession disconnectedUserSession = this.lobbyManager.onUserDisconnect(gameLobbyId, principal);
        return disconnectedUserSession;
    }

    public void sendLobbyMessage(Long gameLobbyId, GameLobbyMessage gameLobbymessage, Principal principal) {
        this.lobbyManager.sendGameLobbyMessage(gameLobbyId, gameLobbymessage, principal);
    }

    public GameSession startGameSession(Long gameLobbyId, GameSettings gameSettings, Principal principal) {
        UserSession host = this.getUser(gameLobbyId, principal);

        if (!host.getHost()) {
            throw new RuntimeException(String.format("Cannot start game, user '%s' is not the host", principal.getName()));
        }

        LobbySession lobbySession = this.lobbyManager.startGameLobby(gameLobbyId);
        GameSession gameSession = this.gameSessionManager.loadGameSession(lobbySession, gameSettings);

        // we set the timer for the next phase if game is timed
        if (gameSession.getGameSettings().isTimed()) {
            this.scheduleLobbyPhase(gameLobbyId);
        }

        this.lobbyManager.sendSystemGameLobbyMessage(gameLobbyId, "Starting Game");

        return gameSession;
    }

    public Prompt submitRoundPrompt(Long gameLobbyId, Prompt prompt, Principal principal) {

        UserSession userSession = this.getUser(gameLobbyId, principal);

        if (userSession == null) throw new RuntimeException("User [" + principal.getName() +  "] is not within lobby");

        // get the user's player state and assign it to the author of the prompt
        prompt.setAuthorId(this.gameSessionManager.getPlayerState(gameLobbyId, userSession).getUserSessionId());

        // submit prompt to the current round
        Prompt submittedPrompt = this.gameSessionManager.submitPrompt(gameLobbyId, prompt);

        this.lobbyManager.sendSystemGameLobbyMessage(
                gameLobbyId,
                String.format("%s has submitted their prompt", userSession.getDisplayName())
        );

        // check if all players have submitted prompts, if so set phase to choosing song
        if (this.gameSessionManager.checkReadyStatus(gameLobbyId, RoundStatus.CHOOSING_SONG)) {
            // if it is ready we then schedule the lobbyPhase

            startSelectMusicPhase(gameLobbyId);
        }

        return submittedPrompt;
    }

    public void startSelectMusicPhase(Long lobbySessionId) {
        withBothLocks(lobbySessionId, (gameSession, lobbySession) -> {
            // first want to check if every user has submitted a prompt (need a certain amount)
            this.gameSessionManager.verifyUserPromptsUnsafe(gameSession);

            // distribute the prompts to the users
            RoundSession roundSession = this.gameSessionManager.distributePrompts(gameSession);

            // send the assigned prompts to the lobby users
            this.lobbyManager.broadcastLobbyEventUnsafe(
                    lobbySession,
                    roundSession,
                    "Prompts Received!",
                    MessageEvent.PROMPT_ASSIGNED
            );

            // system notification
            this.lobbyManager.sendSystemGameLobbyMessageUnsafe(lobbySession, "All Prompts Received!");

            return null;
        });
    }

    public PromptSubmission submitSongChoice(Long gameLobbyId, PromptSubmission promptSubmission, Principal principal) {

        UserSession userSession = this.getUser(gameLobbyId, principal);

        PromptSubmission submittedSong = this.gameSessionManager.submitSongChoice(gameLobbyId, promptSubmission, principal);

        PlayerState playerState = this.gameSessionManager.getPlayerState(gameLobbyId, userSession);
        playerState.getPromptSubmissions().add(submittedSong);

        // check if player has responded to proper number of prompts
        // if we make it so the prompts are variable this has to be changed to number of responses need
        if (playerState.getPromptSubmissions().size() == 2) {
            playerState.setReady(true);
        }

        // check if all the players are ready to move on to presenting stage (all have submitted valid responses)
        if (this.gameSessionManager.checkReadyStatus(gameLobbyId, RoundStatus.PRESENTING)) {

            // need to start presenting the prompts (PRESENTING -> VOTING -> RESULTS and loop until finished)
            // only need to start the process here
            withBothLocks(gameLobbyId, (gameSession, lobbySession) -> {

                RoundSession roundSession = gameSession.getCurrentRound();

                // get first prompt to display
                PromptPair firstPrompt = roundSession.getPromptToDisplay();

                if (firstPrompt == null) throw new RuntimeException("First prompt is null (for some reason...)");

                // TODO add proper event and messaging broadcasting

                return null;
            });


            /* TODO finish implementing phase transition
                - implement proper broadcasting logic using specific broadcast executor
                - ensure that proper logic for
                    a.) everyone is ready so we continue and make sure to cancel scheduled event and manually progress
                    b.) time ran out and we continue as normal while also scheduling next event
                    c.) ensure proper messages are being sent to users that tell the time frames
                - implement vote submission endpoint
                    - add another option where both songs are ass
            */
        }

        return submittedSong;
    }
}