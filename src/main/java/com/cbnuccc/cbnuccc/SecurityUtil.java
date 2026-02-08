package com.cbnuccc.cbnuccc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    private final String pepper;

    public SecurityUtil(@Value("${pepper}") String pepper) {
        this.pepper = pepper;
    }

    public String addPepper(String password) {
        return password + pepper;
    }
}
