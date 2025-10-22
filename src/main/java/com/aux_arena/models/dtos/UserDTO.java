package com.aux_arena.models.dtos;

import com.aux_arena.models.tables.User;

import java.util.Set;

public class UserDTO {
    private Long id;

    private String email;

    public UserDTO(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
    }
}
