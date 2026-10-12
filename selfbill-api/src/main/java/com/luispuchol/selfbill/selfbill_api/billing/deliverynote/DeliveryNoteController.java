package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.ARTICLE_NOT_FOUND;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.CLIENT_EMAIL_NOT_CONFIGURED;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.CLIENT_NOT_FOUND;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.DELIVERY_NOTE_ALREADY_INVOICED;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.DELIVERY_NOTE_DUPLICATE_CODE;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.DELIVERY_NOTE_NOT_FOUND;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.EMAIL_SEND_FAILED;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.INVOICE_MIXED_CLIENTS;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.MAIL_NOT_CONFIGURED;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.TAX_CONFIG_NOT_FOUND;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/delivery-notes")
@Tag(name = "Delivery Notes", description = """
        One delivery note per delivery to a client. It can be edited until the invoice that contains it is issued, \
        and follows its client's current tax regime until then.""")
@RequiredArgsConstructor
public class DeliveryNoteController {

    private final DeliveryNoteService deliveryNoteService;

    @Operation(summary = "Get all delivery notes", description = "Returns paginated and filtered list of non-deleted delivery notes")
    @GetMapping
    public Page<DeliveryNoteResponse> getAllDeliveryNotes(@ParameterObject @Valid DeliveryNoteFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return deliveryNoteService.getAllDeliveryNotes(filter, pageable);
    }

    @Operation(summary = "Get a delivery note by id")
    @ApiErrors(DELIVERY_NOTE_NOT_FOUND)
    @GetMapping("/{id}")
    public DeliveryNoteResponse getDeliveryNoteById(@PathVariable @Positive Integer id) {
        return deliveryNoteService.getDeliveryNoteById(id);
    }

    @Operation(summary = "Create a delivery note", description = """
            Takes the print format from the client's default, and the VAT and surcharge rates from the client's \
            regime and the tax configuration in force on its date.""")
    @ApiErrors({ DELIVERY_NOTE_DUPLICATE_CODE, CLIENT_NOT_FOUND, ARTICLE_NOT_FOUND, TAX_CONFIG_NOT_FOUND })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryNoteResponse createDeliveryNote(@RequestBody @Valid DeliveryNoteRequest request) {
        return deliveryNoteService.createDeliveryNote(request);
    }

    @Operation(summary = "Update a delivery note", description = """
            Replaces its data and all its lines. Not allowed once its invoice is issued.
            Inside a draft invoice the client cannot change, and the invoice is recalculated.
            Changing the client takes the new client's print format and tax regime.""")
    @ApiErrors({ DELIVERY_NOTE_NOT_FOUND, DELIVERY_NOTE_ALREADY_INVOICED, INVOICE_MIXED_CLIENTS,
            DELIVERY_NOTE_DUPLICATE_CODE, CLIENT_NOT_FOUND, ARTICLE_NOT_FOUND, TAX_CONFIG_NOT_FOUND })
    @PutMapping("/{id}")
    public DeliveryNoteResponse updateDeliveryNote(@PathVariable @Positive Integer id,
            @RequestBody @Valid DeliveryNoteRequest request) {
        return deliveryNoteService.updateDeliveryNote(id, request);
    }

    @Operation(summary = "Delete a delivery note", description = "Only while it is not in any invoice, draft or issued")
    @ApiErrors({ DELIVERY_NOTE_NOT_FOUND, DELIVERY_NOTE_ALREADY_INVOICED })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeliveryNote(@PathVariable @Positive Integer id) {
        deliveryNoteService.deleteDeliveryNote(id);
    }

    @Operation(summary = "Send delivery note by email", description = "Sends the PDF to the client's email address")
    @Parameter(name = "Accept-Language", in = ParameterIn.HEADER, description = "Language (es, en)", example = "en")
    @ApiErrors({ DELIVERY_NOTE_NOT_FOUND, CLIENT_EMAIL_NOT_CONFIGURED, MAIL_NOT_CONFIGURED, EMAIL_SEND_FAILED })
    @PostMapping("/{id}/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendDeliveryNoteEmail(@PathVariable @Positive Integer id) {
        deliveryNoteService.sendDeliveryNoteEmail(id);
    }

    @Operation(summary = "Export delivery note as PDF", description = "Printed in the delivery note's own format")
    @Parameter(name = "Accept-Language", in = ParameterIn.HEADER, description = "Language (es, en)", example = "en")
    @ApiResponse(responseCode = "200", description = "The PDF, as an attachment",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary")))
    @ApiErrors(DELIVERY_NOTE_NOT_FOUND)
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportDeliveryNotePdf(@PathVariable @Positive Integer id) {
        return PdfResponses.attachment(deliveryNoteService.exportDeliveryNotePdf(id));
    }
}
