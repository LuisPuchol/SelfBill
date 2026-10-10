package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import java.math.BigDecimal;

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
@Table(name = "tax_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxConfig extends BaseEntity {

    @Column(name = "vat_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatPercentage;

    @Column(name = "surcharge_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal surchargePercentage;
}
