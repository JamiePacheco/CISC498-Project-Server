package com.aux_arena.models.session.round;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongChoice {
    private String videoUrl;
    private String title;
    private String thumbnail;
    private Long timestampStartAt;
    private Long timestampEndAt;
    private Instant submittedAt;
}
