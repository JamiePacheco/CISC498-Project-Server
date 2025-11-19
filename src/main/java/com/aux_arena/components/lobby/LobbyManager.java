package com.aux_arena.components.lobby;

import com.aux_arena.models.enums.message.MessageEvent;
import com.aux_arena.models.enums.message.MessageType;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.socket.Message;
import com.aux_arena.models.socket.event.GameLobbyEvent;
import com.aux_arena.models.tables.LobbyUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LobbyManager {

    private final Map<Long, LobbySession> lobbies = new ConcurrentHashMap<>();
    private final Map<String, UserSession> userSessions = new ConcurrentHashMap<>();

    // for sending messages to lobby channel
    private SimpMessagingTemplate messagingTemplate;

    // TODO get user from database (create if no user exists) then add it to the current lobby
    public LobbySession onUserConnect(Long lobbyId, LobbyUser user) {

        // create lobby if no lobby exists
        LobbySession lobbySession = lobbies.computeIfAbsent(lobbyId, LobbySession::new);
        UserSession addedUser = lobbySession.addUser(user);

        if (addedUser == null) {
            throw new RuntimeException(String.format("Game Lobby %s is full", lobbySession.getLobbyCode()));
        }

        userSessions.put(user.getLastSocketConnectionId(), addedUser);
        return lobbySession;
    }

    public UserSession onUserDisconnect(Long lobbyId, LobbyUser user) {
        LobbySession lobbySession = lobbies.get(lobbyId);
        if (lobbySession == null) {
            throw new RuntimeException(String.format("Game Lobby %s not found", lobbyId));
        }
        UserSession deactivatedUser = lobbySession.disconnectUser(user);
        return deactivatedUser;
    }

    public void onUserDisconnect(String sessionId) {
        LobbySession lobbySession = lobbies.get(userSessions.get(sessionId).getLobbyId());
        UserSession disconnectedUser = lobbySession.disconnectUser(sessionId);
        if (disconnectedUser == null) {
            throw new RuntimeException("User not connected to a lobby");
        }
    }

    public void cleanupInactiveUsers() {
        Instant now = Instant.now();
        Duration timeout = Duration.ofMinutes(2);

        for (LobbySession lobbySession : lobbies.values()) {
            if (!lobbySession.getActiveUsers().isEmpty()) {
                //get the inactive users
                List<UserSession> inactiveUsers = new  ArrayList<>();

                // remove the inactive users
                for (UserSession user : lobbySession.getActiveUsers().values()) {
                    if (Duration.between(user.getLastPingTime(), now).compareTo(timeout) > 0) {
                        lobbySession.removeUser(user.getSessionId());
                        lobbySession.getActiveUsers().remove(user.getSessionId());
                        userSessions.remove(user.getSessionId());
                        inactiveUsers.add(user);
                    }
                }
                // broadcast the disconnected users to the respective lobby
                broadCastLobbyUpdate(lobbySession, inactiveUsers);
            }
        }
    }

    private void broadCastLobbyUpdate(LobbySession lobbySession, List<UserSession> inactiveUsers) {
        GameLobbyEvent<List<UserSession>> lobbyEvent = GameLobbyEvent.<List<UserSession>>builder()
                .type(MessageEvent.USER_CLEANUP)
                .message(String.format("%d inactive users removed", inactiveUsers.size()))
                .payload(inactiveUsers)
                .timestamp(Instant.now())
                .build();

        messagingTemplate.convertAndSend("/topic/game-lobby." + lobbySession.getId(), lobbyEvent);
    }

}