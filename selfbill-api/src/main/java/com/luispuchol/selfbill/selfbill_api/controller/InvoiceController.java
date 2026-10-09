package com.luispuchol.selfbill.selfbill_api.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceFilter;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceRequest;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.service.InvoiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Invoice management endpoints")
@Validated
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "Get all invoices", description = "Returns paginated and filtered list of non-deleted invoices")
    @GetMapping
    public Page<InvoiceResponse> getAllInvoices(@ParameterObject InvoiceFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return invoiceService.getAllInvoices(filter, pageable);
    }

    @GetMapping("/{id}")
    public InvoiceResponse getInvoiceById(@PathVariable @Positive Integer id) {
        return invoiceService.getInvoiceById(id);
    }

    @GetMapping("/by-code/{code}")
    public InvoiceResponse getInvoiceByCode(@PathVariable @Positive Integer code) {
        return invoiceService.getInvoiceByCode(code);
    }

    @Operation(summary = "Create a draft invoice")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@RequestBody @Valid InvoiceRequest request) {
        return invoiceService.createInvoice(request);
    }

    @Operation(summary = "Change the delivery notes of a draft invoice")
    @PutMapping("/{id}")
    public InvoiceResponse updateInvoice(@PathVariable @Positive Integer id,
            @RequestBody @Valid InvoiceRequest request) {
        return invoiceService.updateInvoice(id, request);
    }

    @Operation(summary = "Issue a draft invoice", description = "Assigns the next invoice number and the date, and freezes its content")
    @PostMapping("/{id}/issue")
    public InvoiceResponse issueInvoice(@PathVariable @Positive Integer id) {
        return invoiceService.issueInvoice(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoice(@PathVariable @Positive Integer id) {
        invoiceService.deleteInvoice(id);
    }

    @Operation(summary = "Send an issued invoice by email")
    @Parameter(name = "Accept-Language", in = ParameterIn.HEADER, description = "Language (es, en)", example = "en")
    @PostMapping("/{id}/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendInvoiceEmail(@PathVariable @Positive Integer id) {
        invoiceService.sendInvoiceEmail(id);
    }

    @Operation(summary = "Export an issued invoice as PDF")
    @Parameter(name = "Accept-Language", in = ParameterIn.HEADER, description = "Language (es, en)", example = "en")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportInvoicePdf(@PathVariable @Positive Integer id) {
        return PdfResponses.attachment(invoiceService.exportInvoicePdf(id));
    }
}
