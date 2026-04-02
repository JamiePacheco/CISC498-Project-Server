package com.aux_arena.components.lobby;


import com.aux_arena.models.session.GameLobbyMessage;
import com.aux_arena.models.session.GameSession;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
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

    public GameSession startGameSession(Long gameLobbyId, Principal principal) {
        UserSession host = this.getUser(gameLobbyId, principal);

        if (!host.getHost()) {
            throw new RuntimeException(String.format("Cannot start game, user '%s' is not the host", principal.getName()));
        }

        LobbySession lobbySession = this.lobbyManager.startGameLobby(gameLobbyId);
        GameSession gameSession = this.gameSessionManager.loadGameSession(lobbySession);

        return gameSession;
    }
}