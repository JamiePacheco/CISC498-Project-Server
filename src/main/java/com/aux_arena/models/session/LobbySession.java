package com.aux_arena.models.session;

import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
    private int maxCapacity;
    private Instant createdAt;
    private boolean privateStatus;
    private String password;
    private LobbyUser author;

    private Instant lastUpdated;
    private Map<String, UserSession> activeUsers = new ConcurrentHashMap<>();

    private boolean dirty;

    public LobbySession(Long id) {
        this.id = id;
    }

    public List<UserSession> getPlayers() {
        return activeUsers
                .values()
                .stream()
                .filter(user -> !user.getIsSpectator())
                .toList();
    }

    public void loadAttributes(GameLobby gameLobby) {
        this.lobbyCode = gameLobby.getLobbyCode();
        this.name = gameLobby.getName();
        this.status = gameLobby.getStatus();
        this.maxPlayers = gameLobby.getMaxPlayers();
        this.privateStatus = gameLobby.isPrivateStatus();
        this.password = gameLobby.getPassword();
        this.author = gameLobby.getAuthor();
    }

    public UserSession addUser(LobbyUser lobbyUser) {
        UserSession addedUser = null;

        if (this.activeUsers.size() == maxCapacity) {
            return addedUser;
        }

        addedUser = UserSession.builder()
                .userId(lobbyUser.getId())
                .lobbyId(this.id)
                .displayName(lobbyUser.getNickname())
                .isSpectator(this.getPlayers().size() == maxPlayers)
                .build();

        addedUser = activeUsers.put(lobbyUser.getLastSocketConnectionId(), addedUser);
        return addedUser;
    }

    public void removeUser(UserSession userSession) {
        activeUsers.remove(userSession.getSessionID());
    }

    public boolean isInactive() {
        return Duration.between(lastUpdated, Instant.now()).toMillis() > 10;
    }
}
