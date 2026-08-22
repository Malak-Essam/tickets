package com.example.tickets.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.example.tickets.domain.User;
import com.example.tickets.service.UserProvisioningService;

@ExtendWith(MockitoExtension.class)
class UserProvisioningFilterTest {

    private final UUID id = UUID.randomUUID();

    @Mock
    UserProvisioningService userProvisioningService;

    @InjectMocks
    UserProvisioningFilter filter;

    MockHttpServletRequest request;
    MockHttpServletResponse response;
    MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_skipsWhenNotAuthenticated() throws ServletException, java.io.IOException {
        filter.doFilter(request, response, filterChain);

        verify(userProvisioningService, never()).upsert(any());
        assertThat(request.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE)).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    void doFilter_provisionsAndExposesCurrentUser() throws ServletException, java.io.IOException {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(id.toString())
            .claim("preferred_username", "malak")
            .claim("email", "malak@example.com")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        User user = User.builder().id(id).name("malak").email("malak@example.com").build();
        when(userProvisioningService.upsert(jwt)).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        verify(userProvisioningService).upsert(jwt);
        assertThat(request.getAttribute(UserProvisioningFilter.CURRENT_USER_ATTRIBUTE)).isSameAs(user);
        assertThat(filterChain.getRequest()).isNotNull();
    }
}
