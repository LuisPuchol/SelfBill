package com.luispuchol.selfbill.selfbill_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.luispuchol.selfbill.selfbill_api.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE invoices SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private Integer code;

    @Column(nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.ACTIVE;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // All delivery notes included in this invoice (1 for PER_DELIVERY_NOTE, N for
    // GROUPED)
    @ToString.Exclude
    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    @Builder.Default
    private List<DeliveryNote> deliveryNotes = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLine> lines = new ArrayList<>();

    // Financial fields
    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "vat_percentage", precision = 5, scale = 2)
    private BigDecimal vatPercentage;

    @Column(name = "vat_amount", precision = 10, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "surcharge_percentage", precision = 5, scale = 2)
    private BigDecimal surchargePercentage;

    @Column(name = "surcharge_amount", precision = 10, scale = 2)
    private BigDecimal surchargeAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /*
     * TODO campos Verifactu (añadir cuando implementes la integración)
     * 
     * @Column(nullable = false, length = 20)
     * private String series; // "2026", "A", etc.
     * 
     * @Enumerated(EnumType.STRING)
     * 
     * @Column(name = "invoice_type", nullable = false)
     * private InvoiceType invoiceType; // F1, F2, R1...R5
     * 
     * @Column(name = "verifactu_hash", length = 64)
     * private String verifactuHash; // SHA-256 hex
     * 
     * @Column(name = "verifactu_timestamp")
     * private LocalDateTime verifactuTimestamp;
     * 
     * // FK a la factura ANTERIOR en la serie (para construir el hash, no a la
     * rectificada)
     * 
     * @ManyToOne(fetch = FetchType.LAZY)
     * 
     * @JoinColumn(name = "previous_invoice_id")
     * private Invoice previousInvoice;
     * 
     * @Column(name = "verifactu_status", length = 20)
     * private String verifactuStatus; // PENDING | SENT | ACCEPTED | REJECTED
     */
}
