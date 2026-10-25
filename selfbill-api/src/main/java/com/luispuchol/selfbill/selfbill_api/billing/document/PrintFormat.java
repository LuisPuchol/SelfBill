package com.luispuchol.selfbill.selfbill_api.billing.document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** The Spanish number and date formatting of the printed documents. */
final class PrintFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormatSymbols SYMBOLS = symbols();

    private PrintFormat() {
    }

    private static DecimalFormatSymbols symbols() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("es", "ES"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return symbols;
    }

    /** Two decimals, "." for thousands and "," for decimals: {@code 1.234,56}. */
    static String number(BigDecimal value) {
        // DecimalFormat is not thread-safe, so each call gets its own
        DecimalFormat format = new DecimalFormat("#,##0.00", SYMBOLS);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }

    static String amountWithCurrency(BigDecimal value) {
        return number(value) + " €";
    }

    static String date(LocalDateTime value) {
        return DATE.format(value);
    }
}
