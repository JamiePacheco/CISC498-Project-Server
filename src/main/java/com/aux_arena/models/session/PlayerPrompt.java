package com.aux_arena.models.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayerPrompt {

    private Long id;
    private String prompt;
    private UserSession author;

}
