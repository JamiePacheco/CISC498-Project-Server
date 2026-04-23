package com.aux_arena.models.session.round;


import com.aux_arena.models.enums.PromptPairStatus;
import com.aux_arena.models.enums.RoundStatus;
import com.aux_arena.models.session.PlayerPrompt;
import com.aux_arena.utility.UuidGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoundSession {

    private Long roundId;
    private RoundStatus roundStatus;

    private Map<String, PromptPair> promptPairs = new ConcurrentHashMap<>();

    private Long phaseDuration = 30L;

    // This is used during the presenting phase
    private String currentPromptId;

    public Prompt submitPrompt(Prompt prompt) {
        String promptId = UuidGenerator.generateUuid();
        promptPairs.put(promptId,
                PromptPair.builder()
                        .prompt(prompt)
                        .promptId(promptId) // generate a uniqueID for the prompt to use
                        .status(PromptPairStatus.WAITING_FOR_PLAYERS)
                        .build()
        );

        return prompt;
    }

    public PromptPair getPromptToDisplay() {
        List<PromptPair> viablePrompts = promptPairs
                .values()
                .stream()
                .filter(p -> p.getStatus() == PromptPairStatus.WAITING_FOR_VOTES)
                .toList();

        // if all prompts have been voted on then we return null
        if (viablePrompts.isEmpty()) return null;

        // choose a prompt at random
        Random rand = new Random();
        PromptPair nextPrompt = viablePrompts.get(rand.nextInt(viablePrompts.size()));

        this.setCurrentPromptId(nextPrompt.getPromptId());

        // change the status so it is not included in next filter
        nextPrompt.setStatus(PromptPairStatus.RECEIVED_VOTES);

        return nextPrompt;
    }
}
