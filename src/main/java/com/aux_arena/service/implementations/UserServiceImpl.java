package com.aux_arena.service.implementations;

import com.aux_arena.models.tables.User;
import com.aux_arena.repository.UserRepository;
import com.aux_arena.service.definitions.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

public class UserServiceImpl implements UserService {

    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByEmail(username);
        return user;
    }
}
