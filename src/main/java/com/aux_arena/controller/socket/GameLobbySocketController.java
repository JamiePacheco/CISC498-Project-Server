package com.aux_arena.controller.socket;

import com.aux_arena.components.lobby.LobbyManager;
import com.aux_arena.models.enums.message.MessageEvent;
import com.aux_arena.models.enums.message.MessageStatus;
import com.aux_arena.models.enums.message.MessageType;
import com.aux_arena.models.session.GameLobbyMessage;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.socket.Message;
import com.aux_arena.models.socket.event.GameLobbyEvent;
import com.aux_arena.models.tables.LobbyUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.Instant;

@Slf4j
@Controller
public class GameLobbySocketController {

    private LobbyManager lobbyManager;

    public GameLobbySocketController(
            LobbyManager lobbyManager,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.lobbyManager = lobbyManager;
        this.messagingTemplate = messagingTemplate;
    }

    // used to exhibit better control over where messages and responses are sent.
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("game-lobby/join/{game-lobby-id}")
    public void joinGameLobby(
            Principal principal,
            @DestinationVariable(value = "game-lobby-id") Long gameLobbyId,
            @RequestParam(value = "lobby-password") String lobbyPassword,
            @Payload UserSession userSession,
            MessageHeaders messageHeaders
    ) {
        try {
            String sessionId = (String) messageHeaders.get("simpSessionId");
            userSession.setSessionId(sessionId);
            UserSession newUserSession = this.lobbyManager.onUserConnect(gameLobbyId, userSession, principal);

            Message<UserSession> userSessionMessage = Message.<UserSession>builder()
                    .Message(String.format("%s joined game lobby", newUserSession.getDisplayName()))
                    .messageStatus(MessageStatus.SUCCESS)
                    .messageContent(newUserSession)
                    .messageType(MessageType.USER_UPDATE)
                    .sequence(newUserSession.getUserEventSequence())
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/game-lobby/" + gameLobbyId, userSessionMessage);

        } catch (Exception ex) {
            Message<String> message = Message.<String>builder()
                    .errorMessage(ex.getMessage())
                    .messageContent(ex.getMessage())
                    .messageStatus(MessageStatus.FAILED)
                    .messageType(MessageType.LOBBY_UPDATE)
                    .sequence(0L)
                    .build();

            log.info("Principle Name: {}", principal.getName());
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", message);
        }
    }

    @MessageMapping("game-lobby/send-message/{game-lobby-id}")
    public void sendGameLobbyMessage(
            Principal principal,
            @DestinationVariable(value = "game-lobby-id") Long gameLobbyId,
            @Payload GameLobbyMessage gameLobbyMessage,
            MessageHeaders messageHeaders
    ) {
        try {
            this.lobbyManager.sendGameLobbyMessage(gameLobbyId, gameLobbyMessage, principal);
        } catch (Exception ex) {
            Message<String> message = Message.<String>builder()
                    .errorMessage(ex.getMessage())
                    .messageContent(ex.getMessage())
                    .messageStatus(MessageStatus.FAILED)
                    .messageType(MessageType.LOBBY_UPDATE)
                    .sequence(0L)
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", message);
        }
    }

    // this will be the last call before the subscriber is unsubscribed to
    @MessageMapping("game-lobby/leave/{game-lobby-id}")
    public void leaveGameLobby(
            Principal principal,
            @DestinationVariable(value = "game-lobby-id") Long gameLobbyId,
            @Payload LobbyUser lobbyUser,
            MessageHeaders messageHeaders
    ) {
        try {
            String sessionId = (String) messageHeaders.get("simpSessionId");
            lobbyUser.setLastSocketConnectionId(sessionId);
            UserSession disconnectedUser = this.lobbyManager.onUserDisconnect(gameLobbyId, principal);

            // we send the current snapshot of the lobby so it's bare details can stored to rejoin (not needed but good for display)
            Message<LobbySession> message = Message.<LobbySession>builder()
                    .Message(String.format("successfully left lobby", lobbyUser.getNickname()))
                    .messageStatus(MessageStatus.SUCCESS)
                    .messageContent(null)
                    .messageType(MessageType.LOBBY_UPDATE)
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/game-lobby/" + gameLobbyId, message);

            GameLobbyEvent<UserSession> newUserEvent = GameLobbyEvent.<UserSession>builder()
                    .payload(disconnectedUser)
                    .type(MessageEvent.USER_LEFT)
                    .message(String.format("%s has left", disconnectedUser.getDisplayName()))
                    .timestamp(Instant.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/game-lobby/" + gameLobbyId, newUserEvent);

        } catch (Exception ex) {
            Message<LobbySession> message = Message.<LobbySession>builder()
                    .errorMessage("Error disconnecting from lobby: " + ex.getMessage())
                    .messageContent(null)
                    .messageStatus(MessageStatus.FAILED)
                    .messageType(MessageType.LOBBY_UPDATE)
                    .build();

            String sessionId = (String) messageHeaders.get("simpSessionId");
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", message);
        }
    }


}