package com.aux_arena.components;

import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.tables.GameLobby;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LobbyManager {

    private final Map<String, GameLobby> lobbies = new ConcurrentHashMap<>();
    private final Map<String, UserSession> userSessions = new ConcurrentHashMap<>();

}
