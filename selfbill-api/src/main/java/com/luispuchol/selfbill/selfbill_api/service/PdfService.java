package com.luispuchol.selfbill.selfbill_api.service;

import org.springframework.stereotype.Service;

import com.luispuchol.selfbill.selfbill_api.entity.Invoice;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.repository.InvoiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfService implements IPdfService {

    private final InvoiceRepository invoiceRepository;

    @Override
    public byte[] generateInvoicePdf(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND, invoiceId));

        // TODO: generate PDF with invoice data (e.g. OpenPDF)
        throw new UnsupportedOperationException("PDF generation not yet implemented");
    }
}
