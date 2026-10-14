package com.pashumandi.milk.config;

import com.pashumandi.milk.security.CustomAccessDeniedHandler;
import com.pashumandi.milk.security.JwtAuthenticationEntryPoint;
import com.pashumandi.milk.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        // OpenAPI / Swagger documentation endpoints
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Health endpoints
                        .requestMatchers("/actuator/health").permitAll()

                        // Protected Seller Listings Endpoint (MUST precede general GET listings rules)
                        .requestMatchers(HttpMethod.GET, "/api/v1/milk/listings/my-listings").authenticated()

                        // Public Milk Browsing & Search Endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/milk/listings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/milk/listings/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/milk/listings/*").permitAll()

                        // Protected Milk Listing Management (Create, Update, Deactivate, Status)
                        .requestMatchers(HttpMethod.POST, "/api/v1/milk/listings").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/milk/listings/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/milk/listings/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/milk/listings/*/status").authenticated()

                        // Orders (One-time orders, lifecycle, cancellation)
                        .requestMatchers("/api/v1/milk/orders/**").authenticated()

                        // Subscriptions (Recurring milk subscriptions)
                        .requestMatchers("/api/v1/milk/subscriptions/**").authenticated()

                        // Favorites
                        .requestMatchers("/api/v1/milk/favorites/**").authenticated()

                        // Inquiries
                        .requestMatchers("/api/v1/milk/inquiries/**").authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
