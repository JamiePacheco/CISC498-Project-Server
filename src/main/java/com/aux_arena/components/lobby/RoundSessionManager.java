package com.aux_arena.components.lobby;

import com.aux_arena.components.scheduling.PhaseTimerManager;
import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.enums.RoundStatus;
import com.aux_arena.models.enums.message.MessageEvent;
import com.aux_arena.models.enums.message.UserEventType;
import com.aux_arena.models.session.*;
import com.aux_arena.models.session.round.PromptPair;
import com.aux_arena.models.session.round.RoundSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/*
    Round Session manager primarily handles the phase change events of a game session
 */
@Component
public class RoundSessionManager {

    private final LobbyManager lobbyManager;

    private final GameSessionManager gameSessionManager;

    private final BroadcastService broadcastService;

    private final PhaseTimerManager phaseTimerManager;

    // payload body
    record PhaseChangePayload(
            RoundStatus roundStatus,
            Instant lastUpdated,
            Long phaseDuration
    ){};

    private static Logger log = LoggerFactory.getLogger(RoundSessionManager.class);

    public RoundSessionManager(
            LobbyManager lobbyManager,
            GameSessionManager gameSessionManager,
            BroadcastService broadcastService,
            PhaseTimerManager phaseTimerManager
    ) {
        this.lobbyManager = lobbyManager;
        this.gameSessionManager = gameSessionManager;
        this.broadcastService = broadcastService;
        this.phaseTimerManager = phaseTimerManager;
    }

    // redefine this in class scope because we don't have access from GameManager
    public void sendSystemMessage(Long lobbyId, String message) {
        GameLobbyMessage savedMessage = this.lobbyManager.sendSystemGameLobbyMessage(lobbyId, message);
        LobbySession lobbySession = this.lobbyManager.getLobbies().get(lobbyId);

        this.broadcastService.broadcastLobbyMessage(
                lobbySession,
                savedMessage
        );

    }

    public void scheduleLobbyPhase(Long lobbyId, RoundStatus nextPhase) {
        switch (nextPhase) {
            case CHOOSING_SONG:
                this.phaseTimerManager.schedulePhase(
                        lobbyId,
                        () -> {
                            this.gameSessionManager.setRoundStatus(lobbyId, RoundStatus.PRESENTING);
                            this.startSelectMusicPhase(lobbyId);
                        },
                        nextPhase.defaultDuration
                );
                break;
            case PRESENTING:
        }
    }


    public void startSelectMusicPhase(Long gameLobbyId) {
        // cancel any scheduled events
        this.phaseTimerManager.cancelTimer(gameLobbyId);

        // first want to check if every user has submitted a prompt (need a certain amount)
        this.gameSessionManager.verifyUserPrompts(gameLobbyId);

        // distribute the prompts to the users
        RoundSession roundSession = this.gameSessionManager.distributePrompts(gameLobbyId);
        LobbySession lobbySession = this.lobbyManager.getLobbies().get(gameLobbyId);

        // we broadcast here because we have to handle both manual and scheduled cases

        // users should only get the prompts they actually are responding to.


        String message = "Song Selection Phase Started";
        Long eventIndex = this.lobbyManager.nextEventSequence(gameLobbyId);

        // broadcast the new phase to the users
        this.broadcastService.broadcastLobbyEvent(
                lobbySession,
                new PhaseChangePayload(
                        RoundStatus.CHOOSING_SONG,
                        Instant.now(),
                        roundSession.getPhaseDuration()
                ),
                message,
                MessageEvent.PHASE_CHANGE,
                eventIndex
        );

        this.sendSystemMessage(gameLobbyId, message);

        // could parallelize this but idgaf(lip)
        for (PromptPair promptPair : roundSession.getPromptPairs().values()) {
            for (PlayerState playerState : promptPair.getPlayers()) {
                UserSession userSession = this.lobbyManager.getUserSessions().get(playerState.getUserSessionId());
                this.broadcastService.broadcastUserEvent(
                        lobbySession,
                        userSession,
                        promptPair,
                        "prompt assigned",
                        UserEventType.PROMPT_ASSIGNED
                );
            }
        }

        this.scheduleLobbyPhase(gameLobbyId, RoundStatus.PRESENTING);
    }
}