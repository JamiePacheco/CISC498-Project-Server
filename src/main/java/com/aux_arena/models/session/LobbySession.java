package com.aux_arena.models.session;

import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.tables.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LobbySession {
    // basic fields
    private Long id;
    private String lobbyCode;
    private String name;
    private GameLobbyStatus status;
    private int maxPlayers;
    private Instant createdAt;
    private boolean privateStatus;
    private String password;
    private User author;

    private Instant lastUpdated;
    private Map<Long, UserSession> activeUsers = new ConcurrentHashMap<>();

    private boolean dirty;

    public LobbySession(Long id) {
        this.id = id;
    }

    public void addUser(UserSession userSession) {
        activeUsers.compute(userSession.getSessionID(), UserSession::new);
    }

    public boolean isInactive() {
        return Duration.between(lastUpdated, Instant.now()).toMillis() > 10;
    }
}
