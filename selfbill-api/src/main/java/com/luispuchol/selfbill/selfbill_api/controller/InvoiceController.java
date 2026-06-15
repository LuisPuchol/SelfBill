package com.luispuchol.selfbill.selfbill_api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceFilter;
import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceRequest;
import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.service.IInvoiceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Invoice management endpoints")
@Validated
@RequiredArgsConstructor
public class InvoiceController {

    private final IInvoiceService invoiceService;

    @Operation(summary = "Get all invoices", description = "Returns paginated and filtered list of non-deleted invoices")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getAllInvoices(
            @ParameterObject @ModelAttribute InvoiceFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getAllInvoices(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @PathVariable @NotNull @Positive Integer id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<InvoiceResponse> getInvoiceByCode(
            @PathVariable @NotNull @Positive Integer code) {
        return ResponseEntity.ok(invoiceService.getInvoiceByCode(code));
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(
            @RequestBody @Valid InvoiceRequest invoiceRequest) {
        InvoiceResponse created = invoiceService.createInvoice(invoiceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable @NotNull @Positive Integer id,
            @RequestBody @Valid InvoiceRequest invoiceRequest) {
        InvoiceResponse updated = invoiceService.updateInvoice(id, invoiceRequest);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(
            @PathVariable @NotNull @Positive Integer id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
