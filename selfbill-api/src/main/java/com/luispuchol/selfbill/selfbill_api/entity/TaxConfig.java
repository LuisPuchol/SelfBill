package com.luispuchol.selfbill.selfbill_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "vat_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatPercentage;

    @Column(name = "surcharge_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal surchargePercentage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
