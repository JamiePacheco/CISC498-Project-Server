package com.aux_arena.models;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "EMAIL")
    private String username;

    @Column(name = "PASSWORD")
    private String password;



}
