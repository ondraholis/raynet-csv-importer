package com.example.raynetcrm.configuration.security;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Holds the API key required to call the upload endpoint.
 *
 * <p>The application fails to start when the key is blank, so the endpoint can never be
 * exposed without authentication by accident.
 */
@Getter
@Component
public class ApiKeyProperties {

    private final String apiKey;

    public ApiKeyProperties(@Value("${security.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "security.api-key (env IMPORT_API_KEY) must be set.");
        }
    }
}