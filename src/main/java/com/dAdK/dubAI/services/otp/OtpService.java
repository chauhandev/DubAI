package com.dAdK.dubAI.services.otp;

import com.dAdK.dubAI.models.User;

import java.util.Optional;

public interface OtpService {

    Optional<User> findByUserId(String userId);
    String generateOtp(User user);
    boolean validateOtp(User user, String otp);
}
