package com.aux_arena.models.session.round;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PromptSubmission {
    private SongChoice songChoice;
    private String promptPairId; // what prompt pair does this correspond to
    private Instant submittedAt;
}