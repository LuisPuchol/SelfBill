package com.luispuchol.selfbill.selfbill_api.catalog.client;

import org.hibernate.annotations.SQLDelete;

import com.luispuchol.selfbill.selfbill_api.shared.SearchText;
import com.luispuchol.selfbill.selfbill_api.shared.SoftDeletableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// No @SQLRestriction: documents keep loading deleted rows (CONCEPTO.md §7)
@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE clients SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Client extends SoftDeletableEntity {

    @Column(nullable = false)
    private Integer code;

    @Column(name = "business_name", nullable = false, length = 255)
    private String businessName;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 20)
    private String nif;

    @Column(length = 50)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String province;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(length = 20)
    private String phone;

    @Column(length = 20)
    private String phone2;

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_type", nullable = false)
    @Builder.Default
    private VatType vatType = VatType.WITH_VAT;

    @Enumerated(EnumType.STRING)
    @Column(name = "surcharge_type", nullable = false)
    @Builder.Default
    private SurchargeType surchargeType = SurchargeType.WITHOUT_SURCHARGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_delivery_note_format", nullable = false)
    @Builder.Default
    private DeliveryNoteFormat defaultDeliveryNoteFormat = DeliveryNoteFormat.PLAIN;

    @Column(name = "search_text", nullable = false)
    private String searchText;

    @PrePersist
    @PreUpdate
    void refreshSearchText() {
        searchText = SearchText.normalize(name, businessName, nif);
    }
}
