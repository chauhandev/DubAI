package com.dAdK.dubAI.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*"); // or your frontend URL
        config.addAllowedHeader("*");        // includes Authorization
        config.addAllowedMethod("*");        // GET, POST, OPTIONS
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
