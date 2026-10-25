package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * VAT, surcharge and total of a base, computed the one way invoices, pending invoices and valued
 * delivery notes all share: each amount is {@code base × % / 100} rounded half up to two decimals.
 */
public record TaxAmounts(BigDecimal vatAmount, BigDecimal surchargeAmount, BigDecimal total) {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    public static TaxAmounts of(BigDecimal base, BigDecimal vatPercentage, BigDecimal surchargePercentage) {
        BigDecimal vatAmount = percentageOf(base, vatPercentage);
        BigDecimal surchargeAmount = percentageOf(base, surchargePercentage);
        return new TaxAmounts(vatAmount, surchargeAmount, base.add(vatAmount).add(surchargeAmount));
    }

    private static BigDecimal percentageOf(BigDecimal amount, BigDecimal percentage) {
        return amount.multiply(percentage).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
