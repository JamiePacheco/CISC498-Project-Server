package com.aux_arena.models.session;

import com.aux_arena.models.session.round.PromptSubmission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerState {
    private Long userId; // this should link to a user session variable
    private String userSessionId; // this should be the string that identifies them in lobby manager
    private Long score;
    private boolean ready; // this indicates if they are ready to move onto the next round;
    private boolean isSpectator; // spectators should be able to vote, but can't submit prompts or respond to them
    private List<PromptSubmission> promptSubmissions;
}