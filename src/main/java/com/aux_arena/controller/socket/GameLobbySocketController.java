package com.aux_arena.controller.socket;

import com.aux_arena.components.lobby.GameManager;
import com.aux_arena.models.enums.message.MessageEvent;
import com.aux_arena.models.enums.message.MessageStatus;
import com.aux_arena.models.enums.message.UserEventType;
import com.aux_arena.models.session.*;
import com.aux_arena.models.session.round.Prompt;
import com.aux_arena.models.session.round.PromptSubmission;
import com.aux_arena.models.session.round.VoteSubmission;
import com.aux_arena.models.socket.event.UserEvent;
import com.aux_arena.models.socket.event.GameLobbyEvent;
import com.aux_arena.models.tables.LobbyUser;
import lombok.extern.slf4j.Slf4j;
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



    // TODO use gameManager to implement all functionality within controller methods.
    private GameManager gameManager;

    public GameLobbySocketController(
            GameManager gameManager,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.gameManager = gameManager;
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
            UserSession newUserSession = this.gameManager.connectUser(gameLobbyId, userSession, principal);

            UserEvent<UserSession> userSessionUserEvent = UserEvent.<UserSession>builder()
                    .Message(String.format("%s joined game lobby", newUserSession.getDisplayName()))
                    .messageStatus(MessageStatus.SUCCESS)
                    .messageContent(newUserSession)
                    .userEventType(UserEventType.USER_UPDATE)
                    .sequence(newUserSession.getUserEventSequence())
                    .build();

            log.info("Sending message to user [{} ({})] ", principal.getName(), newUserSession.getDisplayName());
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/game-lobby/" + gameLobbyId, userSessionUserEvent);
        } catch (Exception ex) {
            UserEvent<String> userEvent = UserEvent.<String>builder()
                    .errorMessage(ex.getMessage())
                    .messageContent(ex.getMessage())
                    .messageStatus(MessageStatus.FAILED)
                    .userEventType(UserEventType.LOBBY_UPDATE)
                    .sequence(0L)
                    .build();

            log.info("Principle Name: {}", principal.getName());
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", userEvent);
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
            this.gameManager.sendLobbyMessage(gameLobbyId, gameLobbyMessage, principal);

            UserSession user = gameManager.getUserSession(gameLobbyId, principal);

            UserEvent<GameLobbyMessage> chatUserEventConfirmation = UserEvent.<GameLobbyMessage>builder()
                    .Message(String.format("Your (%s) message was successfully sent", user.getDisplayName()))
                    .messageStatus(MessageStatus.SUCCESS)
                    .messageContent(gameLobbyMessage)
                    .userEventType(UserEventType.CHAT_UPDATE)
                    .sequence(user.getUserEventSequence())
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", chatUserEventConfirmation);
        } catch (Exception ex) {
            UserEvent<String> userEvent = UserEvent.<String>builder()
                    .errorMessage(ex.getMessage())
                    .messageContent(ex.getMessage())
                    .messageStatus(MessageStatus.FAILED)
                    .userEventType(UserEventType.LOBBY_UPDATE)
                    .sequence(0L)
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", userEvent);
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
            UserSession disconnectedUser = this.gameManager.disconnectUser(gameLobbyId, principal);

            // we send the current snapshot of the lobby so it's bare details can stored to rejoin (not needed but good for display)
            UserEvent<LobbySession> userEvent = UserEvent.<LobbySession>builder()
                    .Message(String.format("successfully left lobby", lobbyUser.getNickname()))
                    .messageStatus(MessageStatus.SUCCESS)
                    .messageContent(null)
                    .userEventType(UserEventType.LOBBY_UPDATE)
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/game-lobby/" + gameLobbyId, userEvent);

            GameLobbyEvent<UserSession> newUserEvent = GameLobbyEvent.<UserSession>builder()
                    .payload(disconnectedUser)
                    .type(MessageEvent.USER_LEFT)
                    .message(String.format("%s has left", disconnectedUser.getDisplayName()))
                    .timestamp(Instant.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/game-lobby/" + gameLobbyId, newUserEvent);

        } catch (Exception ex) {
            UserEvent<LobbySession> userEvent = UserEvent.<LobbySession>builder()
                    .errorMessage("Error disconnecting from lobby: " + ex.getMessage())
                    .messageContent(null)
                    .messageStatus(MessageStatus.FAILED)
                    .userEventType(UserEventType.LOBBY_UPDATE)
                    .build();

            String sessionId = (String) messageHeaders.get("simpSessionId");
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", userEvent);
        }
    }

    @MessageMapping("game-lobby/start-game/{game-lobby-id}")
    public void startGameLobby(
            Principal principal,
            @DestinationVariable(value = "game-lobby-id") Long gameLobbyId,
            @Payload GameSettings gameSettings,
            MessageHeaders messageHeaders
    ) {
        try {
            GameSession gameSession = this.gameManager.startGameSession(gameLobbyId, gameSettings, principal);
            log.info("Started Game Session for lobby {}", gameLobbyId);



//           GameLobbyEvent<GameSession> gameStartedEvent = GameLobbyEvent.<GameSession>builder()
//                    .payload(gameSession)
//                    .type(MessageEvent.GAME_STARTED)
//                    .message(String.format("Starting game for lobby %s", gameSession.getLobbySessionId()))
//                    .timestamp(Instant.now())
//                    .build();
//
//            messagingTemplate.convertAndSend("/topic/game-lobby/" + gameLobbyId, gameStartedEvent);

        } catch (Exception ex) {
            log.info("Error starting game session for lobby {}", gameLobbyId);
            log.info(ex.getMessage());
            UserEvent<LobbySession> userEvent = UserEvent.<LobbySession>builder()
                    .errorMessage("Error Starting Game: " + ex.getMessage())
                    .messageContent(null)
                    .messageStatus(MessageStatus.FAILED)
                    .userEventType(UserEventType.LOBBY_UPDATE)
                    .build();

            String sessionId = (String) messageHeaders.get("simpSessionId");
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", userEvent);
        }
    }

    @MessageMapping("/game-lobby/submit-prompt/{game-lobby-id}")
    public void submitPrompt(
            Principal principal,
            @DestinationVariable("game-lobby-id") Long gameLobbyId,
            @Payload Prompt prompt
    ) {
        try {
            Prompt submittedPrompt = this.gameManager.submitRoundPrompt(gameLobbyId, prompt, principal);
            // send user verification their prompt has been submitted

            // we send the current snapshot of the lobby so it's bare details can stored to rejoin (not needed but good for display)
            UserEvent<Prompt> userEvent = UserEvent.<Prompt>builder()
                    .Message("Prompted successfully submitted")
                    .messageStatus(MessageStatus.SUCCESS)
                    .messageContent(submittedPrompt)
                    .userEventType(UserEventType.LOBBY_UPDATE)
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/game-lobby/" + gameLobbyId, userEvent);

        } catch (Exception ex) {
            UserEvent<LobbySession> userEvent = UserEvent.<LobbySession>builder()
                    .errorMessage("Error processing prompt submission: " + ex.getMessage())
                    .messageContent(null)
                    .messageStatus(MessageStatus.FAILED)
                    .userEventType(UserEventType.LOBBY_UPDATE)
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", userEvent);
        }
    }

    @MessageMapping("/game-lobby/submit-song/{game-lobby-id}")
    public void submitSong(
            Principal principal,
            @DestinationVariable("game-lobby-id") Long gameLobbyId,
            @Payload PromptSubmission promptSubmission
            ) {
        try {

            PromptSubmission submittedPrompt = this.gameManager.submitSongChoice(gameLobbyId, promptSubmission, principal);
            // send user verification their prompt has been submitted

            // we send the current snapshot of the lobby so it's bare details can stored to rejoin (not needed but good for display)
            UserEvent<PromptSubmission> userEvent = UserEvent.<PromptSubmission>builder()
                    .Message("Prompted successfully submitted")
                    .messageStatus(MessageStatus.SUCCESS)
                    .messageContent(submittedPrompt)
                    .userEventType(UserEventType.LOBBY_UPDATE)
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/game-lobby/" + gameLobbyId, userEvent);


//            GameLobbyEvent<PromptSubmission> newPromptEvent = GameLobbyEvent.<PromptSubmission>builder()
//                    .payload(submittedPrompt)
//                    .type(MessageEvent.PROMPT_SUBMITTED)
//                    .message("prompt received")
//                    .timestamp(Instant.now())
//                    .build();
//
//            messagingTemplate.convertAndSend("/topic/game-lobby/" + gameLobbyId, newPromptEvent);

        } catch (Exception ex) {
            UserEvent<LobbySession> userEvent = UserEvent.<LobbySession>builder()
                    .errorMessage("Error processing prompt submission: " + ex.getMessage())
                    .messageContent(null)
                    .messageStatus(MessageStatus.FAILED)
                    .userEventType(UserEventType.LOBBY_UPDATE)
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", userEvent);
        }
    }


    @MessageMapping("/game-lobby/submit-vote/{game-lobby-id}")
    public void submitSong(
            Principal principal,
            @DestinationVariable("game-lobby-id") Long gameLobbyId,
            @Payload VoteSubmission voteSubmission
            ) {
        try {
            VoteSubmission submittedVote = this.gameManager.submitSongVote(gameLobbyId, voteSubmission, principal);
            // send user verification their prompt has been submitted

            // we send the current snapshot of the lobby so it's bare details can stored to rejoin (not needed but good for display)
            UserEvent<VoteSubmission> userEvent = UserEvent.<VoteSubmission>builder()
                    .Message("Vote successfully submitted")
                    .messageStatus(MessageStatus.SUCCESS)
                    .messageContent(submittedVote)
                    .userEventType(UserEventType.LOBBY_UPDATE)
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/game-lobby/" + gameLobbyId, userEvent);

            GameLobbyEvent<VoteSubmission> newPromptEvent = GameLobbyEvent.<VoteSubmission>builder()
                    .payload(submittedVote)
                    .type(MessageEvent.PROMPT_SUBMITTED)
                    .message(String.format("Vote from user [%s] received", principal.getName()))
                    .timestamp(Instant.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/game-lobby/" + gameLobbyId, newPromptEvent);

        } catch (Exception ex) {
            UserEvent<LobbySession> userEvent = UserEvent.<LobbySession>builder()
                    .errorMessage("Error processing prompt submission: " + ex.getMessage())
                    .messageContent(null)
                    .messageStatus(MessageStatus.FAILED)
                    .userEventType(UserEventType.LOBBY_UPDATE)
                    .build();

            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", userEvent);
        }
    }


}