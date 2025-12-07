package com.dAdK.dubAI.config;

import com.theokanning.openai.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAIConfig {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Bean
    public OpenAiService openAiService() {
        // You might want to configure a timeout here, e.g., Duration.ofSeconds(60)
        return new OpenAiService(openaiApiKey, Duration.ofSeconds(60));
    }
}