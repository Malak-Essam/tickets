package com.example.tickets.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.example.tickets.filter.UserProvisioningFilter;
import com.example.tickets.service.UserProvisioningService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserProvisioningService userProvisioningService)
        throws Exception {
        UserProvisioningFilter userProvisioningFilter = new UserProvisioningFilter(userProvisioningService);
        return http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
            .addFilterAfter(userProvisioningFilter, BearerTokenAuthenticationFilter.class)
            .build();
    }
}
