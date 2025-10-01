package com.dAdK.dubAI.services.sms;

import org.springframework.stereotype.Service;

@Service
public class SmsService {

    public void sendOtpSms(String phoneNumber, String otp) {
        // TODO: Integrate with an actual SMS gateway (e.g., Twilio)
        System.out.println("Sending OTP " + otp + " to " + phoneNumber);
    }
}
