package com.aux_arena.controller.socket;

import com.aux_arena.components.LobbyManager;
import com.aux_arena.models.enums.message.MessageEvent;
import com.aux_arena.models.enums.message.MessageStatus;
import com.aux_arena.models.enums.message.MessageType;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.socket.Message;
import com.aux_arena.models.socket.event.GameLobbyEvent;
import com.aux_arena.models.tables.LobbyUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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

    @MessageMapping("/game-lobby.send/{game-lobby-id}")
    public void joinGameLobby(
            @DestinationVariable(value = "game-lobby-id") Long gameLobbyId,
            @Payload LobbyUser lobbyUser,
            MessageHeaders messageHeaders
    ) {
        try {
            String sessionId = (String) messageHeaders.get("simpSessionId");
            lobbyUser.setLastSocketConnectionId(sessionId);
            LobbySession joinedLobby = this.lobbyManager.onUserConnect(gameLobbyId, lobbyUser);

            // used to send the entire lobby to the new user
            Message<LobbySession> message =  Message.<LobbySession>builder()
                    .Message(String.format("%s joined game lobby", lobbyUser.getNickname()))
                    .messageStatus(MessageStatus.SUCCESS)
                    .messageContent(joinedLobby)
                    .messageType(MessageType.LOBBY_UPDATE)
                    .build();

            messagingTemplate.convertAndSendToUser(sessionId, "/topic/game-lobby." + gameLobbyId, message);

            UserSession joinedUser = joinedLobby.getActiveUsers().get(sessionId);

            GameLobbyEvent<UserSession> newUserEvent = GameLobbyEvent.<UserSession>builder()
                    .payload(joinedUser)
                    .type(MessageEvent.USER_JOINED)
                    .message(String.format("%s had joined", joinedUser.getDisplayName()))
                    .timestamp(Instant.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/game-lobby."+gameLobbyId, newUserEvent);

        } catch (Exception ex) {
            Message<LobbySession> message = Message.<LobbySession>builder()
                    .errorMessage("Could not join lobby: " + ex.getMessage())
                    .messageContent(null)
                    .messageStatus(MessageStatus.FAILED)
                    .messageType(MessageType.LOBBY_UPDATE)
                    .build();

            String sessionId = (String) messageHeaders.get("simpSessionId");
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", message);
        }
    }







}
