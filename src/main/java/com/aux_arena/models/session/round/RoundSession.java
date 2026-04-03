package com.aux_arena.models.session.round;


import com.aux_arena.models.enums.PromptPairStatus;
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

    // This is used during the presenting phase
    private int currentPairIndex = 0;

    public Prompt submitPrompt(Prompt prompt) {
        promptPairs.add(
                PromptPair.builder()
                        .prompt(prompt)
                        .status(PromptPairStatus.WAITING_FOR_PLAYERS)
                        .build()
        );

        return prompt;
    }
}
