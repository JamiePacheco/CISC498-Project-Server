package com.aux_arena.models.session;

import com.aux_arena.models.enums.GameMode;
import lombok.Builder;

@Builder
public class GameSettings {

    private GameMode gameMode;
    // should each phase (minus display phase) have a timer
    private boolean timed;
    // max time to display song
    private Long maxDisplayTime;

}
