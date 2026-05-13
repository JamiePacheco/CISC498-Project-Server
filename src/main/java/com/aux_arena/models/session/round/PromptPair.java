package com.aux_arena.models.session.round;

import com.aux_arena.models.enums.PromptPairStatus;
import com.aux_arena.models.session.PlayerState;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class PromptPair {
    private String promptId;

    private Prompt prompt;
    private List<PlayerState> players = new ArrayList<>();

    //key is users session id in PlayerState
    private Map<String, PromptSubmission> promptSubmissions = new HashMap<>();

    private List<VoteSubmission> voteSubmissions;
    private PromptPairStatus status;

}