package com.aux_arena.controller.rest;

import com.aux_arena.utility.JwtUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class Authentication {

    @NonNull
    private final AuthenticationManager authenticationManager;

    @NonNull
    private final JwtUtil





}
