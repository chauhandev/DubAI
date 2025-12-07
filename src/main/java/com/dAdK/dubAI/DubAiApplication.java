package com.dAdK.dubAI;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "DubAI APIs",
        version = "1.0",
        description = "API documentation for DubAI"
))
@EnableScheduling
public class DubAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DubAiApplication.class, args);
    }

}
