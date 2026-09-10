package com.example.tickets.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAuthoritiesConverterTest {

    private final JwtAuthoritiesConverter converter = new JwtAuthoritiesConverter();

    private Jwt jwt(String claimName, Object claimValue) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim(claimName, claimValue)
            .build();
    }

    @Test
    void convert_mapsRealmAccessRoles() {
        Jwt jwt = jwt("realm_access", Map.of("roles", List.of("ROLE_ORGANIZER", "ROLE_STAFF", "ROLE_ATTENDEE")));

        assertThat(converter.convert(jwt))
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ORGANIZER", "ROLE_STAFF", "ROLE_ATTENDEE");
    }

    @Test
    void convert_prefixesRoleWhenMissing() {
        Jwt jwt = jwt("realm_access", Map.of("roles", List.of("organizer")));

        assertThat(converter.convert(jwt))
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_organizer");
    }

    @Test
    void convert_fallsBackToRolesClaim() {
        Jwt jwt = jwt("roles", List.of("ROLE_ATTENDEE"));

        assertThat(converter.convert(jwt))
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ATTENDEE");
    }

    @Test
    void convert_returnsEmptyWhenNoRoleClaims() {
        Jwt jwt = jwt("email", "malak@example.com");

        assertThat(converter.convert(jwt)).isEmpty();
    }
}