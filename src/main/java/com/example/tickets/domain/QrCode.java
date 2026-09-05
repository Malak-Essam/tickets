package com.example.tickets.domain;

import com.example.tickets.domain.base.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "qr_codes")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCode extends Auditable {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QrCodeStatusEnum status;

    @Column(nullable = false, columnDefinition = "TEXT", name = "\"value\"")
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QrCode qrCode = (QrCode) o;
        return Objects.equals(id, qrCode.id)
            && status == qrCode.status
            && Objects.equals(value, qrCode.value)
            && Objects.equals(getCreatedAt(), qrCode.getCreatedAt())
            && Objects.equals(getUpdatedAt(), qrCode.getUpdatedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, value, getCreatedAt(), getUpdatedAt());
    }
}
