package com.luispuchol.selfbill.selfbill_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_lines")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE invoice_lines SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // Nullable FK: traceability to the source delivery note; set to null if the delivery note is deleted
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_note_id")
    private DeliveryNote deliveryNote;

    // Snapshot fields — delivery note data frozen at invoice creation for grouping and display,
    // independent of the delivery note lifecycle
    @Column(name = "delivery_note_code", nullable = false)
    private Integer deliveryNoteCode;

    @Column(name = "delivery_note_date", nullable = false)
    private LocalDateTime deliveryNoteDate;

    // Nullable: the article may be deleted but the invoice line must be preserved
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private Article article;

    // Snapshot fields — frozen at invoice creation, independent of future article changes
    @Column(name = "article_name", nullable = false, length = 255)
    private String articleName;

    @Column(name = "article_code", nullable = false)
    private Integer articleCode;

    @Column(name = "trazability_code1", nullable = false)
    private Integer trazabilityCode1;

    @Column(name = "trazability_code2")
    private Integer trazabilityCode2;

    @Column(name = "trazability_code3")
    private Integer trazabilityCode3;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

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
}
