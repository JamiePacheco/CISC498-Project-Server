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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private List<GameLobbyMessage> messages = new ArrayList<>();
    private UserSession host;

    private boolean active;

    private boolean dirty;
    private long gameLobbyEventIndex = 1L;
    private long gameLobbyMessageIndex = 1L;

    private static final Logger logger = LoggerFactory.getLogger(LobbySession.class);

    public LobbySession(Long id) {
        this.id = id;
        this.lastUpdated = Instant.now();
    }

    public long getGameLobbyEventIndex() {
        long index = gameLobbyEventIndex;
        logger.info("Event index has been incremented to {}", index);
        this.gameLobbyEventIndex++;
        return index;
    }

    public long getGameLobbyMessageIndex() {
        long index = gameLobbyMessageIndex;
        logger.info("Message index has been incremented to {}", index);
        ++this.gameLobbyMessageIndex;
        return index;
    }


    public GameLobbyMessage addNewMessage(GameLobbyMessage gameLobbyMessage) {
        gameLobbyMessage.setMessageIndex(this.getGameLobbyMessageIndex());
        gameLobbyMessage.setTimestamp(Instant.now());
        this.messages.add(gameLobbyMessage);
        logger.info("New message has entered the domain [{}]: {}", gameLobbyMessage.getAuthor(), gameLobbyMessage.getTextMessage());
        return gameLobbyMessage;
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
            addedUser.setTempId(principal.getName());
            addedUser.setSessionId(userSession.getSessionId());
            addedUser.setLastPingTime(Instant.now());
            // user is spectator if there is a full lobby or the lobby is active (in-game)
            addedUser.setIsSpectator(this.getPlayers().size() >= maxPlayers || this.isActive());
            addedUser.setActive(true);
            addedUser.setJoinedAt(Instant.now());
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
                .joinedAt(Instant.now())
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

    public UserSession disconnectUser(String principleName) {
        UserSession connectedUser = activeUsers.get(principleName);
        if (connectedUser != null) {
            connectedUser.setLastPingTime(Instant.now());
            connectedUser.setIsSpectator(true);
            connectedUser.setActive(false);
            connectedUser.setFunctionMessage("Disconnect User");
        }
        return connectedUser;
    }

    // TODO fix issue when there are 2+ players and this throws a nullpointerexception
    public UserSession assignHost(UserSession newHost) {

        if (newHost != null) {
            if (activeUsers.get(newHost.getTempId()) == null) {
                throw new RuntimeException("Error new host does not exist");
            }

            newHost.setHost(true);
            this.host = newHost;
            return this.host;
        }

        Optional<UserSession> oldestUser = activeUsers.values().stream()
                .filter(u -> {
                    logger.info("[{}] {} joined at {}",
                            u.getTempId(),
                            u.getDisplayName(),
                            u.getJoinedAt()
                    );
                    return u.getActive();
                })
                .min(Comparator.comparing(UserSession::getJoinedAt));


        if (oldestUser.isPresent()) {
            UserSession newHostSession = oldestUser.get();
            newHostSession.setHost(true);
            this.host = newHostSession;
            return newHostSession;
        }

        this.host = null;
        return null;
    }

    public void removeUser(String principle) {
        activeUsers.remove(principle);
    }

    public boolean isInactive() {
        return Duration.between(lastUpdated, Instant.now()).toMillis() > 10;
    }
}
