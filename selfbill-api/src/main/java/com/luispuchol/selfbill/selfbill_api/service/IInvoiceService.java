package com.luispuchol.selfbill.selfbill_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceFilter;
import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceRequest;
import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceResponse;

public interface IInvoiceService {

    Page<InvoiceResponse> getAllInvoices(InvoiceFilter filter, Pageable pageable);

    InvoiceResponse getInvoiceById(Integer id);

    InvoiceResponse getInvoiceByCode(Integer code);

    InvoiceResponse createInvoice(InvoiceRequest request);

    InvoiceResponse updateInvoice(Integer id, InvoiceRequest request);

    void deleteInvoice(Integer id);
}
