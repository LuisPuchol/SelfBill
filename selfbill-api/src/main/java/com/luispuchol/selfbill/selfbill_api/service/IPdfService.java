package com.luispuchol.selfbill.selfbill_api.service;

import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.entity.Invoice;

public interface IPdfService {

    byte[] generateInvoicePdf(Invoice invoice);

    byte[] generateDeliveryNotePdf(DeliveryNote deliveryNote);

    public byte[] generateDeliveryNoteWithTaxesPdf(DeliveryNote deliveryNote);
}
