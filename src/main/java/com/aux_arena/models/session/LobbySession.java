package com.aux_arena.models.session;

import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.tables.User;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

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
    private Set<UserSession> activeUsers;

    private boolean dirty;

    public boolean isInactive() {
        return Duration.between(lastUpdated, Instant.now()).toMillis() > 10;
    }
}
