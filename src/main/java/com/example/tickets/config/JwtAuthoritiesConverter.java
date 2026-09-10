package com.example.tickets.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_KEY = "roles";
    private static final String FALLBACK_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        return extractRoles(jwt).stream()
            .map(this::toAuthority)
            .toList();
    }

    private List<String> extractRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess instanceof Map<?, ?> map && map.get(ROLES_KEY) instanceof Collection<?> roles) {
            return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
        }
        List<String> fallback = jwt.getClaimAsStringList(FALLBACK_CLAIM);
        return fallback == null ? Collections.emptyList() : fallback;
    }

    private GrantedAuthority toAuthority(String role) {
        return new SimpleGrantedAuthority(role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role);
    }
}