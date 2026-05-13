package com.aux_arena.components.lobby;

import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.session.GameLobbyMessage;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.tables.GameLobby;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@Component
@Data
public class LobbyManager {

    // mappings for lobbies and users
    private final Map<Long, LobbySession> lobbies = new ConcurrentHashMap<>();
    // TODO probably remove this because it is pretty redundant at this point
    private final Map<String, UserSession> userSessions = new ConcurrentHashMap<>();
    // mutex locks for lobbies
    private final ConcurrentHashMap<Long, ReentrantLock> lobbyLocks = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(LobbyManager.class);

    private ReentrantLock getLock(Long lobbyId) {
        return lobbyLocks.computeIfAbsent(lobbyId, id -> new ReentrantLock());
    }

    public <T> T modifyLobbyAtomically(Long lobbyId, Function<LobbySession, T> task) {
        ReentrantLock lock = getLock(lobbyId);
        lock.lock();
        try {
            LobbySession lobbySession = lobbies.get(lobbyId);
            return task.apply(lobbySession);
        } finally {
            lock.unlock();
        }
    }

    // TODO test to see if this needs to be loaded atomically or not...
    // A race condition shouldn't really matter as if the lobby does not exist then it will always be a unique lobby
    public LobbySession loadGameLobby(GameLobby gameLobby) {
        LobbySession lobbySession = lobbies.get(gameLobby.getId());
        if (lobbySession == null) {
            lobbySession = new LobbySession(gameLobby.getId());
            lobbySession.loadAttributes(gameLobby);
            lobbies.put(gameLobby.getId(), lobbySession);
        }
        return lobbySession;
    }

    public LobbySession startLobbyGame(Long lobbyId) {
        return modifyLobbyAtomically(lobbyId, lobbySession -> {

            if (lobbySession == null) {
                throw new RuntimeException("Error starting game lobby: Game lobby does not exist within memory");
            }
            lobbySession.setStatus(GameLobbyStatus.GAME_IN_PROGRESS);
            lobbySession.setActive(true);
            lobbySession.setLastUpdated(Instant.now());
            // new game session should be populated within memory
            log.info("Lobby {} status updated to in progress", lobbyId);
            return lobbySession;
        });
    }

    public LobbySession endLobbyGame(Long lobbyId) {
        return modifyLobbyAtomically(lobbyId, lobbySession -> {

            if (lobbySession == null) {
                throw new RuntimeException("Error ending game lobby????");
            }

            lobbySession.setStatus(GameLobbyStatus.WAITING);
            lobbySession.setActive(false);
            lobbySession.setLastUpdated(Instant.now());

            return lobbySession;
        });
    }

    // gets the next sequence to use when sending the next lobby event
    public long nextEventSequence(Long lobbyId) {
        return modifyLobbyAtomically(lobbyId, lobbySession -> lobbySession.getGameLobbyEventIndex());
    }

    public UserSession onUserConnect(Long lobbyId, UserSession userSession, Principal principal) {
        // atomically add new user to lobby
         return modifyLobbyAtomically(lobbyId, lobbySession -> {
            UserSession addedUser = lobbySession.addUser(userSession, principal);

            if (!userSessions.containsKey(principal.getName())) {
                userSessions.put(principal.getName(), addedUser);
            }

            return addedUser;
        });
    }

    public UserSession onUserDisconnect(Long lobbyId, Principal principal) {
        return modifyLobbyAtomically(lobbyId, lobbySession -> {
            if (lobbySession == null) {
                throw new RuntimeException(String.format("Game Lobby %s not found", lobbyId));
            }
            UserSession deactivatedUser = lobbySession.disconnectUser(principal.getName());

            if (userSessions.containsKey(principal.getName())) {
                userSessions.remove(principal.getName());
            }

            return deactivatedUser;
        });
    }

    public UserSession promoteUserToHost(Long lobbyId, UserSession newHost, Principal principal) {
        return modifyLobbyAtomically(lobbyId, lobbySession -> {
            if (principal != null && lobbySession.getHost().getTempId() != principal.getName()) {
                throw new RuntimeException("Non-host user cannot initiate host change");
            }

            return lobbySession.assignHost(newHost);
        });
    }



    // TODO fix this function
    public void cleanupInactiveUsers() {
        Instant now = Instant.now();
        Duration timeout = Duration.ofSeconds(30);

        // TODO parallelize this jawn (textbook definition of divide and conquer...)
        for (Long lobbySessionId : lobbies.keySet()) {
            // atomically access the game lobby and send the corresponding event
            modifyLobbyAtomically(lobbySessionId, lobbySession -> {
                List<UserSession> removedPlayers = new ArrayList<>();
                if (!lobbySession.getActiveUsers().isEmpty()) {
                    // remove the inactive users
                    for (String principle : lobbySession.getActiveUsers().keySet()) {
                        UserSession userSession = lobbySession.getActiveUsers().get(principle);
                        log.info("Last pinged user [{} : {}]", userSession.getDisplayName(), userSession.getLastPingTime());
                        if (!userSession.getActive() && Duration.between(userSession.getLastPingTime(), now).compareTo(timeout) > 0) {
                            removedPlayers.add(lobbySession.getActiveUsers().get(principle));
                            lobbySession.removeUser(principle);
                            lobbySession.getActiveUsers().remove(principle);
                            userSessions.remove(principle);
                        }
                    }

                    if (lobbySession.getActiveUsers().isEmpty()) {
                        lobbySession.setActive(false);
                    }

                    // broadcast the disconnected users to the respective lobby
//                    broadcastLobbyEvent(
//                            lobbySession,
//                            removedPlayers,
//                            "Inactive players removed",
//                            MessageEvent.USER_CLEANUP);

                    // broadcast notification message of user clean up to lobby
                    sendGameLobbyMessage(
                            lobbySessionId,
                            GameLobbyMessage.builder()
                                    .textMessage(String.format("Removed %d inactive users", removedPlayers.size()))
                                    .author("SYSTEM")
                                    .build(),
                            null
                    );
                }
                return null;
            });
        }
    }


    public GameLobbyMessage sendGameLobbyMessage(Long lobbyId, GameLobbyMessage gameLobbyMessage, Principal principal) {
        return modifyLobbyAtomically(lobbyId, lobbySession -> {
            if (principal != null) {
                gameLobbyMessage.setAuthorId(principal.getName()); // this is used to fetch user information (tempId -> userSession -> userId -> user)
            }

            GameLobbyMessage newMessage = lobbySession.addNewMessage(gameLobbyMessage);
            return newMessage;
        });

    }


    public GameLobbyMessage sendSystemGameLobbyMessage(Long lobbyId, String message) {
        return this.sendGameLobbyMessage(
                lobbyId,
                GameLobbyMessage.builder()
                        .textMessage(message)
                        .author("SYSTEM")
                        .build(),
                null
        );
    }
}