package com.danny.taskmanager.controller;

import com.danny.taskmanager.dto.AuthLoginRequest;
import com.danny.taskmanager.dto.AuthResponse;
import com.danny.taskmanager.dto.UserSummaryDto;
import com.danny.taskmanager.model.User;
import com.danny.taskmanager.repository.UserRepository;
import com.danny.taskmanager.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            JwtService jwtService,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    private static UserSummaryDto toSummary(User u) {
        return new UserSummaryDto(u.getId(), u.getUsername(), u.getRole(), u.getEmail());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        UserDetails ud = userDetailsService.loadUserByUsername(req.username());
        String token = jwtService.generateToken(ud);
        User u = userRepository.findByUsername(req.username()).orElseThrow();
        return ResponseEntity.ok(new AuthResponse(token, toSummary(u)));
    }

    @GetMapping("/me")
    public UserSummaryDto me() {
        User u = userRepository.findByUsername(
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();
        return toSummary(u);
    }
}
