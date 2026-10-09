package com.luispuchol.selfbill.selfbill_api.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import com.luispuchol.selfbill.selfbill_api.enums.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE clients SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
