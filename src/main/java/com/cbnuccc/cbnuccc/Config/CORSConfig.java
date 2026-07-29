package com.cbnuccc.cbnuccc.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CORSConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 아래 origin을 제외한 다른 origin에서의 접근 차단
        String[] origins = { "https://cbnuccc.co.kr", "https://cbnuccc-frontend.vercel.app" };
        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedHeaders("*")
                .allowedMethods("*");
    }
}
