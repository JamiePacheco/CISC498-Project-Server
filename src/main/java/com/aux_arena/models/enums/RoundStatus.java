package com.aux_arena.models.enums;

public enum RoundStatus {
    TRANSITIONING(15L),
    WAITING(0L),
    SCORING(20L),
    VOTING(30L),
    PRESENTING(60L),
    CHOOSING_SONG(60L),

    WRITING_PROMPT(30L);

    public Long defaultDuration;
//    public RoundStatus nextPhase;

    RoundStatus(Long defaultDuration) {
        this.defaultDuration = defaultDuration;
//        this.nextPhase = nextPhase;
    }
}
