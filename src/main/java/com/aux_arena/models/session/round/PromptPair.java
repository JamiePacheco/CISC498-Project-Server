package com.aux_arena.models.session.round;

import com.aux_arena.models.enums.PromptPairStatus;
import com.aux_arena.models.session.PlayerState;
import com.aux_arena.models.session.UserSession;

import java.util.List;
import java.util.Map;

public class PromptPair {

    private Long promptId;
    private List<PlayerState> players;

    //key is id in PlayerState
    private Map<Long, SongChoice> songChoices;

    private List<Vote> votes;
    private PromptPairStatus status;

}
