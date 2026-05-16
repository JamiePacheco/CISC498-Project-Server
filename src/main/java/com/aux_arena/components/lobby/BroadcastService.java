package com.aux_arena.components.lobby;

import com.aux_arena.models.enums.message.MessageEvent;
import com.aux_arena.models.enums.message.MessageStatus;
import com.aux_arena.models.enums.message.UserEventType;
import com.aux_arena.models.session.GameLobbyMessage;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.socket.event.UserEvent;
import com.aux_arena.models.socket.event.GameLobbyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// handle broadcast events within socket architectureBr
@Component
public class BroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    private final Executor broadcastExecutor = Executors.newFixedThreadPool(4);

    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);

    public BroadcastService(SimpMessagingTemplate simpMessagingTemplate) {
        this.messagingTemplate = simpMessagingTemplate;
    }

    public <T> CompletableFuture<Void> broadcastLobbyEvent(
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

        log.info(String.format("[Lobby %d] %s %s %d", lobbySession.getId(), lobbyEvent.getType(), lobbyEvent.getMessage(), lobbyEvent.getSequence()));

        // this will run on a separate thread specifically allocated for broadcasting
        return CompletableFuture.runAsync(
                () -> messagingTemplate.convertAndSend("/topic/game-lobby/" + lobbySession.getId(), lobbyEvent),
                broadcastExecutor
        ).exceptionally(ex -> {
            log.error(
                    "Failed to broadcast event {} to lobby {}: {}",
                    eventContent, lobbySession.getId(), ex.getMessage(), ex
            );
            return null;
        });
    }

    public <T> CompletableFuture<Void> broadcastUserEvent(
            LobbySession lobbySession,
            UserSession userSession,
            T eventContent,
            String message,
            UserEventType userEventType
    ) {
        UserEvent<T> userEvent = UserEvent.<T>builder()
                .Message(String.format("successfully left lobby"))
                .messageStatus(MessageStatus.SUCCESS)
                .messageContent(eventContent)
                .userEventType(userEventType)
                .build();

        log.info(String.format("[Lobby %d] %s %s", lobbySession.getId(), userEvent.getUserEventType(), userEvent.getMessage()));

        // this will run on a separate thread specifically allocated for broadcasting
        return CompletableFuture.runAsync(
                () -> messagingTemplate.convertAndSendToUser(
                        userSession.getTempId(),
                        "/queue/game-lobby/" + lobbySession.getId(),
                        userEvent
                ),
                broadcastExecutor
        ).exceptionally(ex -> {
            log.error(
                    "Failed to broadcast event {} to lobby {}: {}",
                    eventContent, lobbySession.getId(), ex.getMessage(), ex
            );
            return null;
        });
    }

    public CompletableFuture<Void> broadcastLobbyMessage(
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

        log.info(String.format("[Lobby %d] %s %s", lobbySession.getId(), lobbyMessage.getType(), lobbyMessage.getMessage()));

        return CompletableFuture.runAsync(
                () ->  messagingTemplate.convertAndSend("/topic/game-lobby/message/" + lobbySession.getId(), lobbyMessage),
                broadcastExecutor
        );
    }

}