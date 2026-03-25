package me.dhiya.hr.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "currencies")
public class CurrencyEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    private String id;

    @Column(name = "code", unique = true, nullable = false, length = 3)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
