package com.aux_arena.components;

import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.tables.GameLobby;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LobbyManager {

    private final Map<Long, LobbySession> lobbies = new ConcurrentHashMap<>();
    private final Map<Long, UserSession> userSessions = new ConcurrentHashMap<>();

    // TODO get user from database (create if no user exists) then add it to the current lobby
    public void onUserConnect(Long lobbyId, Long sessionId, Long userId) {
        LobbySession lobbySession = lobbies.computeIfAbsent(lobbyId, LobbySession::new);

    }


}
