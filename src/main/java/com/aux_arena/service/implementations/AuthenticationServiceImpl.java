package com.aux_arena.service.implementations;

import com.aux_arena.models.tables.User;
import com.aux_arena.repository.UserRepository;
import com.aux_arena.service.definitions.AuthenticationService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    UserRepository userRepository;

    @Override
    public User createNewUser(User user) {
        User newUser = userRepository.save(user);
        return newUser;
    }

    @Override
    public User loadUserByUsername(String email) {
        User user = userRepository.findUserByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return user;
    }


}
