package com.dAdK.dubAI.services.authservice;

import com.dAdK.dubAI.dto.userdto.LoginRequestDTO;

import java.io.IOException;
import java.security.GeneralSecurityException;


import com.dAdK.dubAI.dto.userdto.OtpVerificationRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserRequestDTO;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;


public interface AuthService {
    String login(LoginRequestDTO loginRequestDTO);

    String googleLogin(String idToken) throws GeneralSecurityException, IOException;

    Map<String, String> registerUser(UserRequestDTO userRequestDTO, String ipAddress) throws IOException;

    Map<String, String> verifyOtp(OtpVerificationRequestDTO otpVerificationRequestDTO);
}
