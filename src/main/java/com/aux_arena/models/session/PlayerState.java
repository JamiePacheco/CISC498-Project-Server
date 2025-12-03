package com.aux_arena.models.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerState {
    private Long userId; // this should link to a user session variable
    private Long score;
    private boolean ready;
}
