package com.ledgerline.service;

import com.ledgerline.dto.*;
import com.ledgerline.entity.*;
import com.ledgerline.exception.BusinessException;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, AuthenticationManager authManager, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.authManager = authManager; this.jwt = jwt;
    }

    /** Public registration ALWAYS creates ACCOUNT_HOLDER. A client can never choose its own role. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw BusinessException.conflict("EMAIL_EXISTS", "An account with this email already exists");
        User u = new User();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(req.password()));
        u.setRole(Role.ACCOUNT_HOLDER);
        users.save(u);
        return toResponse(u);
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        authManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
        return toResponse(users.findByEmail(email).orElseThrow(() -> new BadCredentialsException("x")));
    }

    public UserDTO me(String email) {
        return UserDTO.from(users.findByEmail(email).orElseThrow(() -> new BadCredentialsException("x")));
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwt.generateToken(u.getEmail(), u.getRole().name()), "Bearer", jwt.ttlSeconds(), UserDTO.from(u));
    }
}
