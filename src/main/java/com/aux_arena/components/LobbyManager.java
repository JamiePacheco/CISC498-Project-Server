package com.aux_arena.components;

import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LobbyManager {

    private final Map<Long, LobbySession> lobbies = new ConcurrentHashMap<>();
    private final Map<String, UserSession> userSessions = new ConcurrentHashMap<>();

    // TODO get user from database (create if no user exists) then add it to the current lobby
    public LobbySession onUserConnect(Long lobbyId, LobbyUser user) {

        // create lobby if no lobby exists
        LobbySession lobbySession = lobbies.computeIfAbsent(lobbyId, LobbySession::new);
        UserSession addedUser = lobbySession.addUser(user);

        if (addedUser == null) {
            throw new RuntimeException(String.format("Game Lobby %s is full", lobbySession.getLobbyCode()));
        }

        userSessions.put(user.getLastSocketConnectionId(), addedUser);
        return lobbySession;
    }

    public UserSession onUserDisconnect(Long lobbyId, LobbyUser user) {
        LobbySession lobbySession = lobbies.get(lobbyId);
        if (lobbySession == null) {
            throw new RuntimeException(String.format("Game Lobby %s not found", lobbyId));
        }
        UserSession deactivatedUser = lobbySession.disconnectUser(user);


    }








}
