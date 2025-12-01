package com.aux_arena.service.implementations;

import com.aux_arena.models.enums.Roles;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.models.tables.User;
import com.aux_arena.repository.LobbyUserRepository;
import com.aux_arena.repository.UserRepository;
import com.aux_arena.service.definitions.AuthenticationService;
import com.aux_arena.service.definitions.LobbyUserService;
import com.aux_arena.utility.UuidGenerator;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final PasswordEncoder passwordEncoder;
    UserRepository userRepository;

    LobbyUserRepository lobbyUserRepository;

    GameLobbyServiceImpl gameLobbyServiceImpl;

    @Override
    public User createNewUser(User user) {
        if (userRepository.findUserByEmail(user.getEmail()) != null) {
            throw new UsernameNotFoundException("User with email " + user.getEmail() + " already exists");
        }

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        User newUser = userRepository.save(user);
        return newUser;
    }

    @Override
    public LobbyUser createNewLobbyUser(String username, String lobbyCode, Boolean isAuthor) {
        GameLobby joinedGameLobby = gameLobbyServiceImpl.getGameLobby(lobbyCode, "");
        if (joinedGameLobby == null) {
            throw new RuntimeException(String.format("Game lobby '%s' does not exist", lobbyCode));
        }

        LobbyUser lobbyUser = LobbyUser.builder()
                .gameLobby(joinedGameLobby)
                .role(Roles.GUEST)
                .nickname(username)
                .user(null)
                .guestIdentifier(username + UuidGenerator.generateUuid())
                .joinedAt(Instant.now())
                .build();

        LobbyUser savedUser = lobbyUserRepository.save(lobbyUser);

        if (isAuthor && joinedGameLobby.getAuthor() == null) {
            joinedGameLobby.setAuthor(savedUser);
        }

        return savedUser;
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
