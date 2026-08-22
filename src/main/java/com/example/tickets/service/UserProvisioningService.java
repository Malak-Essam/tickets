package com.example.tickets.service;

import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.example.tickets.domain.User;
import com.example.tickets.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProvisioningService {

    private final UserRepository userRepository;

    public User upsert(Jwt jwt) {
        UUID id = parseSubject(jwt);
        if (id == null) {
            return null;
        }
        String name = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        return userRepository.findById(id)
            .map(existing -> updateIfChanged(existing, name, email))
            .orElseGet(() -> createUser(id, name, email));
    }

    private UUID parseSubject(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null) {
            log.warn("JWT without subject; skipping user provisioning");
            return null;
        }
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            log.warn("JWT subject is not a UUID: {}; skipping user provisioning", sub);
            return null;
        }
    }

    private User updateIfChanged(User existing, String name, String email) {
        boolean changed = false;
        if (!Objects.equals(existing.getName(), name)) {
            existing.setName(name);
            changed = true;
        }
        if (!Objects.equals(existing.getEmail(), email)) {
            existing.setEmail(email);
            changed = true;
        }
        return changed ? userRepository.save(existing) : existing;
    }

    private User createUser(UUID id, String name, String email) {
        try {
            return userRepository.saveAndFlush(User.builder().id(id).name(name).email(email).build());
        } catch (DataIntegrityViolationException e) {
            log.debug("Lost insert race for user {}; refetching", id);
            return userRepository.findById(id).orElseThrow();
        }
    }
}
