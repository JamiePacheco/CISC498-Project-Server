package com.aux_arena.models.session;

import com.aux_arena.models.enums.GameMode;
import com.aux_arena.models.enums.GameStatus;
import com.aux_arena.models.enums.PromptPairStatus;
import com.aux_arena.models.enums.RoundStatus;
import com.aux_arena.models.session.round.PromptPair;
import com.aux_arena.models.session.round.RoundSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameSession {

    private Long id;
    private Long lobbySessionId;
    private GameStatus gameStatus;
    private Instant createdAt;
    private Instant lastUpdated;

    private GameSettings gameSettings;

    // uses the same principle id
    private Map<String, PlayerState> players = new ConcurrentHashMap<>();

    private RoundSession currentRound;



    public GameSession(LobbySession lobbySession, GameSettings gameSettings) {

        // add based attributes for the game session
        this.lobbySessionId = lobbySession.getId();
        this.gameStatus = GameStatus.STARTING;
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();

        // this eventually should be made to accept some settings input
        if (gameSettings == null) {
            this.gameSettings = GameSettings.builder()
                    .timed(true)
                    .maxDisplayTime(30L)
                    .gameMode(GameMode.PROMPT_BATTLE)
                    .build();
        } else {
            this.gameSettings = gameSettings;
        }
        this.currentRound = RoundSession.builder()
                .roundStatus(RoundStatus.WRITING_PROMPT)
                .phaseDuration(RoundStatus.WRITING_PROMPT.defaultDuration)
                .build();

        // add a new player state for each user session within the current game lobby
        for (String key : lobbySession.getActiveUsers().keySet()) {
            PlayerState newPlayerState = PlayerState.builder()
                    .ready(false)
                    .score(0L)
                    .userId(lobbySession.getActiveUsers().get(key).getUserId())
                    .isSpectator(lobbySession.getActiveUsers().get(key).getIsSpectator())
                    .build();

            players.put(key, newPlayerState);
        }
    }

    public PlayerState addNewPlayerState(UserSession userSession) {
        PlayerState newPlayerState = PlayerState.builder()
                .ready(false)
                .score(0L)
                .userId(userSession.getUserId())
                .isSpectator(userSession.getIsSpectator())
                .build();

        this.getPlayers().put(newPlayerState.getUserSessionId(), newPlayerState);
        return newPlayerState;
    }

    public List<PlayerState> getNonSpectatorPlayers() {
        return players.values().stream().filter(u -> u.isSpectator()).toList();
    }

    public boolean checkReadyStatus(RoundStatus nextPhase) {
        // all non spectator players are ready;
        boolean isReady = this.getNonSpectatorPlayers().stream()
                .filter(u -> u.isReady())
                .toList()
                .size() == this.getNonSpectatorPlayers().size();

        if (isReady) {
            for (PlayerState playerState : players.values()) {
                playerState.setReady(false);
            }

            this.getCurrentRound().setRoundStatus(nextPhase);
            this.getCurrentRound().setPhaseDuration(nextPhase.defaultDuration);

            // schedule the new phase
        }

        return isReady;
    }



    // distribute the prompts among the active players
    public RoundSession distributePrompts() {

        List<PromptPair> promptPairs = this.currentRound.getPromptPairs().values().stream().toList();
        Collections.shuffle(promptPairs);
        int n = promptPairs.size();

        for (int i = 0; i < n ; i++) {

            // get the corresponding player
            PlayerState playerState = this.getPlayers().get(promptPairs.get(i).getPrompt().getAuthorId());

            // get the index of the prompts this user will be assigned to
            // using circular assignment (one to left, one to right from pair array)
            int promptOne = (i + n - 1) % n;
            int promptTwo = (i + n - 2) % n;

            promptPairs.get(promptOne).getPlayers().add(playerState);
            promptPairs.get(promptTwo).getPlayers().add(playerState);
        }

        // make sure all prompts have two players assigned to them
        for (PromptPair promptPair : promptPairs) {
            if (promptPair.getPlayers().size() == 2) {
                promptPair.setStatus(PromptPairStatus.WAITING_FOR_VOTES);
            }
        }

        return this.getCurrentRound();
    }
}
