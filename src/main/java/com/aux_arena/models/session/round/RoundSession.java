package com.aux_arena.models.session.round;


import com.aux_arena.models.enums.RoundStatus;
import com.aux_arena.models.session.PlayerPrompt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoundSession {

    private Long roundId;
    private RoundStatus roundStatus;

    private List<PromptPair> promptPairs;

    private int currentPairIndex = 0;
}
