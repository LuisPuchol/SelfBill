package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;
import com.luispuchol.selfbill.selfbill_api.shared.SoftDeletableEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE invoices SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Invoice extends SoftDeletableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceSeries series;

    private Integer code;

    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "client_business_name", length = 255)
    private String clientBusinessName;

    @Column(name = "client_nif", length = 20)
    private String clientNif;

    @Column(name = "client_address", length = 500)
    private String clientAddress;

    @Column(name = "client_city", length = 100)
    private String clientCity;

    @Column(name = "client_province", length = 100)
    private String clientProvince;

    @Column(name = "client_postal_code", length = 10)
    private String clientPostalCode;

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    @Builder.Default
    private List<DeliveryNote> deliveryNotes = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("deliveryNoteDate, deliveryNoteCode, id")
    @Builder.Default
    private List<InvoiceLine> lines = new ArrayList<>();

    @Column(precision = 10, scale = 2)
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

    public String getNumber() {
        return code == null ? null : series.format(code);
    }
}
