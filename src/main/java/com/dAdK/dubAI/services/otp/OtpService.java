package com.dAdK.dubAI.services.otp;

import com.dAdK.dubAI.models.Otp;

import java.util.Optional;

public interface OtpService {

    void generateAndSendOtp(String userId, String target, String type);

    void resendOtp(String userId, String target, String type);

    boolean validateOtp(String userId, String otp, String type);

    void deleteOtpsByUserId(String userId);

    boolean hasValidOtp(String userId, String type);

    Optional<Otp> findByUserId(String userId);
}
