package com.example.tickets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.example.tickets.filter.UserProvisioningFilter;
import com.example.tickets.service.UserProvisioningService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtAuthoritiesConverter());
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserProvisioningService userProvisioningService,
        JwtAuthenticationConverter jwtAuthenticationConverter)
        throws Exception {
        UserProvisioningFilter userProvisioningFilter = new UserProvisioningFilter(userProvisioningService);
        return http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/error").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/public", "/api/v1/events/public/**").permitAll()
                .requestMatchers("/api/v1/events/**").hasRole("ORGANIZER")
                .requestMatchers(HttpMethod.POST, "/api/v1/tickets/purchase").hasRole("ATTENDEE")
                .anyRequest().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .addFilterAfter(userProvisioningFilter, BearerTokenAuthenticationFilter.class)
            .build();
    }
}