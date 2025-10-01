package com.dAdK.dubAI.services.authservice;

import com.dAdK.dubAI.dto.userdto.LoginRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserResponseDTO;

import java.util.Map;

public interface AuthService {
    String login(LoginRequestDTO loginRequestDTO);
}