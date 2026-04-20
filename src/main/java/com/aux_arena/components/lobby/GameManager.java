package com.aux_arena.components.lobby;


import com.aux_arena.components.lobby.model.AtomicOperationResult;
import com.aux_arena.components.scheduling.PhaseTimerManager;
import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.enums.RoundStatus;
import com.aux_arena.models.enums.message.MessageEvent;
import com.aux_arena.models.session.*;
import com.aux_arena.models.session.round.*;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

@Data
@Component
public class GameManager {
    private final GameSessionManager gameSessionManager;
    private final LobbyManager lobbyManager;
    private final PhaseTimerManager phaseTimerManager;
    private final RoundSessionManager roundSessionManager;
    private final BroadcastService broadcastService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public GameManager(
            GameSessionManager gameSessionManager,
            LobbyManager lobbyManager,
            PhaseTimerManager phaseTimerManager,
            RoundSessionManager roundSessionManager,
            BroadcastService broadcastService
    ) {
        this.phaseTimerManager = phaseTimerManager;
        this.roundSessionManager = roundSessionManager;
        this.gameSessionManager = gameSessionManager;
        this.lobbyManager = lobbyManager;
        this.broadcastService = broadcastService;
    }

    public UserSession getUserSession(Long gameLobbyId, Principal principal) {
        UserSession user = lobbyManager
                .getLobbies()
                .get(gameLobbyId)
                .getActiveUsers()
                .get(principal.getName());

        if (user == null) {
            throw new RuntimeException(
                    String.format("No user connected to lobby %d with id %s", gameLobbyId, principal.getName())
            );
        }
        return user;
    }

    public LobbySession getLobbySession(Long gameLobbyId) {
        LobbySession lobbySession = lobbyManager.getLobbies().get(gameLobbyId);

        if (lobbySession == null) {
            throw new RuntimeException(
                    String.format("No lobby with id %d", gameLobbyId)
            );
        }

        return lobbySession;
    }

    // function that acquires both locks from lobby and game session to prevent race conditions and
    // dependencies between them from changing causing race conditons
    public <T> T withBothLocks(Long lobbyId, BiFunction<GameSession, LobbySession, T> actions) {

        ReentrantLock lobbyLock = this.gameSessionManager.getGameSessionLocks().get(lobbyId);
        ReentrantLock gameLock = this.lobbyManager.getLobbyLocks().get(lobbyId);

        lobbyLock.lock();
        try {
            gameLock.lock();
            try {
                return actions.apply(
                        this.gameSessionManager.getGameSession(lobbyId),
                        this.lobbyManager.getLobbies().get(lobbyId)
                );
            } finally {
                gameLock.unlock();
            }
        } finally {
            lobbyLock.unlock();
        }
    }

    public void sendSystemMessage(Long lobbyId, String message) {
        GameLobbyMessage savedMessage = this.lobbyManager.sendSystemGameLobbyMessage(lobbyId, message);
        LobbySession lobbySession = this.getLobbySession(lobbyId);

        this.broadcastService.broadcastLobbyMessage(
                lobbySession,
                savedMessage
        );

    }

    //TODO more precise logic needs to be added here (logic that manages gameSession instance)

    public UserSession connectUser(long lobbyId, UserSession newUserSession, Principal principal) {

        UserSession connectedUser = this.lobbyManager.onUserConnect(lobbyId, newUserSession, principal);

        Long eventIndex = this.lobbyManager.nextEventSequence(lobbyId);
        LobbySession lobbySession = this.getLobbySession(lobbyId);

        String message = String.format("User %s joined", connectedUser.getDisplayName());

        // send out the new UserSession to the users
        broadcastService.broadcastLobbyEvent(
                lobbySession,
                connectedUser,
                message,
                MessageEvent.USER_JOINED,
                eventIndex
        );


        // if game is in session then we want to add a new user
        if (lobbySession.getStatus() == GameLobbyStatus.GAME_IN_PROGRESS) {
            PlayerState playerState = this.gameSessionManager.addNewUser(lobbyId, connectedUser);

            eventIndex = this.lobbyManager.nextEventSequence(lobbyId);

            // udpate the lobby of the new player state
            broadcastService.broadcastLobbyEvent(
                    lobbySession,
                    playerState,
                    message,
                    MessageEvent.UPDATE_PLAYER_STATE,
                    eventIndex
            );
        }

        // send system message to game lobby notifying user has joined
        this.sendSystemMessage(lobbyId, message);


        // we only return the actual user session because we can derive which player state is the user's based on tempId
        return connectedUser;
    }

    //TODO more precise logic needs to be added here (logic that manages gameSession instance)
    public UserSession disconnectUser(Long gameLobbyId, Principal principal) {
        UserSession disconnectedUserSession = this.lobbyManager.onUserDisconnect(gameLobbyId, principal);


        return disconnectedUserSession;
    }

    public UserSession disconnectUser(Principal principal) {
        // disconnect the current user
        UserSession userConnection = this.lobbyManager.getUserSessions().get(principal.getName());

        if (userConnection == null) {
            throw new RuntimeException("Bro how the flip we got an active user that isn't anywhere????");
        }



        UserSession disconnectedUserSession = this.lobbyManager.onUserDisconnect(userConnection.getLobbyId(), principal);
        // broadcast the disconnected user

        LobbySession lobbySession = this.getLobbySession(userConnection.getLobbyId());


        if (disconnectedUserSession.getHost()) {
            // this indicates that we want the oldest member as new host
            this.promoteUserToHost(lobbySession.getId(), null, null);
            disconnectedUserSession.setHost(false);
        }

        Long eventIndex = this.lobbyManager.nextEventSequence(lobbySession.getId());
        String message = String.format("%s disconnected", disconnectedUserSession.getDisplayName());

        // update the lobby of the new player state
        broadcastService.broadcastLobbyEvent(
                lobbySession,
                disconnectedUserSession,
                message,
                MessageEvent.USER_LEFT,
                eventIndex
        );

        // send out system message of new user
        this.sendSystemMessage(lobbySession.getId(), message);

        // check if a new host needs to be assigned


        return disconnectedUserSession;
    }

    public void promoteUserToHost(Long gameLobbyId, UserSession newHostUser, Principal principal) {
        UserSession newHost = this.lobbyManager.promoteUserToHost(gameLobbyId, newHostUser, principal);

        if (newHost == null) {
            // TODO make it so the lobby closes as this indicates no host could be assigned (thus empty lobby)
            return;
        }

        long eventIndex = this.lobbyManager.nextEventSequence(gameLobbyId);
        String message = String.format("%s promoted to host", newHost.getDisplayName());

        LobbySession lobbySession = this.getLobbySession(gameLobbyId);

        broadcastService.broadcastLobbyEvent(
                lobbySession,
                newHost,
                message,
                MessageEvent.NEW_HOST,
                eventIndex
        );

        this.sendSystemMessage(lobbySession.getId(), message);
    }

    public void sendLobbyMessage(Long gameLobbyId, GameLobbyMessage gameLobbymessage, Principal principal) {
        GameLobbyMessage message = this.lobbyManager.sendGameLobbyMessage(gameLobbyId, gameLobbymessage, principal);
        LobbySession lobbySession = this.getLobbySession(gameLobbyId);
        this.broadcastService.broadcastLobbyMessage(lobbySession, message);

    }

    /*
        TODO refactor
         - Test all lobby functionality and ensure it works (connect, disconnect, host change, reconnect, etc)
         - implement RoundSessionManager and GameSessionManager methods using similar methodology as LobbyManager (and lobby methods here)
         - plan how implementing scheduling phases will work
         - make sure to check that socket messages contain information to keep track of time.
         - implement sending in votes
     */


    public GameSession startGameSession(Long gameLobbyId, GameSettings gameSettings, Principal principal) {

        // data format for what we send to client
        record StartGamePayload(
                GameSession gameSession,
                Instant lastUpdated,
                GameLobbyStatus lobbyStatus
        ){};

        UserSession host = this.getUserSession(gameLobbyId, principal);

        if (!host.getHost()) {
            throw new RuntimeException(String.format("Cannot start game, user '%s' is not the host", principal.getName()));
        }

        LobbySession lobbySession = this.lobbyManager.startGameLobby(gameLobbyId);
        GameSession gameSession = this.gameSessionManager.loadGameSession(lobbySession, gameSettings);

        if (gameSession.getGameSettings().isTimed()) {
            this.roundSessionManager.scheduleLobbyPhase(gameLobbyId, RoundStatus.CHOOSING_SONG);
        }

        long eventIndex = this.lobbyManager.nextEventSequence(gameLobbyId);

        this.broadcastService.broadcastLobbyEvent(
                lobbySession,
                new StartGamePayload(
                        gameSession,
                        lobbySession.getLastUpdated(),
                        lobbySession.getStatus()
                ),
                "Starting Game",
                MessageEvent.GAME_STARTED,
                eventIndex
        );

        this.lobbyManager.sendSystemGameLobbyMessage(gameLobbyId, "Starting Game");

        return gameSession;
    }

    public Prompt submitRoundPrompt(Long gameLobbyId, Prompt prompt, Principal principal) {

        UserSession userSession = this.getUserSession(gameLobbyId, principal);

        if (userSession == null) throw new RuntimeException("User [" + principal.getName() +  "] is not within lobby");

        // get the user's player state and assign it to the author of the prompt
        prompt.setAuthorId(this.gameSessionManager.getPlayerState(gameLobbyId, userSession).getUserSessionId());

        // submit prompt to the current round
        Prompt submittedPrompt = this.gameSessionManager.submitPrompt(gameLobbyId, prompt);

        this.lobbyManager.sendSystemGameLobbyMessage(
                gameLobbyId,
                String.format("%s has submitted their prompt", userSession.getDisplayName())
        );

        // check if all players have submitted prompts, if so set phase to choosing song
        if (this.gameSessionManager.checkReadyStatus(gameLobbyId, RoundStatus.CHOOSING_SONG)) {
            // manually start the phase change
            this.roundSessionManager.startSelectMusicPhase(gameLobbyId);
        }

        return submittedPrompt;
    }


    public PromptSubmission submitSongChoice(Long gameLobbyId, PromptSubmission promptSubmission, Principal principal) {

        UserSession userSession = this.getUserSession(gameLobbyId, principal);

        PromptSubmission submittedSong = this.gameSessionManager.submitSongChoice(gameLobbyId, promptSubmission, principal);

        PlayerState playerState = this.gameSessionManager.getPlayerState(gameLobbyId, userSession);
        playerState.getPromptSubmissions().add(submittedSong);

        // check if player has responded to proper number of prompts
        // if we make it so the prompts are variable this has to be changed to number of responses need
        if (playerState.getPromptSubmissions().size() == 2) {
            playerState.setReady(true);
        }

        // check if all the players are ready to move on to presenting stage (all have submitted valid responses)
        if (this.gameSessionManager.checkReadyStatus(gameLobbyId, RoundStatus.PRESENTING)) {
            this.roundSessionManager.startPresentingPhase(gameLobbyId);
        }

        return submittedSong;
    }

    // TODO add skip method that host can use to skip a currently presented song (maybe add a vote to skip...)

    public VoteSubmission submitSongVote(Long gameLobbyId, VoteSubmission voteSubmission, Principal principal) {
        UserSession userSession = this.getUserSession(gameLobbyId, principal);
        voteSubmission.setVoterId(principal.getName());

        VoteSubmission submittedVote = this.gameSessionManager.submitSongVote(gameLobbyId, voteSubmission);

        if (submittedVote == null) {
            throw new RuntimeException("Error submitting vote")
        }

        PlayerState playerState = this.gameSessionManager.getPlayerState(gameLobbyId, userSession);
        playerState.getVoteSubmissions().add(submittedVote);

        playerState.setReady(true);

        if (this.gameSessionManager.checkReadyStatus(gameLobbyId, RoundStatus.SCORING)) {
            this.roundSessionManager.startScoringPhase(gameLobbyId);
        }


        return submittedVote;
    }
}