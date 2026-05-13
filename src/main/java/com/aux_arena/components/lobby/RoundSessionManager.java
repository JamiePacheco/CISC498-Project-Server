package com.aux_arena.components.lobby;

import com.aux_arena.components.scheduling.PhaseTimerManager;
import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.enums.PromptPairStatus;
import com.aux_arena.models.enums.RoundStatus;
import com.aux_arena.models.enums.message.MessageEvent;
import com.aux_arena.models.enums.message.UserEventType;
import com.aux_arena.models.session.*;
import com.aux_arena.models.session.round.PromptPair;
import com.aux_arena.models.session.round.RoundSession;
import com.aux_arena.models.session.round.VoteSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    ) {
    }

    ;

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

    public RoundSession getRoundSession(Long gameLobbyId) {
        return this.gameSessionManager.getGameSession(gameLobbyId).getCurrentRound();
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
        this.phaseTimerManager.schedulePhase(
                lobbyId,
                () -> {
                    log.info("Changing phase: {}", nextPhase );
                    this.gameSessionManager.setRoundStatus(lobbyId, nextPhase);
                    switch (nextPhase) {
                        case WRITING_PROMPT -> this.startPromptCreationPhase(lobbyId);
                        case CHOOSING_SONG -> this.startSelectMusicPhase(lobbyId);
                        case PRESENTING -> this.startPresentingPhase(lobbyId);
                        case VOTING -> this.startVotingPhase(lobbyId);
                        case SCORING -> this.startScoringPhase(lobbyId);
                        case TRANSITIONING -> this.endRound(lobbyId);
                    }
            },
            this.getRoundSession(lobbyId).getRoundStatus().defaultDuration
        );
    }

    public void startPromptCreationPhase(Long gameLobbyId) {
        // cancel any scheduled events
        this.phaseTimerManager.cancelTimer(gameLobbyId);

        // distribute the prompts to the users
        LobbySession lobbySession = this.lobbyManager.getLobbies().get(gameLobbyId);

        // users should only get the prompts they actually are responding to.
        String message = "Prompt Creation Phase Started";
        Long eventIndex = this.lobbyManager.nextEventSequence(gameLobbyId);

        // broadcast the new phase to the users
        this.broadcastService.broadcastLobbyEvent(
                lobbySession,
                new PhaseChangePayload(
                        RoundStatus.WRITING_PROMPT,
                        Instant.now(),
                        RoundStatus.WRITING_PROMPT.defaultDuration
                ),
                message,
                MessageEvent.PHASE_CHANGE,
                eventIndex
        );


        if (this.gameSessionManager.getGameSessions().get(gameLobbyId).getGameSettings().isTimed()) {
            scheduleLobbyPhase(gameLobbyId, RoundStatus.CHOOSING_SONG);
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

        // want to make sure all prompts are distributed before we change phases
        List<CompletableFuture<Void>> assignmentFutures = new ArrayList<>();

        // could parallelize this but idgaf(lip)
        for (PromptPair promptPair : roundSession.getPromptPairs().values()) {
            log.info("Assigning Prompt: [{}] to users [{}] [{}]",
                    promptPair.getPrompt().getPrompt(),
                    promptPair.getPlayers().get(0).getUserSessionId(),
                    promptPair.getPlayers().get(1).getUserSessionId()
            );

            for (PlayerState playerState : promptPair.getPlayers()) {
                UserSession userSession = this.lobbyManager.getUserSessions().get(playerState.getUserSessionId());

                // add completable to track which broadcasts are completed
                assignmentFutures.add(this.broadcastService.broadcastUserEvent(
                        lobbySession,
                        userSession,
                        promptPair,
                        "prompt assigned",
                        UserEventType.PROMPT_ASSIGNED
                ));
            }
        }
            // we broadcast here because we have to handle both manual and scheduled cases

        // make sure that all of the broadcasts are finished first
        CompletableFuture.allOf(assignmentFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
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

                    if (this.gameSessionManager.getGameSessions().get(gameLobbyId).getGameSettings().isTimed()) {
                        scheduleLobbyPhase(gameLobbyId, RoundStatus.PRESENTING);
                    }
                });
    }

    // display phase (presenting -> voting -> results/score) repeats until no prompts left

    public void startPresentingPhase(Long gameLobbyId) {
        // cancel any scheduled events
        this.phaseTimerManager.cancelTimer(gameLobbyId);

        // distribute the prompts to the users
        RoundSession roundSession = this.getRoundSession(gameLobbyId);
        LobbySession lobbySession = this.lobbyManager.getLobbies().get(gameLobbyId);

        // get the first prompt pair to present
        PromptPair displayPrompt = roundSession.getPromptToDisplay();

        if (displayPrompt == null) {
            this.endRound(gameLobbyId);
            return;
        }

        // turn this into a function
        int promptNumber = roundSession.getPromptPairs().size() - roundSession.getPromptPairs().values().stream().filter(p -> p.getStatus() != PromptPairStatus.RECEIVED_VOTES).toList().size();

        String message = "Displaying Prompt " + promptNumber;
        Long eventIndex = this.lobbyManager.nextEventSequence(gameLobbyId);

        this.broadcastService.broadcastLobbyEvent(
                lobbySession,
                displayPrompt,
                message,
                MessageEvent.DISPLAY_PROMPT,
                eventIndex
        );

        eventIndex = this.lobbyManager.nextEventSequence(gameLobbyId);

        // broadcast the new phase to the users
        this.broadcastService.broadcastLobbyEvent(
                lobbySession,
                new PhaseChangePayload(
                        RoundStatus.PRESENTING,
                        Instant.now(),
                        roundSession.getPhaseDuration()
                ),
                message,
                MessageEvent.PHASE_CHANGE,
                eventIndex
        );

        this.sendSystemMessage(gameLobbyId, message);

        // should always schedule it (even if rounds are not timed)
        scheduleLobbyPhase(gameLobbyId, RoundStatus.VOTING);
    }


    public void startVotingPhase(Long gameLobbyId) {
        this.phaseTimerManager.cancelTimer(gameLobbyId);

        // distribute the prompts to the users
        LobbySession lobbySession = this.lobbyManager.getLobbies().get(gameLobbyId);

        String message = "Voting Phase Starting";
        Long eventIndex = this.lobbyManager.nextEventSequence(gameLobbyId);

        // broadcast the new phase to the users
        this.broadcastService.broadcastLobbyEvent(
                lobbySession,
                new PhaseChangePayload(
                        RoundStatus.VOTING,
                        Instant.now(),
                        RoundStatus.VOTING.defaultDuration
                ),
                message,
                MessageEvent.PHASE_CHANGE,
                eventIndex
        );

        this.sendSystemMessage(gameLobbyId, message);
        if (this.gameSessionManager.getGameSessions().get(gameLobbyId).getGameSettings().isTimed()) {
            scheduleLobbyPhase(gameLobbyId, RoundStatus.SCORING);
        }
    }

    public void startScoringPhase(Long gameLobbyId) {
        this.phaseTimerManager.cancelTimer(gameLobbyId);

        PromptPair promptCalculatedScores = this.gameSessionManager.calculatePromptScores(gameLobbyId);

        String message = "Scores calculated";
        Long eventIndex = this.lobbyManager.nextEventSequence(gameLobbyId);

        LobbySession lobbySession = this.lobbyManager.getLobbies().get(gameLobbyId);

        // broadcast the new phase to the users
        this.broadcastService.broadcastLobbyEvent(
                lobbySession,
                new PhaseChangePayload(
                        RoundStatus.SCORING,
                        Instant.now(),
                        RoundStatus.SCORING.defaultDuration
                ),
                message,
                MessageEvent.PHASE_CHANGE,
                eventIndex
        );

        eventIndex = this.lobbyManager.nextEventSequence(gameLobbyId);

        // broadcast the new prompt pairs to the users
        this.broadcastService.broadcastLobbyEvent(
                lobbySession,
                promptCalculatedScores,
                message,
                MessageEvent.SCORE_UPDATES,
                eventIndex
        );

        this.scheduleLobbyPhase(gameLobbyId, RoundStatus.TRANSITIONING);
    }

    public void endRound(Long gameLobbyId) {
        this.phaseTimerManager.cancelTimer(gameLobbyId);

        RoundSession newRound = gameSessionManager.startNewRound(gameLobbyId);

        // All rounds are finished and game is over
        if (newRound == null) {
            this.endGame(gameLobbyId);
            // go to final score screen and declare winner
        }

        String message = "Round " + gameSessionManager.getGameSession(gameLobbyId).getRounds().size() + " Starting";
        Long eventIndex = this.lobbyManager.nextEventSequence(gameLobbyId);

        LobbySession lobbySession = this.lobbyManager.getLobbies().get(gameLobbyId);

        // broadcast the new phase to the users
        this.broadcastService.broadcastLobbyEvent(
                lobbySession,
                newRound,
                message,
                MessageEvent.ROUND_STARTED,
                eventIndex
        );

       this.sendSystemMessage(gameLobbyId, message);

        // broadcast the new phase to the users
        this.broadcastService.broadcastLobbyEvent(
                lobbySession,
                new PhaseChangePayload(
                        RoundStatus.TRANSITIONING,
                        Instant.now(),
                        RoundStatus.TRANSITIONING.defaultDuration
                ),
                message,
                MessageEvent.PHASE_CHANGE,
                eventIndex
        );



        this.scheduleLobbyPhase(gameLobbyId, RoundStatus.WRITING_PROMPT);
    }

    public void endGame(Long gameLobbyId) {

        // end the game session
        this.gameSessionManager.endGameSession(gameLobbyId);

        // update lobby state
        this.lobbyManager.endLobbyGame(gameLobbyId);

        // calculate the final scores
        List<PlayerState> scoreResults = this.gameSessionManager.obtainGameWinners(gameLobbyId);

        String message = "Game Finished";
        Long eventIndex = this.lobbyManager.nextEventSequence(gameLobbyId);

        LobbySession lobbySession = this.lobbyManager.getLobbies().get(gameLobbyId);

        // broadcast the new phase to the users
        this.broadcastService.broadcastLobbyEvent(
                lobbySession,
                scoreResults,
                message,
                MessageEvent.GAME_ENDED,
                eventIndex
        );

        this.sendSystemMessage(gameLobbyId, message);
    }

}