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

import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE clients SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer code;

    // Business and personal details
    @Column(name = "business_name", nullable = false, length = 255)
    private String businessName;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 20)
    private String nif;

    // Contact details
    @Column(length = 50)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "phone2", length = 20)
    private String phone2;

    // Client options
    @Column(name = "customer_type", nullable = false)
    private Boolean customerType = false;

    @Column(name = "equivalence_surcharge", nullable = false)
    private Boolean equivalenceSurcharge = false;

    @Column(name = "invoice_per_delivery_note", nullable = false)
    private Boolean invoicePerDeliveryNote = false;

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