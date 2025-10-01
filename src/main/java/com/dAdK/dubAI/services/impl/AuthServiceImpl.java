package com.dAdK.dubAI.services.impl;

import com.dAdK.dubAI.dto.userdto.LoginRequestDTO;
import com.dAdK.dubAI.models.User;
import com.dAdK.dubAI.repository.UserRepository;
import com.dAdK.dubAI.services.authservice.AuthService;
import com.dAdK.dubAI.util.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public String login(LoginRequestDTO loginRequestDTO) {
        String identifier = loginRequestDTO.getIdentifier();
        User user = userRepository.findByUsernameOrEmailOrContactNumber(identifier,identifier,identifier).orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return jwtService.generateToken(user);
    }
}