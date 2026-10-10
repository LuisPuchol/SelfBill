package com.luispuchol.selfbill.selfbill_api.settings.profile;

import com.luispuchol.selfbill.selfbill_api.shared.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends BaseEntity {

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
}
