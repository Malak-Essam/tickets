package com.example.tickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;

import com.example.tickets.domain.User;
import com.example.tickets.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    private final UUID id = UUID.randomUUID();

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserProvisioningService service;

    private Jwt jwt(String sub, String preferredUsername, String email) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256");
        if (sub != null) builder.subject(sub);
        if (preferredUsername != null) builder.claim("preferred_username", preferredUsername);
        if (email != null) builder.claim("email", email);
        return builder.build();
    }

    @Test
    void upsert_createsUserFromJwtClaims() {
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.upsert(jwt(id.toString(), "malak", "malak@example.com"));

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("malak");
        assertThat(result.getEmail()).isEqualTo("malak@example.com");
    }

    @Test
    void upsert_updatesChangedFields() {
        User existing = User.builder().id(id).name("old-name").email("old@example.com").build();
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = service.upsert(jwt(id.toString(), "new-name", "new@example.com"));

        assertThat(result.getName()).isEqualTo("new-name");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(userRepository).save(existing);
    }

    @Test
    void upsert_doesNotSaveUnchangedUser() {
        User existing = User.builder().id(id).name("malak").email("malak@example.com").build();
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        User result = service.upsert(jwt(id.toString(), "malak", "malak@example.com"));

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void upsert_refetchesWhenInsertLosesRace() {
        User winner = User.builder().id(id).name("winner").email("winner@example.com").build();
        when(userRepository.findById(id)).thenReturn(Optional.empty()).thenReturn(Optional.of(winner));
        when(userRepository.saveAndFlush(any()))
            .thenThrow(new DataIntegrityViolationException("duplicate key"));

        User result = service.upsert(jwt(id.toString(), "loser", "loser@example.com"));

        assertThat(result).isSameAs(winner);
    }

    @Test
    void upsert_skipsWhenSubjectMissing() {
        User result = service.upsert(jwt(null, "malak", "malak@example.com"));

        assertThat(result).isNull();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void upsert_skipsWhenSubjectNotUuid() {
        User result = service.upsert(jwt("not-a-uuid", "malak", "malak@example.com"));

        assertThat(result).isNull();
        verify(userRepository, never()).findById(any());
    }
}
