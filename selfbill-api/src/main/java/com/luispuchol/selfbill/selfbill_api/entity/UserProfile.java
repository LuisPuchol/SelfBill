package com.luispuchol.selfbill.selfbill_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "fiscal_name", nullable = false, length = 255)
    private String fiscalName;

    @Column(length = 20)
    private String nif;

    @Column(length = 500)
    private String address;

    @Column(length = 50)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "smtp_host", length = 255)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "mail_password_encrypted", length = 500)
    private String mailPasswordEncrypted;

    @Column(name = "logo_data")
    private byte[] logoData;

    @Column(name = "logo_content_type", length = 50)
    private String logoContentType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
