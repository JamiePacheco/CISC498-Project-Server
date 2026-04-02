package com.aux_arena.components.lobby;

import com.aux_arena.models.session.GameSession;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import lombok.Data;
import org.springframework.stereotype.Component;

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
