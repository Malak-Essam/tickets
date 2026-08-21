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
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends Auditable {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @OneToMany(mappedBy = "purchaser")
    @Builder.Default
    private ArrayList<Ticket> purchasedTickets = new ArrayList<>();

    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL)
    @Builder.Default
    private ArrayList<Event> organizedEvents = new ArrayList<>();

    @ManyToMany(mappedBy = "attendees")
    @Builder.Default
    private ArrayList<Event> attendingEvents = new ArrayList<>();

    @ManyToMany(mappedBy = "staff")
    @Builder.Default
    private ArrayList<Event> staffingEvents = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id)
            && Objects.equals(name, user.name)
            && Objects.equals(email, user.email)
            && Objects.equals(getCreatedAt(), user.getCreatedAt())
            && Objects.equals(getUpdatedAt(), user.getUpdatedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, getCreatedAt(), getUpdatedAt());
    }
}
