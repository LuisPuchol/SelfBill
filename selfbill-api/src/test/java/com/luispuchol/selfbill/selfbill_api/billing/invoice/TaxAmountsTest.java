package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class TaxAmountsTest {

    @Test
    void computesVatSurchargeAndTotal() {
        TaxAmounts amounts = TaxAmounts.of(new BigDecimal("100.00"), new BigDecimal("4"), new BigDecimal("0.5"));

        assertThat(amounts).isEqualTo(new TaxAmounts(new BigDecimal("4.00"), new BigDecimal("0.50"),
                new BigDecimal("104.50")));
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        TaxAmounts amounts = TaxAmounts.of(new BigDecimal("13.00"), BigDecimal.ZERO, new BigDecimal("0.5"));

        assertThat(amounts).isEqualTo(new TaxAmounts(new BigDecimal("0.00"), new BigDecimal("0.07"),
                new BigDecimal("13.07")));
    }
}
