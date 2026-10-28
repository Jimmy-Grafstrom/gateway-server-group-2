package se.iths.gatewayservergroup2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration class for the gateway server.
 * Disables CSRF, sets session management to stateless, and permits all incoming gateway requests
 * since authentication tokens and cookies are managed and verified via proxying.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures the security filter chain for HTTP requests.
     *
     * @param http the {@link HttpSecurity} to modify
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/gateway/auth/**").permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}