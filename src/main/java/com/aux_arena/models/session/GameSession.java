package com.aux_arena.models.session;

import com.aux_arena.models.enums.GameStatus;
import com.aux_arena.models.session.round.RoundSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

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

    private Map<String, PlayerState> players;

    private RoundSession currentRound;
}
