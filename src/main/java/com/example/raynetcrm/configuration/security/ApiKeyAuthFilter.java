package com.example.raynetcrm.configuration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Authenticates requests carrying a valid {@value #API_KEY_HEADER} header.
 *
 * <p>On a match an authenticated token is placed in the {@link SecurityContextHolder},
 * otherwise the context is left empty.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    static final String API_KEY_HEADER = "X-API-Key";

    private final byte[] expectedKey;

    public ApiKeyAuthFilter(ApiKeyProperties apiKeyProperties) {
        this.expectedKey = apiKeyProperties.getApiKey().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String presentedKey = request.getHeader(API_KEY_HEADER);
        if (StringUtils.hasText(presentedKey) && matchesExpectedKey(presentedKey)) {
            Authentication authentication = new PreAuthenticatedAuthenticationToken(
                    "api-key-client", null, AuthorityUtils.NO_AUTHORITIES);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Constant-time comparison to avoid leaking the key through response timing.
     */
    private boolean matchesExpectedKey(String presentedKey) {
        return MessageDigest.isEqual(presentedKey.getBytes(StandardCharsets.UTF_8), expectedKey);
    }
}