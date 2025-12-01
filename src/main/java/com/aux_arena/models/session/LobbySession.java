package com.aux_arena.models.session;

import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.utility.UuidGenerator;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
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
    private UserSession host;

    private boolean active;

    private boolean dirty;

    private long gameLobbyEventIndex = 1L;

    public LobbySession(Long id) {
        this.id = id;
        this.lastUpdated = Instant.now();
    }

    public long getGameLobbyEventIndex() {
        long index = gameLobbyEventIndex;
        this.gameLobbyEventIndex++;
        return index;
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
        this.id = gameLobby.getId();
        this.name = gameLobby.getName();
        this.status = gameLobby.getStatus();
        this.maxPlayers = gameLobby.getMaxPlayers();
        this.privateStatus = gameLobby.isPrivateStatus();
        this.password = gameLobby.getPassword();
        this.author = gameLobby.getAuthor();
        this.maxCapacity = gameLobby.getMaxCapacity();
    }

    public UserSession addUser(UserSession userSession, Principal principal) {
        UserSession addedUser = null;
        if (this.activeUsers.size() == maxCapacity) return null;

        addedUser = this.activeUsers.get(principal.getName());
        if (addedUser != null) {
            addedUser.setTempId(userSession.getTempId());
            addedUser.setSessionId(userSession.getSessionId());
            addedUser.setLastPingTime(Instant.now());
            addedUser.setIsSpectator(this.getPlayers().size() == maxPlayers);
            addedUser.setActive(true);
            addedUser.setFunctionMessage("Reconnect User");

            if (this.getHost() == null) {
                this.setHost(addedUser);
                addedUser.setHost(true);
            }

            return addedUser;
        }

        addedUser = UserSession.builder()
                .userId(userSession.getUserId()) // this is probably null
                .tempId(userSession.getTempId())
                .lobbyId(this.id)
                .lobbyCode(userSession.getLobbyCode())
                .displayName(userSession.getDisplayName())
                .isSpectator(this.getPlayers().size() == maxPlayers) // TODO this should also depend on isSpector property
                .lastPingTime(Instant.now())
                .active(true)
                .sessionId(userSession.getSessionId())
                .functionMessage("Connect User")
                .build();

        if (this.getHost() == null) {
            this.setHost(addedUser);
            addedUser.setHost(true);
        }

        UserSession finalAddedUser = addedUser;
        if (!this
                .getActiveUsers()
                .values()
                .stream()
                .filter(u -> u.getDisplayName().equals(finalAddedUser.getDisplayName())
                ).toList().isEmpty()
        ) {
            Random rand = new Random();
            finalAddedUser.setDisplayName(finalAddedUser.getDisplayName() + rand.nextInt(9999));
        }

        // use the socket principal to get user (this allows for reconnection without duplicate users)
        activeUsers.put(principal.getName(), finalAddedUser);
        return finalAddedUser;
    }

    public UserSession disconnectUser(LobbyUser lobbyUser) {
        UserSession connectedUser = activeUsers.get(lobbyUser.getLastSocketConnectionId());
        if (connectedUser != null) {
            connectedUser.setLastPingTime(Instant.now());
            connectedUser.setIsSpectator(false);
        }
        return connectedUser;
    }

    public UserSession disconnectUser(String principleName) {
        UserSession connectedUser = activeUsers.get(principleName);
        if (connectedUser != null) {
            connectedUser.setLastPingTime(Instant.now());
            connectedUser.setIsSpectator(false);
            connectedUser.setActive(false);
            connectedUser.setFunctionMessage("Disconnect User");
        }
        return connectedUser;
    }

    // TODO fix issue when there are 2+ players and this throws a nullpointerexception
    public UserSession assignHost() {
        List<UserSession> orderedUsers = this.activeUsers.values().stream().filter(u -> u.getActive()).sorted(
                Comparator.comparing(u1 -> u1.getJoinedAt())
        ).toList();

        if (orderedUsers.isEmpty()) {
            return null;
        }

        UserSession newHost = orderedUsers.get(0);
        newHost.setHost(true);
        this.host = newHost;
        return newHost;
    }


    public void removeUser(String socketId) {
        activeUsers.remove(socketId);
    }



    public boolean isInactive() {
        return Duration.between(lastUpdated, Instant.now()).toMillis() > 10;
    }
}
