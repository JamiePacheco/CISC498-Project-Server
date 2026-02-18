package com.aux_arena.models.tables;

import com.aux_arena.models.enums.RoundStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "GAME_ID")
    private Long gameId;

    @Column(name = "CREATED_AT")
    private Instant createdAt;

    @Column(name = "ROUND_STATUS")
    private RoundStatus roundStatus;

    @Column(name = "ROUND_NUMBER")
    private int roundNumber;

}
