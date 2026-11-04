package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import java.math.BigDecimal;

/** The four amounts of an invoice: all that summing a period needs to read. */
public interface InvoiceAmounts {

    BigDecimal getSubtotal();

    BigDecimal getVatAmount();

    BigDecimal getSurchargeAmount();

    BigDecimal getTotal();
}
