package com.aux_arena.components.lobby;


import com.aux_arena.models.enums.RoundStatus;
import com.aux_arena.models.enums.message.MessageEvent;
import com.aux_arena.models.session.GameLobbyMessage;
import com.aux_arena.models.session.GameSession;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.session.round.Prompt;
import com.aux_arena.models.session.round.RoundSession;
import com.aux_arena.models.tables.Game;
import com.aux_arena.models.tables.GameLobby;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Component
public class GameManager {
    private GameSessionManager gameSessionManager;

    private LobbyManager lobbyManager;

    public GameManager(GameSessionManager gameSessionManager, LobbyManager lobbyManager) {

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

    public UserSession connectUser(long gameLobbyId, UserSession newUserSession, Principal principal) {
        UserSession connectedUserSession = this.lobbyManager.onUserConnect(gameLobbyId, newUserSession, principal);
        return connectedUserSession;
    }

    public UserSession disconnectUser(Long gameLobbyId, Principal principal) {
        UserSession disconnectedUserSession = this.lobbyManager.onUserDisconnect(gameLobbyId, principal);
        return disconnectedUserSession;
    }

    public void sendLobbyMessage(Long gameLobbyId, GameLobbyMessage gameLobbymessage, Principal principal) {
        this.lobbyManager.sendGameLobbyMessage(gameLobbyId, gameLobbymessage, principal);
    }

    public Prompt submitRoundPrompt(Long gameLobbyId, Prompt prompt, Principal principal) {

        UserSession userSession = this.getUser(gameLobbyId, principal);

        if (userSession == null) throw new RuntimeException("User [" + principal.getName() +  "] is not within lobby");

        // get the user's player state and assign it to the author of the prompt
        prompt.setAuthor(this.gameSessionManager.getPlayerState(gameLobbyId, userSession));

        // submit prompt to the current round
        Prompt submittedPrompt = this.gameSessionManager.submitPrompt(gameLobbyId, prompt);

        this.lobbyManager.sendSystemGameLobbyMessage(
                gameLobbyId,
                String.format("%s has submitted their prompt", userSession.getDisplayName())
        );

        // check if all players have submitted prompts, if so set phase to choosing song
        if (this.gameSessionManager.checkReadyStatus(gameLobbyId, RoundStatus.CHOOSING_SONG)) {

            // need to distribute the prompts to the players
            RoundSession roundSession = this.gameSessionManager.distributePrompts(gameLobbyId);

            LobbySession lobbySession = this.lobbyManager.getLobbies().get(gameLobbyId);

            // send the assigned prompts to the lobby users
            this.lobbyManager.broadcastLobbyEvent(
                    lobbySession,
                    roundSession,
                    "All Prompts Received!",
                    MessageEvent.PROMPT_ASSIGNED
            );

            // system notification
            this.lobbyManager.sendSystemGameLobbyMessage(gameLobbyId, "All Prompts Received!");
        }

        return submittedPrompt;
    }

    public GameSession startGameSession(Long gameLobbyId, Principal principal) {
        UserSession host = this.getUser(gameLobbyId, principal);

        if (!host.getHost()) {
            throw new RuntimeException(String.format("Cannot start game, user '%s' is not the host", principal.getName()));
        }

        LobbySession lobbySession = this.lobbyManager.startGameLobby(gameLobbyId);
        GameSession gameSession = this.gameSessionManager.loadGameSession(lobbySession);

        this.lobbyManager.sendSystemGameLobbyMessage(gameLobbyId, "Starting Game");

        return gameSession;
    }
}