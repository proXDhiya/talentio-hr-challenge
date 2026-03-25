package me.dhiya.hr.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/apis/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/apis/v1/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/apis/v1/currencies").hasAnyRole("MANAGER", "HR")
                .requestMatchers(HttpMethod.GET, "/apis/v1/employees").hasAnyRole("MANAGER", "HR")
                .requestMatchers(HttpMethod.GET, "/apis/v1/employees/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/apis/v1/employees/*").hasAnyRole("MANAGER", "HR")
                .requestMatchers(HttpMethod.PUT, "/apis/v1/employees/*").hasAnyRole("MANAGER", "HR")
                .requestMatchers(HttpMethod.DELETE, "/apis/v1/employees/*").hasAnyRole("MANAGER", "HR")
                .requestMatchers(HttpMethod.POST, "/apis/v1/employees").hasAnyRole("MANAGER", "HR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("""
                            {"message":"Authentication required. Please login.","errors":[],"timestamp":"%s"}"""
                            .formatted(Instant.now()));
                })
                .accessDeniedHandler((request, response, e) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("""
                            {"message":"Access denied.","errors":[],"timestamp":"%s"}"""
                            .formatted(Instant.now()));
                })
            );
        return http.build();
    }
}
