package com.aux_arena.models.session;

import com.aux_arena.models.enums.GameStatus;
import com.aux_arena.models.session.round.RoundSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameSession {

    private Long id;
    private Long lobbyId;
    private GameStatus gameStatus;
    private Instant createdAt;
    private Instant lastUpdated;

    // uses the same principle id
    private Map<String, PlayerState> players = new ConcurrentHashMap<>();

    private RoundSession currentRound;

    public GameSession(LobbySession lobbySession) {

        // add based attributes for the game session
        this.lobbyId = lobbySession.getId();
        this.gameStatus = GameStatus.STARTING;
        this.createdAt = Instant.now();
        this.lastUpdated = Instant.now();

        // add a new player state for each user session within the current game lobby
        for (String key : lobbySession.getActiveUsers().keySet()) {
            PlayerState newPlayerState = PlayerState.builder()
                    .ready(true)
                    .score(0L)
                    .userId(lobbySession.getActiveUsers().get(key).getUserId())
                    .build();

            players.put(key, newPlayerState);
        }
    }
}
