package com.cbnuccc.cbnuccc.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;

@ConfigurationProperties(prefix = "supabase")
@Getter
public class SupabaseProperties {
    private final String url;
    private final String key;

    public SupabaseProperties(String url, String key) {
        this.url = url;
        this.key = key;
    }
}
