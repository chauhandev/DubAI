package com.dAdK.dubAI.services.email;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final SendGrid sendGrid;

    @Value("${app.email.sender}")
    private String senderEmail;

    public EmailService(@Value("${sendgrid.api.key}") String sendgridApiKey) {
        this.sendGrid = new SendGrid(sendgridApiKey);
    }

    public void sendOtpEmail(String to, String otp) throws IOException {
        if (senderEmail == null || senderEmail.isBlank()) {
            throw new IllegalStateException("Sender email is not configured.");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email is missing.");
        }

        Email from = new Email(senderEmail);
        Email recipient = new Email(to);
        String subject = "Your OTP Code";
        Content content = new Content("text/plain", "Your OTP code is: " + otp);
        Mail mail = new Mail(from, subject, recipient, content);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sendGrid.api(request);

        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            logger.error("❌ Failed to send OTP email to {}. Status code: {}, Body: {}",
                    to, response.getStatusCode(), response.getBody());
            throw new IOException("Failed to send OTP email. Status code: "
                    + response.getStatusCode() + ", Body: " + response.getBody());
        }

        logger.info("✅ OTP email sent successfully to {}", to);
    }
}
