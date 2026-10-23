package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.CLIENT_EMAIL_NOT_CONFIGURED;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.CLIENT_FISCAL_DATA_MISSING;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.CLIENT_NOT_FOUND;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.DELIVERY_NOTE_NOT_FOUND;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.EMAIL_SEND_FAILED;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.INVOICE_ALREADY_ISSUED;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.INVOICE_MIXED_CLIENTS;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.INVOICE_MIXED_MONTHS;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.INVOICE_MIXED_TAX_RATES;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.INVOICE_NOT_FOUND;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.INVOICE_NOT_ISSUED;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.MAIL_NOT_CONFIGURED;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.USER_PROFILE_NOT_FOUND;

import java.time.LocalDate;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.luispuchol.selfbill.selfbill_api.billing.document.PdfResponses;
import com.luispuchol.selfbill.selfbill_api.shared.ApiErrors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = """
        An invoice groups delivery notes of one client, in one section per delivery note. It is created as `DRAFT`, \
        where only its set of delivery notes can change, and gets its number when it is issued.""")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "Get all invoices", description = """
            Returns paginated and filtered list of non-deleted invoices, without their sections: \
            get an invoice by id to read them.""")
    @GetMapping
    public Page<InvoiceResponse> getAllInvoices(@ParameterObject InvoiceFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return invoiceService.getAllInvoices(filter, pageable);
    }

    @Operation(summary = "Get the invoices that are pending to issue", description = """
            Groups the delivery notes that are in no invoice, up to the given day inclusive, into one row per \
            client, calendar month and VAT and surcharge rates: each row is an invoice that can be created and \
            issued. Delivery notes in a draft invoice are not pending. Amounts are the ones issuing the row would \
            give. Ordered by client code, month and rates.""")
    @GetMapping("/pending")
    public List<PendingInvoiceResponse> getPendingInvoices(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Schema(example = "2026-10-31") LocalDate dateTo) {
        return invoiceService.getPendingInvoices(dateTo);
    }

    @Operation(summary = "Get an invoice by id")
    @ApiErrors(INVOICE_NOT_FOUND)
    @GetMapping("/{id}")
    public InvoiceResponse getInvoiceById(@PathVariable @Positive Integer id) {
        return invoiceService.getInvoiceById(id);
    }

    @Operation(summary = "Get an invoice by its number within a series")
    @ApiErrors(INVOICE_NOT_FOUND)
    @GetMapping("/by-code/{code}")
    public InvoiceResponse getInvoiceByCode(@PathVariable @Positive Integer code,
            @RequestParam(defaultValue = "GENERAL") InvoiceSeries series) {
        return invoiceService.getInvoiceByCode(series, code);
    }

    @Operation(summary = "Create a draft invoice", description = """
            Groups delivery notes of the client that are not in another invoice. All of them must be from the same \
            month and have the same VAT and surcharge rates. Sections are ordered by delivery note date, oldest \
            first. The invoice gets no number until it is issued.""")
    @ApiErrors({ CLIENT_NOT_FOUND, DELIVERY_NOTE_NOT_FOUND, INVOICE_MIXED_CLIENTS,
            INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED, INVOICE_MIXED_MONTHS, INVOICE_MIXED_TAX_RATES })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@RequestBody @Valid InvoiceRequest request) {
        return invoiceService.createInvoice(request);
    }

    @Operation(summary = "Create and issue an invoice in one step", description = """
            Does what creating a draft and issuing it do, in a single transaction: if anything fails, no invoice \
            is created and the delivery notes stay free.""")
    @ApiErrors({ CLIENT_NOT_FOUND, DELIVERY_NOTE_NOT_FOUND, INVOICE_MIXED_CLIENTS,
            INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED, INVOICE_MIXED_MONTHS, INVOICE_MIXED_TAX_RATES,
            CLIENT_FISCAL_DATA_MISSING, USER_PROFILE_NOT_FOUND })
    @PostMapping("/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createAndIssue(@RequestBody @Valid InvoiceRequest request) {
        return invoiceService.createAndIssue(request);
    }

    @Operation(summary = "Change the delivery notes of a draft invoice", description = """
            Replaces the set of delivery notes and recalculates the invoice. \
            Delivery notes left out become free again.""")
    @ApiErrors({ INVOICE_NOT_FOUND, INVOICE_ALREADY_ISSUED, CLIENT_NOT_FOUND, DELIVERY_NOTE_NOT_FOUND,
            INVOICE_MIXED_CLIENTS, INVOICE_DELIVERY_NOTE_ALREADY_ASSOCIATED, INVOICE_MIXED_MONTHS,
            INVOICE_MIXED_TAX_RATES })
    @PutMapping("/{id}")
    public InvoiceResponse updateInvoice(@PathVariable @Positive Integer id,
            @RequestBody @Valid InvoiceRequest request) {
        return invoiceService.updateInvoice(id, request);
    }

    @Operation(summary = "Issue a draft invoice", description = """
            Recalculates the invoice from its delivery notes, assigns the next number of its series and today's \
            date, and freezes its content and the client's fiscal data. Its delivery notes can no longer be edited. \
            Requires a live client with NIF, address, postal code and city, and the user profile.""")
    @ApiErrors({ INVOICE_NOT_FOUND, INVOICE_ALREADY_ISSUED, CLIENT_NOT_FOUND, CLIENT_FISCAL_DATA_MISSING,
            USER_PROFILE_NOT_FOUND, INVOICE_MIXED_MONTHS, INVOICE_MIXED_TAX_RATES })
    @PostMapping("/{id}/issue")
    public InvoiceResponse issueInvoice(@PathVariable @Positive Integer id) {
        return invoiceService.issueInvoice(id);
    }

    @Operation(summary = "Delete an invoice", description = """
            Allowed in any status, as the way to correct an issued invoice. Its delivery notes become free again. \
            An issued invoice keeps its number, which is never reused.""")
    @ApiErrors(INVOICE_NOT_FOUND)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoice(@PathVariable @Positive Integer id) {
        invoiceService.deleteInvoice(id);
    }

    @Operation(summary = "Send an issued invoice by email", description = "Sends the PDF to the client's email address")
    @Parameter(name = "Accept-Language", in = ParameterIn.HEADER, description = "Language (es, en)", example = "en")
    @ApiErrors({ INVOICE_NOT_FOUND, INVOICE_NOT_ISSUED, CLIENT_EMAIL_NOT_CONFIGURED, MAIL_NOT_CONFIGURED,
            EMAIL_SEND_FAILED })
    @PostMapping("/{id}/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendInvoiceEmail(@PathVariable @Positive Integer id) {
        invoiceService.sendInvoiceEmail(id);
    }

    @Operation(summary = "Export an issued invoice as PDF")
    @Parameter(name = "Accept-Language", in = ParameterIn.HEADER, description = "Language (es, en)", example = "en")
    @ApiResponse(responseCode = "200", description = "The PDF, as an attachment",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary")))
    @ApiErrors({ INVOICE_NOT_FOUND, INVOICE_NOT_ISSUED })
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportInvoicePdf(@PathVariable @Positive Integer id) {
        return PdfResponses.attachment(invoiceService.exportInvoicePdf(id));
    }
}
