package com.luispuchol.selfbill.selfbill_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceFilter;
import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceRequest;
import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.mapper.InvoiceMapper;
import com.luispuchol.selfbill.selfbill_api.repository.DeliveryNoteRepository;
import com.luispuchol.selfbill.selfbill_api.repository.InvoiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceService implements IInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final DeliveryNoteRepository deliveryNoteRepository;
    private final InvoiceMapper invoiceMapper;

    @Transactional(readOnly = true)
    @Override
    public Page<InvoiceResponse> getAllInvoices(InvoiceFilter filter, Pageable pageable) {
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public InvoiceResponse getInvoiceById(Integer id) {
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public InvoiceResponse getInvoiceByCode(Integer code) {
        return null;
    }

    @Transactional
    @Override
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        // TODO:
        // 1. Find each DeliveryNote in request.deliveryNoteIds (throw DELIVERY_NOTE_NOT_FOUND if any missing)
        // 2. Verify all delivery notes belong to the same client (otherwise reject)
        // 3. Verify none of the delivery notes already have an invoice (throw INVOICE_DUPLICATE_DELIVERY_NOTE)
        // 4. Build Invoice with client from the delivery notes:
        //    - subtotal = sum of deliveryNote.total for all delivery notes
        //    - vatPercentage = client.vatType == WITH_VAT ? 21 : 0
        //    - vatAmount = subtotal * vatPercentage / 100
        //    - surchargePercentage = client.surchargeType == WITH_SURCHARGE ? 5.2 : 0
        //    - surchargeAmount = subtotal * surchargePercentage / 100
        //    - total = subtotal + vatAmount + surchargeAmount
        // 5. For each delivery note, set deliveryNote.invoice = invoice
        // 6. Create InvoiceLine snapshots from each DeliveryNoteArticles (with deliveryNote reference for grouping)
        // 7. Save and return invoiceMapper.toResponse(saved)
        return null;
    }

    @Transactional
    @Override
    public InvoiceResponse updateInvoice(Integer id, InvoiceRequest request) {
        return null;
    }

    @Transactional
    @Override
    public void deleteInvoice(Integer id) {
        // TODO: find invoice, soft-delete it, unlink delivery notes (set invoice = null)
    }
}
