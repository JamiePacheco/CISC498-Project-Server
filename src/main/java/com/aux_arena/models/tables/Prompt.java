package com.aux_arena.models.tables;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Prompt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ROUND_ID")
    private Long roundId;

    @Column(name = "PROMPT", length = 400)
    private String prompt;

    @Column(name = "AUTHOR")
    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    @Column(name = "WINNER")
    @OneToOne(fetch = FetchType.LAZY)
    private User winner;

    @Column(name = "POSITION")
    private int position;
}