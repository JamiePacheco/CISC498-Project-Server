package com.aux_arena.models.session.round;

import com.aux_arena.models.session.UserSession;

public class Vote {
    private Long id;
    private UserSession voter;
    private Long promptPairId;
    private Long songChoiceId;
}
