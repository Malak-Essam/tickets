package com.example.tickets.domain;

import com.example.tickets.domain.base.Auditable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Column(nullable = false)
    private String name;

    @Setter
    @Column(nullable = false)
    private LocalDateTime startDate;

    @Setter
    @Column(nullable = false)
    private LocalDateTime endDate;

    @Setter
    @Column(nullable = false)
    private String venue;

    @Setter
    @Column(nullable = false)
    private LocalDateTime salesStart;

    @Setter
    @Column(nullable = false)
    private LocalDateTime salesEnd;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatusEnum status;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @ManyToMany
    @JoinTable(
        name = "event_attendees",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private List<User> attendees = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "event_staff",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private List<User> staff = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    @Builder.Default
    private List<TicketType> ticketTypes = new ArrayList<>();

    public void addTicketType(TicketType ticketType) {
        ticketTypes.add(ticketType);
        ticketType.setEvent(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id)
            && Objects.equals(name, event.name)
            && Objects.equals(startDate, event.startDate)
            && Objects.equals(endDate, event.endDate)
            && Objects.equals(venue, event.venue)
            && Objects.equals(salesStart, event.salesStart)
            && Objects.equals(salesEnd, event.salesEnd)
            && status == event.status
            && Objects.equals(getCreatedAt(), event.getCreatedAt())
            && Objects.equals(getUpdatedAt(), event.getUpdatedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, startDate, endDate, venue, salesStart, salesEnd, status, getCreatedAt(), getUpdatedAt());
    }
}