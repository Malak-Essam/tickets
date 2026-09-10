package com.example.tickets;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RoleAuthorizationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void anonymous_canBrowsePublishedEvents() throws Exception {
        mockMvc.perform(get("/api/v1/events/public"))
            .andExpect(status().isOk());
    }

    @Test
    void anonymous_gettingUnknownPublishedEvent_returns404NotUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/events/public/{id}", UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_cannotListEvents() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void userWithoutRole_cannotCreateEvent() throws Exception {
        String sub = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/events")
                .with(authenticatedIdentity(sub))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventPayload()))
            .andExpect(status().isForbidden());
    }

    @Test
    void attendee_cannotManageEvents() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .with(authenticated("ROLE_ATTENDEE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventPayload()))
            .andExpect(status().isForbidden());
    }

    @Test
    void staff_cannotManageEvents() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .with(authenticated("ROLE_STAFF"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventPayload()))
            .andExpect(status().isForbidden());
    }

    @Test
    void organizer_canCreateAndListOwnEvents() throws Exception {
        String sub = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/events")
                .with(authenticated("ROLE_ORGANIZER", sub))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventPayload()))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/events")
                .with(authenticated("ROLE_ORGANIZER", sub)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void organizer_cannotPurchaseTickets() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/purchase")
                .with(authenticated("ROLE_ORGANIZER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(purchasePayload(UUID.randomUUID())))
            .andExpect(status().isForbidden());
    }

    @Test
    void attendee_canPurchaseTickets() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/purchase")
                .with(authenticated("ROLE_ATTENDEE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(purchasePayload(UUID.randomUUID())))
            .andExpect(status().isNotFound());
    }

    private JwtRequestPostProcessor authenticated(String authority) {
        return authenticated(authority, UUID.randomUUID().toString());
    }

    private JwtRequestPostProcessor authenticatedIdentity(String sub) {
        return jwt().jwt(token -> token
                .subject(sub)
                .claim("preferred_username", "malak")
                .claim("email", "malak-%s@example.com".formatted(sub)));
    }

    private JwtRequestPostProcessor authenticated(String authority, String sub) {
        return jwt().jwt(token -> token
                .subject(sub)
                .claim("preferred_username", "malak")
                .claim("email", "malak-%s@example.com".formatted(sub)))
            .authorities(new SimpleGrantedAuthority(authority));
    }

    private String purchasePayload(UUID ticketTypeId) {
        return """
            {"ticketTypeId": "%s", "quantity": 1}
            """.formatted(ticketTypeId);
    }

    private String eventPayload() {
        return """
            {
              "name": "Tech Conference",
              "startDate": "2026-10-01T09:00:00",
              "endDate": "2026-10-01T18:00:00",
              "venue": "Convention Center",
              "salesStart": "2026-09-01T00:00:00",
              "salesEnd": "2026-09-30T23:59:59",
              "status": "DRAFT",
              "ticketTypes": [
                {"name": "General", "price": 50.00, "totalAvailable": 100, "description": "Standard entry"}
              ]
            }
            """;
    }
}