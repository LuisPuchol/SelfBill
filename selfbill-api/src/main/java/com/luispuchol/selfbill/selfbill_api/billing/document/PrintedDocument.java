package com.luispuchol.selfbill.selfbill_api.billing.document;

import java.util.List;

/**
 * Everything a delivery note or an invoice prints, already as display strings. The template only
 * lays it out.
 *
 * @param issuer null when there is no issuer block to print
 * @param logo a data: URI, or null
 * @param client the lines of the client box
 * @param totals null on a plain delivery note
 * @param plainTotal the loose total of a plain delivery note, or null
 * @param nonFiscal true for a valued delivery note, which carries the "not an invoice" legend
 */
public record PrintedDocument(String title, String date, Issuer issuer, String logo, List<String> client,
        List<Section> sections, Totals totals, String plainTotal, boolean nonFiscal) {

    public record Issuer(String fiscalName, String nif, String address) {
    }

    /** A delivery note's block of lines inside an invoice; the heading is null on a delivery note. */
    public record Section(String heading, List<Line> lines) {
    }

    public record Line(String code, String concept, String traceability, String quantity, String price,
            String amount) {
    }

    /** The surcharge fields are null when the document has no surcharge. */
    public record Totals(String base, String vatPercentage, String vatAmount, String surchargePercentage,
            String surchargeAmount, String total) {
    }
}
