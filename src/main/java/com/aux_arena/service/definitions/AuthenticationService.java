package com.aux_arena.service.definitions;

import com.aux_arena.models.tables.User;
import org.springframework.stereotype.Service;

@Service
public interface AuthenticationService {
    User createNewUser(User user);

    User loadUserByUsername(String email);
}