package com.dAdK.dubAI;

import com.dAdK.dubAI.repository.OtpRepository;
import com.dAdK.dubAI.repository.UserRepository;
import com.dAdK.dubAI.services.RateLimiting.RateLimitService;
import com.dAdK.dubAI.services.email.EmailService;
import com.dAdK.dubAI.services.otp.OtpService;
import com.dAdK.dubAI.services.sms.SmsService;
import com.dAdK.dubAI.services.userservice.UserService;
import com.dAdK.dubAI.util.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class DubAiApplicationTests {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private JwtService jwtService;
	@Autowired
	private UserService userService;
	@Autowired
	private OtpService otpService;
	@Autowired
	private RateLimitService rateLimitService;
	@Autowired
	private OtpRepository otpRepository;
	@Autowired
	private EmailService emailService;
	@Autowired
	private SmsService smsService;

	@Configuration
	static class TestConfig {
		@Bean
		@Primary // Use @Primary to ensure this mock is chosen if multiple beans of the same type exist
		public UserRepository userRepository() {
			return Mockito.mock(UserRepository.class);
		}

		@Bean
		@Primary
		public PasswordEncoder passwordEncoder() {
			return Mockito.mock(PasswordEncoder.class);
		}

		@Bean
		@Primary
		public JwtService jwtService() {
			return Mockito.mock(JwtService.class);
		}

		@Bean
		@Primary
		public UserService userService() {
			return Mockito.mock(UserService.class);
		}

		@Bean
		@Primary
		public OtpService otpService() {
			return Mockito.mock(OtpService.class);
		}

		@Bean
		@Primary
		public RateLimitService rateLimitService() {
			return Mockito.mock(RateLimitService.class);
		}

		@Bean
		@Primary
		public OtpRepository otpRepository() {
			return Mockito.mock(OtpRepository.class);
		}

		@Bean
		@Primary
		public EmailService emailService() {
			return Mockito.mock(EmailService.class);
		}

		@Bean
		@Primary
		public SmsService smsService() {
			return Mockito.mock(SmsService.class);
		}
	}


	@Test
	void contextLoads() {
	}

}