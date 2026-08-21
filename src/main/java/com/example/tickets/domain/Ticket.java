package com.example.tickets.domain;

import com.example.tickets.domain.base.Auditable;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tickets")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id")
    private TicketType ticketType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchaser_id")
    private User purchaser;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    @Builder.Default
    private ArrayList<TicketValidation> validations = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    @Builder.Default
    private ArrayList<QrCode> qrCodes = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        return Objects.equals(id, ticket.id)
            && status == ticket.status
            && Objects.equals(getCreatedAt(), ticket.getCreatedAt())
            && Objects.equals(getUpdatedAt(), ticket.getUpdatedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, getCreatedAt(), getUpdatedAt());
    }
}
