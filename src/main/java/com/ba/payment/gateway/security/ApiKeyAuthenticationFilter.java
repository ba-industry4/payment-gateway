package com.ba.payment.gateway.security;

import com.ba.payment.gateway.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String apiKeyHeader = request.getHeader("X-API-Key");

        if (apiKeyHeader != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            apiKeyRepository.findByKeyValue(apiKeyHeader).ifPresent(apiKey -> {
                if (apiKey.getActive() && 
                    (apiKey.getExpiresAt() == null || apiKey.getExpiresAt().isAfter(LocalDateTime.now()))) {
                    
                    // Update last used timestamp
                    apiKey.setLastUsedAt(LocalDateTime.now());
                    apiKeyRepository.save(apiKey);

                    var authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_API_USER")
                    );

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            apiKey.getUser(),
                            null,
                            authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            });
        }

        filterChain.doFilter(request, response);
    }
}
