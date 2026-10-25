package com.luispuchol.selfbill.selfbill_api.billing.document;

import org.springframework.stereotype.Service;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.Invoice;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceStatus;
import com.luispuchol.selfbill.selfbill_api.settings.profile.IssuerData;
import com.luispuchol.selfbill.selfbill_api.settings.profile.UserProfileService;

import lombok.RequiredArgsConstructor;

/**
 * The printed invoice and delivery note. Reads the document's lines, articles and client lazily, so the
 * caller keeps a transaction open while it runs.
 */
@Service
@RequiredArgsConstructor
public class PdfService {

    private final PdfRenderer pdfRenderer;
    private final UserProfileService userProfileService;

    public byte[] generateInvoicePdf(Invoice invoice) {
        return pdfRenderer.render(PrintedDocuments.invoice(invoice, userProfileService.findLogo().orElse(null)));
    }

    public byte[] generateDeliveryNotePdf(DeliveryNote deliveryNote) {
        // A note in an issued invoice prints the invoice's copy of the issuer, so the profile is not read
        Invoice invoice = deliveryNote.getInvoice();
        IssuerData currentIssuer = invoice != null && invoice.getStatus() == InvoiceStatus.ISSUED
                ? null
                : userProfileService.findIssuer().orElse(null);
        return pdfRenderer.render(PrintedDocuments.deliveryNote(deliveryNote, currentIssuer,
                userProfileService.findLogo().orElse(null)));
    }
}
