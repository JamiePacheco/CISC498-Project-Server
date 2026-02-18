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
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "ROUND_ID")
    private Long roundId;

    @Column(name = "PROMPT", length = 400)
    private String prompt;

    @JoinColumn(name = "AUTHOR_ID")
    @ManyToOne(fetch = FetchType.LAZY,  cascade = CascadeType.ALL)
    private LobbyUser author;

    @JoinColumn(name = "ID")
    @OneToOne(fetch = FetchType.LAZY,  cascade = CascadeType.ALL)
    private LobbyUser winner;

    @Column(name = "POSITION")
    private int position;
}