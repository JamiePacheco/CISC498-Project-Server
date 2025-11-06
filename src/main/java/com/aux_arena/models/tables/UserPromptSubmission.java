package com.aux_arena.models.tables;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPromptSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JoinColumn(name = "PROMPT_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Prompt prompt;

    @JoinColumn(name = "SONG_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private Prompt promptId;

    @Column(name = "VOTES")
    private int votes;

    @JoinColumn(name = "AUTHOR_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private User author;



}
