package com.cbnuccc.cbnuccc.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CORSConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // disallow entering from all origins except following origins:
        String[] origins = { "https://cbnuccc.co.kr", "https://cbnuccc-frontend.vercel.app" };
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedHeaders("*")
                .allowedMethods("*");
    }
}
