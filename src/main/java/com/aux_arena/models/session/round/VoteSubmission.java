package com.aux_arena.models.session.round;

import lombok.Data;

@Data
public class VoteSubmission {
    private String voterId;
    private String promptPairId;
    private String submissionAuthorId; // temp id that references PlayerState/UserSession
}
