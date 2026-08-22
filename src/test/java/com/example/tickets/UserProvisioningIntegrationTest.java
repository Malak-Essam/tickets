package com.example.tickets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tickets.filter.UserProvisioningFilter;
import com.example.tickets.domain.User;
import com.example.tickets.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(UserProvisioningIntegrationTest.TestUserController.class)
class UserProvisioningIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Test
    void authenticatedRequest_provisionsUserRow() throws Exception {
        UUID sub = UUID.randomUUID();

        mockMvc.perform(get("/test/me").with(jwt().jwt(jwtToken -> jwtToken
                .subject(sub.toString())
                .claim("preferred_username", "malak")
                .claim("email", "malak@example.com"))))
            .andExpect(status().isOk())
            .andExpect(content().string("malak"));

        assertThat(userRepository.findById(sub)).hasValueSatisfying(user -> {
            assertThat(user.getName()).isEqualTo("malak");
            assertThat(user.getEmail()).isEqualTo("malak@example.com");
        });
    }

    @Test
    void secondRequest_updatesChangedClaims() throws Exception {
        UUID sub = UUID.randomUUID();
        var token = jwt().jwt(jwtToken -> jwtToken
            .subject(sub.toString())
            .claim("preferred_username", "before")
            .claim("email", "before@example.com"));

        mockMvc.perform(get("/test/me").with(token)).andExpect(status().isOk());

        mockMvc.perform(get("/test/me").with(jwt().jwt(jwtToken -> jwtToken
                .subject(sub.toString())
                .claim("preferred_username", "after")
                .claim("email", "after@example.com"))))
            .andExpect(status().isOk())
            .andExpect(content().string("after"));

        assertThat(userRepository.findById(sub)).hasValueSatisfying(user -> {
            assertThat(user.getName()).isEqualTo("after");
            assertThat(user.getEmail()).isEqualTo("after@example.com");
        });
    }

    @RestController
    static class TestUserController {

        @GetMapping("/test/me")
        String me(HttpServletRequest request) {
            User user = (User) request.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE);
            return user.getName();
        }
    }
}
