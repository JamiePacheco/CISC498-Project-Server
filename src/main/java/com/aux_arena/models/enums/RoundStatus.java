package com.aux_arena.models.enums;

public enum RoundStatus {
    TRANSITIONING(15L),
    WAITING(0L),
    SCORING(15L),
    VOTING(45L),
    PRESENTING(30L),
    CHOOSING_SONG(120L),

    WRITING_PROMPT(60L);

    public Long defaultDuration;
//    public RoundStatus nextPhase;

    RoundStatus(Long defaultDuration) {
        this.defaultDuration = defaultDuration;
//        this.nextPhase = nextPhase;
    }
}
