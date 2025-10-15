package com.aux_arena.models;

import com.aux_arena.models.enums.GameMode;
import com.aux_arena.models.enums.GameStatus;
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
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(value = "GAME_MODE")
    private GameMode gameMode;

    @Column(value = "GAME_STATUS")
    private GameStatus gameStatus;

    @Column(value = "WINNER")
    private

}
