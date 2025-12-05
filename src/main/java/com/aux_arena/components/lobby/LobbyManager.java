package com.aux_arena.components.lobby;

import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.enums.message.MessageEvent;
import com.aux_arena.models.session.GameLobbyMessage;
import com.aux_arena.models.session.GameSession;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.socket.event.GameLobbyEvent;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    // for sending messages to lobby channel
    private SimpMessagingTemplate messagingTemplate;

    private GameManager gameManager;

    private static final Logger log = LoggerFactory.getLogger(LobbyManager.class);


    public LobbyManager(SimpMessagingTemplate messagingTemplate) {
//        this.gameLobbyService = gameLobbyService;
        this.messagingTemplate = messagingTemplate;
    }

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

    // TODO finish implementing starting game lobby
    public LobbySession startGameLobby(Long lobbyId) {
        return modifyLobbyAtomically(lobbyId, lobbySession -> {

            if (lobbySession == null) {
                throw new RuntimeException("Error starting game lobby: Game lobby does not exist within memory");
            }
            lobbySession.setStatus(GameLobbyStatus.GAME_IN_PROGRESS);
            lobbySession.setLastUpdated(Instant.now());

            // new game session should be populated within memory
            gameManager.loadGameSession(lobbySession);
            return lobbySession;
        });
    }

    // TODO get user from database (create if no user exists) then add it to the current lobby
    public UserSession onUserConnect(Long lobbyId, UserSession user, Principal principal) {
        return modifyLobbyAtomically(lobbyId, lobbySession -> {
            if (lobbySession == null) {
                throw new RuntimeException("Lobby does not exist");
            }

            UserSession addedUser = lobbySession.addUser(user, principal);

            log.info("added user: {}", addedUser) ;

            if (addedUser == null) {
                throw new RuntimeException(String.format("Game Lobby %s is full", lobbySession.getLobbyCode()));
            }

            if (!userSessions.containsKey(principal.getName())) {
                userSessions.put(principal.getName(), addedUser);
            }

            for (UserSession userSession : lobbySession.getActiveUsers().values()) {
                log.info(userSession.toString());
            }

            broadcastLobbyEvent(
                    lobbySession,
                    addedUser,
                    String.format("%s has joined", addedUser.getDisplayName()),
                    MessageEvent.USER_JOINED
            );


            return addedUser;
        });
    }

    public UserSession onUserDisconnect(Long lobbyId, Principal principal) {
        return modifyLobbyAtomically(lobbyId, lobbySession -> {
            if (lobbySession == null) {
                throw new RuntimeException(String.format("Game Lobby %s not found", lobbyId));
            }
            UserSession deactivatedUser = lobbySession.disconnectUser(principal.getName());

            for (UserSession userSession : lobbySession.getActiveUsers().values()) {
                log.info(userSession.toString());
            }

            return deactivatedUser;
        });
    }

    public void sendGameLobbyMessage(Long lobbyId, GameLobbyMessage gameLobbyMessage, Principal principal) {
        modifyLobbyAtomically(lobbyId, lobbySession -> {
            GameLobbyMessage newMessage = lobbySession.addNewMessage(gameLobbyMessage);
            broadcastLobbyMessage(lobbySession, newMessage);
            return lobbySession;
        });
    }

    public void onUserDisconnect(Principal principal) {
        Long lobbyId = userSessions.get(principal.getName()).getLobbyId();

        // to avoid race conditions we utilize an atomic function to access the in-memory lobby
        modifyLobbyAtomically(lobbyId, lobbySession -> {
            UserSession disconnectedUser = lobbySession.disconnectUser(principal.getName());
            if (disconnectedUser == null) {
                throw new RuntimeException("User not connected to a lobby");
            }

            if (lobbySession.getHost() == disconnectedUser) {
                UserSession newHost = lobbySession.assignHost();
                if (newHost != null) {
                    disconnectedUser.setHost(false);
                    broadcastLobbyEvent(
                            lobbySession,
                            newHost,
                            String.format("%s promoted to host", newHost.getDisplayName()),
                            MessageEvent.NEW_HOST);
                }
            }

            broadcastLobbyEvent(
                    lobbySession,
                    disconnectedUser,
                    String.format("%s disconnected", disconnectedUser.getDisplayName()),
                    MessageEvent.USER_LEFT);

            return null;
        });
    }

    public void cleanupInactiveUsers() {
        Instant now = Instant.now();
        Duration timeout = Duration.ofMinutes(2);

        // TODO parallelize this jawn (textbook definition of divide and conquer...)
        for (Long lobbySessionId : lobbies.keySet()) {
            // atomically access the game lobby and send the corresponding event
            modifyLobbyAtomically(lobbySessionId, lobbySession -> {
                if (!lobbySession.getActiveUsers().isEmpty()) {
                    //get the inactive users
                    List<UserSession> inactiveUsers = new  ArrayList<>();

                    // remove the inactive users
                    for (String principle : lobbySession.getActiveUsers().keySet()) {
                        UserSession userSession = lobbySession.getActiveUsers().get(principle);
                        log.info("Last pinged user: {}", userSession.getLastPingTime());
                        if (Duration.between(userSession.getLastPingTime(), now).compareTo(timeout) > 0) {
                            lobbySession.removeUser(principle);
                            lobbySession.getActiveUsers().remove(principle);
                            userSessions.remove(principle);
                            inactiveUsers.add(userSession);
                        }
                    }

                    if (lobbySession.getActiveUsers().isEmpty()) {
                        lobbySession.setActive(false);
                    }

                    // broadcast the disconnected users to the respective lobby
                    broadcastLobbyEvent(
                            lobbySession,
                            inactiveUsers,
                            "Inactive players removed",
                            MessageEvent.USER_CLEANUP);
                }
                return null;
            });
        }
    }

    // generic event to broadcast a lobbyUpdate
    private <T> void broadcastLobbyEvent(LobbySession lobbySession, T eventContent, String message, MessageEvent event) {
        GameLobbyEvent<T> lobbyEvent = GameLobbyEvent.<T>builder()
                .type(event)
                .message(message)
                .payload(eventContent)
                .timestamp(Instant.now())
                .sequence(
                        modifyLobbyAtomically(lobbySession.getId(), lobby -> lobby.getGameLobbyEventIndex())
                )
                .build();

        messagingTemplate.convertAndSend("/topic/game-lobby/" + lobbySession.getId(), lobbyEvent);
    }

    private void broadcastLobbyMessage(LobbySession lobbySession, GameLobbyMessage gameLobbyMessage) {
        GameLobbyEvent<GameLobbyMessage> lobbyEvent = GameLobbyEvent.<GameLobbyMessage>builder()
                .type(MessageEvent.NEW_MESSAGE)
                .message(String.format("%s sent a message", gameLobbyMessage.getAuthor()))
                .payload(gameLobbyMessage)
                .timestamp(Instant.now())
                .sequence(gameLobbyMessage.getMessageIndex())
                .build();

        messagingTemplate.convertAndSend("/topic/game-lobby/message" + lobbySession.getId(), lobbyEvent);
    }


    // TODO write a function to clean up inactive lobbies.
}