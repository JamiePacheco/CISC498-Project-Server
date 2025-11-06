package com.aux_arena.models.tables;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class LobbyUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_lobby")
    @ManyToOne(fetch = FetchType.LAZY)
    private GameLobby gameLobby;

    @Column(name = "user")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "last_socket_connection_ id")
    private String lastSocketConnectionId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return this.nickname;
    }
}
