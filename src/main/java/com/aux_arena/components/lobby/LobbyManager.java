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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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

    private final Executor broadcastExecutor = Executors.newFixedThreadPool(4);


    public LobbyManager(SimpMessagingTemplate messagingTemplate) {
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

    public LobbySession startGameLobby(Long lobbyId) {
        return modifyLobbyAtomically(lobbyId, lobbySession -> {

            if (lobbySession == null) {
                throw new RuntimeException("Error starting game lobby: Game lobby does not exist within memory");
            }
            lobbySession.setStatus(GameLobbyStatus.GAME_IN_PROGRESS);
            lobbySession.setActive(true);
            lobbySession.setLastUpdated(Instant.now());
            // new game session should be populated within memory
            return lobbySession;
        });
    }

    // TODO get user from database (create if no user exists) then add it to the current lobby
//    public UserSession onUserConnect(Long lobbyId, UserSession user, Principal principal) {
//        return modifyLobbyAtomically(lobbyId, lobbySession -> {
//            if (lobbySession == null) {
//                throw new RuntimeException("Lobby does not exist");
//            }
//
//            UserSession addedUser = lobbySession.addUser(user, principal);
//
//            log.info("added user: {}", addedUser) ;
//
//            if (addedUser == null) {
//                throw new RuntimeException(String.format("Game Lobby %s is full", lobbySession.getLobbyCode()));
//            }
//
//            if (!userSessions.containsKey(principal.getName())) {
//                userSessions.put(principal.getName(), addedUser);
//            }
//
//            for (UserSession userSession : lobbySession.getActiveUsers().values()) {
//                log.info(userSession.toString());
//            }
//
//            broadcastLobbyEventUnsafe(
//                    lobbySession,
//                    addedUser,
//                    String.format("%s has joined", addedUser.getDisplayName()),
//                    MessageEvent.USER_JOINED
//            );
//
//            // Send new connection notification to all users
//            sendGameLobbyMessage(
//                    lobbyId,
//                    GameLobbyMessage.builder()
//                            .textMessage(String.format("%s has connected", addedUser.getDisplayName()))
//                            .author("SYSTEM")
//                            .build(),
//                    null
//            );
//
//            return addedUser;
//        });
//    }

    public UserSession onUserConnect(Long lobbyId, UserSession userSession, Principal principal) {
        record ConnectResult(UserSession userSession, long sequence, GameLobbyMessage message) {}

        // atomically add new user to lobby
        ConnectResult result = modifyLobbyAtomically(lobbyId, lobbySession -> {
            UserSession addedUser = lobbySession.addUser(userSession, principal);
            GameLobbyMessage msg = GameLobbyMessage.builder()
                    .textMessage(String.format("%s has joined", addedUser.getDisplayName()))
                    .author("SYSTEM")
                    .build();
            lobbySession.addNewMessage(msg);

            // get the event index of the new user joining
            long eventIndex = lobbySession.getGameLobbyEventIndex();
            return new ConnectResult(addedUser, eventIndex, msg);
        });

        LobbySession lobbySession = this.getLobbies().get(lobbyId);

        // send new user information to lobby
        broadcastLobbyEvent(
                lobbySession,
                result.userSession,
                result.message.getTextMessage(),
                MessageEvent.USER_JOINED,
                result.sequence
        );

        // send out system message notifying users
        broadcastLobbyMessage(
                lobbySession,
                result.message
        );

        return result.userSession;
    }

    public UserSession onUserDisconnect(Long lobbyId, Principal principal) {
        record DisconnectResult(UserSession userSession, long sequence, GameLobbyMessage message) {}

        DisconnectResult result = modifyLobbyAtomically(lobbyId, lobbySession -> {
            if (lobbySession == null) {
                throw new RuntimeException(String.format("Game Lobby %s not found", lobbyId));
            }
            UserSession deactivatedUser = lobbySession.disconnectUser(principal.getName());

            GameLobbyMessage msg = GameLobbyMessage.builder()
                    .textMessage(String.format("%s has disconnected", deactivatedUser.getDisplayName()))
                    .author("SYSTEM")
                    .build();

            long sequence = lobbySession.getGameLobbyEventIndex();

            // Send disconnection notification to all users

            return new DisconnectResult(deactivatedUser, sequence, msg);
        });

        LobbySession lobbySession = this.getLobbies().get(lobbyId);

        // send new user information to lobby
        broadcastLobbyEvent(
                lobbySession,
                result.userSession,
                result.message.getTextMessage(),
                MessageEvent.USER_LEFT,
                result.sequence
        );

        // send out system message notifying users
        sendSystemGameLobbyMessage(
                lobbyId,
                result.message.getTextMessage()
        );

        return result.userSession;
    }

    public void onUserDisconnect(Principal principal) {
        if (userSessions.get(principal.getName()) == null) return;

        // upon sporadic disconnection the information on the lobby is not known
        Long lobbyId = userSessions.get(principal.getName()).getLobbyId();

        // define how the resulting data should look
        record DisconnectResult(
                UserSession disconnectUser,
                UserSession newHost,
                Long disconnectedSequence,
                Long hostSequence,
                GameLobbyMessage disconnectMessage,
                GameLobbyMessage hostMessage
        ) {}


        // to avoid race conditions we utilize an atomic function to access the in-memory lobby and return resulting data
        DisconnectResult disconnectedResult = modifyLobbyAtomically(lobbyId, lobbySession -> {

            UserSession disconnectedUser = lobbySession.disconnectUser(principal.getName());
            if (disconnectedUser == null) {
                throw new RuntimeException("User not connected to a lobby");
            }
            Long disconnectSequence = lobbySession.getGameLobbyEventIndex();
            GameLobbyMessage disconnectMessage = GameLobbyMessage.builder()
                    .textMessage(String.format("%s has disconnected", disconnectedUser.getDisplayName()))
                    .author("SYSTEM")
                    .build();


            UserSession newHost = null;
            Long hostSequence = null;
            GameLobbyMessage hostMessage = null;
            if (lobbySession.getHost() == disconnectedUser) {
                newHost = lobbySession.assignHost();
                if (newHost != null) {
                    disconnectedUser.setHost(false);
                    hostSequence = lobbySession.getGameLobbyEventIndex();
                    hostMessage = GameLobbyMessage.builder()
                            .textMessage(String.format("%s promoted to host", newHost.getDisplayName()))
                            .author("SYSTEM")
                            .build();
                }
            }

            return new DisconnectResult(disconnectedUser, newHost, disconnectSequence, hostSequence, disconnectMessage, hostMessage);
        });

        LobbySession lobbySession = this.getLobbies().get(lobbyId);

        // broadcast the disconnect user to all players
        broadcastLobbyEvent(
                lobbySession,
                disconnectedResult.disconnectUser,
                disconnectedResult.disconnectMessage.getTextMessage(),
                MessageEvent.USER_LEFT,
                disconnectedResult.disconnectedSequence
        );

        sendSystemGameLobbyMessage(
                lobbyId,
                disconnectedResult.disconnectMessage.getTextMessage()
        );

        // Send new host notification to all users if there was a host reassignment
        if (disconnectedResult.newHost != null) {
            broadcastLobbyEvent(
                    lobbySession,
                    disconnectedResult.newHost,
                    disconnectedResult.hostMessage.getTextMessage(),
                    MessageEvent.NEW_HOST,
                    disconnectedResult.hostSequence
            );

            sendSystemGameLobbyMessage(
                    lobbyId,
                    disconnectedResult.disconnectMessage.getTextMessage()
            );
        }
    }

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
                    broadcastLobbyEvent(
                            lobbySession,
                            removedPlayers,
                            "Inactive players removed",
                            MessageEvent.USER_CLEANUP);

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

    // generic event to broadcast a lobbyUpdate
    public <T> void broadcastLobbyEvent(LobbySession lobbySession, T eventContent, String message, MessageEvent event) {
        GameLobbyEvent<T> lobbyEvent = GameLobbyEvent.<T>builder()
                .type(event)
                .message(message)
                .payload(eventContent)
                .timestamp(Instant.now())
                .sequence(
                        modifyLobbyAtomically(lobbySession.getId(), lobby -> lobby.getGameLobbyEventIndex())
                )
                .build();

        log.info("[Sending message {} to game lobby {}]", lobbyEvent.getType(), lobbySession.getId());

        messagingTemplate.convertAndSend("/topic/game-lobby/" + lobbySession.getId(), lobbyEvent);
    }

    public <T> void broadcastLobbyEvent(
            LobbySession lobbySession,
            T eventContent,
            String message,
            MessageEvent event,
            Long eventSequence
    ) {
        GameLobbyEvent<T> lobbyEvent = GameLobbyEvent.<T>builder()
                .type(event)
                .message(message)
                .payload(eventContent)
                .timestamp(Instant.now())
                .sequence(eventSequence)
                .build();

        // this will run on a separate thread specifically allocated for broadcasting
        CompletableFuture.runAsync(
                () -> messagingTemplate.convertAndSend("/topic/game-lobby/" + lobbySession.getId(), lobbyEvent),
                broadcastExecutor
        );
    }

    // this function is used when we assume the reentrancy lock is already acquired

    public void broadcastLobbyMessage(
            LobbySession lobbySession,
            GameLobbyMessage gameLobbyMessage
    ) {

        GameLobbyEvent<GameLobbyMessage> lobbyMessage = GameLobbyEvent.<GameLobbyMessage>builder()
                .type(MessageEvent.NEW_MESSAGE)
                .message(String.format("%s sent a message", gameLobbyMessage.getAuthor()))
                .payload(gameLobbyMessage)
                .timestamp(Instant.now())
                .sequence(gameLobbyMessage.getMessageIndex())
                .build();

        CompletableFuture.runAsync(
                () ->  messagingTemplate.convertAndSend("/topic/game-lobby/message/" + lobbySession.getId(), lobbyMessage),
                broadcastExecutor
        );
    }

    public void sendGameLobbyMessage(Long lobbyId, GameLobbyMessage gameLobbyMessage, Principal principal) {

        GameLobbyMessage savedMessage = modifyLobbyAtomically(lobbyId, lobbySession -> {
            GameLobbyMessage newMessage = lobbySession.addNewMessage(gameLobbyMessage);
            return newMessage;
        });

        LobbySession lobbySession = this.getLobbies().get(lobbyId);
        broadcastLobbyMessage(lobbySession, savedMessage);
    }

    public void sendGameLobbyMessageUnsafe(LobbySession lobbySession, GameLobbyMessage gameLobbyMessage) {
        GameLobbyMessage newMessage = lobbySession.addNewMessage(gameLobbyMessage);
        broadcastLobbyMessage(lobbySession, newMessage);
    }

    public void sendSystemGameLobbyMessage(Long lobbyId, String message) {
        this.sendGameLobbyMessage(
                lobbyId,
                GameLobbyMessage.builder()
                        .textMessage(message)
                        .author("SYSTEM")
                        .build(),
                null
        );
    }

    // used when we assume calling function has lobby session reentry lock already
    public void sendSystemGameLobbyMessageUnsafe(LobbySession lobbySession, String message) {
        this.sendGameLobbyMessageUnsafe(
                lobbySession,
                GameLobbyMessage.builder()
                        .textMessage(message)
                        .author("SYSTEM")
                        .build()
        );
    }


    // TODO write a function to clean up inactive lobbies.
}