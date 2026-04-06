package com.aux_arena.models.session.round;

import com.aux_arena.models.session.PlayerState;

public class Vote {
    private PlayerState voter;
    private String promptPairId;
    private String submissionAuthorId; // temp id that references PlayerState/UserSession
}
