package com.luispuchol.selfbill.selfbill_api.billing.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.Invoice;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;

@Service
public class PdfService {

    public byte[] generateInvoicePdf(Invoice invoice) {
        return buildMockPdf("invoice", invoice.getNumber());
    }

    public byte[] generateDeliveryNotePdf(DeliveryNote deliveryNote) {
        String label = deliveryNote.getFormat() == DeliveryNoteFormat.VALUED
                ? "Delivery Note with taxes"
                : "delivery note";
        return buildMockPdf(label, String.valueOf(deliveryNote.getCode()));
    }

    private byte[] buildMockPdf(String label, String number) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);

            writeAscii(out, "%PDF-1.4\n");

            offsets.add(out.size());
            writeAscii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            offsets.add(out.size());
            writeAscii(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            offsets.add(out.size());
            writeAscii(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n");

            offsets.add(out.size());
            writeAscii(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            String text = "BT /F1 18 Tf 50 780 Td (Mock " + label + " #" + number + ") Tj ET";
            offsets.add(out.size());
            writeAscii(out, "5 0 obj\n<< /Length " + text.length() + " >>\nstream\n" + text + "\nendstream\nendobj\n");

            int xrefStart = out.size();
            writeAscii(out, "xref\n0 6\n0000000000 65535 f \n");
            for (int i = 1; i < offsets.size(); i++) {
                writeAscii(out, String.format("%010d 00000 n %n", offsets.get(i)));
            }
            writeAscii(out, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xrefStart + "\n%%EOF");

            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build mock PDF", e);
        }
    }

    private static void writeAscii(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.ISO_8859_1));
    }
}
