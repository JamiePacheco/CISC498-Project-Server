package com.aux_arena.models.enums.message;

// TODO reorder enum objects so they reflect the orderings of the game
public enum MessageEvent {
    USER_JOINED,
    UPDATE_PLAYER_STATE,
    USER_LEFT,
    USER_CLEANUP,
    LOBBY_UPDATED,
    NEW_HOST,
    GAME_STARTED,
    GAME_ENDED,
    SCORE_UPDATES,
    ROUND_STARTED,
    PROMPT_SUBMITTED,
    PROMPT_ASSIGNED,
    SUBMISSION_RECEIVED,
    VOTE_UPDATES,
    NEW_MESSAGE,
    PHASE_CHANGE,
    DISPLAY_PROMPT
}
