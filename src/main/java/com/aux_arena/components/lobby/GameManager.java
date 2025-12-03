package com.aux_arena.components.lobby;


import com.aux_arena.models.session.GameSession;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.tables.Game;
import com.aux_arena.models.tables.GameLobby;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Component
public class GameManager {
    private Map<Long, GameSession> gameSessions = new ConcurrentHashMap<>();

    public GameSession loadGameSession(LobbySession lobbySession) {
        GameSession gameSession = gameSessions.get(lobbySession.getId());
        if (gameSession == null) {
            // load new game session
            gameSession = new GameSession(lobbySession);
            gameSessions.put(lobbySession.getId(), gameSession);
        }
        return gameSession;
    }








}
