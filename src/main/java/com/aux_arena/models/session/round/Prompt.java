package com.aux_arena.models.session.round;

import com.aux_arena.models.session.PlayerState;
import com.aux_arena.models.session.UserSession;
import lombok.Data;

@Data
public class Prompt {

    private String prompt;

    private PlayerState author;
}