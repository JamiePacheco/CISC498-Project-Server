package com.aux_arena.models.session.round;

import com.aux_arena.models.enums.PromptPairStatus;
import com.aux_arena.models.session.PlayerState;
import com.aux_arena.models.session.UserSession;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class PromptPair {

    private Prompt prompt;
    private List<PlayerState> players;

    //key is users session id in PlayerState
    private Map<String, SongChoice> songChoices;

    private List<Vote> votes;
    private PromptPairStatus status;

}
