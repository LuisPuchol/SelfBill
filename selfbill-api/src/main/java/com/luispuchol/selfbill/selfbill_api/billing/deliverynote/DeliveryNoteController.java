package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/delivery-notes")
@Tag(name = "Delivery Notes", description = "Delivery note management endpoints")
@RequiredArgsConstructor
public class DeliveryNoteController {

    private final DeliveryNoteService deliveryNoteService;

    @Operation(summary = "Get all delivery notes", description = "Returns paginated and filtered list of non-deleted delivery notes")
    @GetMapping
    public Page<DeliveryNoteResponse> getAllDeliveryNotes(@ParameterObject DeliveryNoteFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return deliveryNoteService.getAllDeliveryNotes(filter, pageable);
    }

    @GetMapping("/{id}")
    public DeliveryNoteResponse getDeliveryNoteById(@PathVariable @Positive Integer id) {
        return deliveryNoteService.getDeliveryNoteById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryNoteResponse createDeliveryNote(@RequestBody @Valid DeliveryNoteRequest request) {
        return deliveryNoteService.createDeliveryNote(request);
    }

    @PutMapping("/{id}")
    public DeliveryNoteResponse updateDeliveryNote(@PathVariable @Positive Integer id,
            @RequestBody @Valid DeliveryNoteRequest request) {
        return deliveryNoteService.updateDeliveryNote(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeliveryNote(@PathVariable @Positive Integer id) {
        deliveryNoteService.deleteDeliveryNote(id);
    }

    @Operation(summary = "Send delivery note by email")
    @Parameter(name = "Accept-Language", in = ParameterIn.HEADER, description = "Language (es, en)", example = "en")
    @PostMapping("/{id}/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendDeliveryNoteEmail(@PathVariable @Positive Integer id) {
        deliveryNoteService.sendDeliveryNoteEmail(id);
    }

    @Operation(summary = "Export delivery note as PDF")
    @Parameter(name = "Accept-Language", in = ParameterIn.HEADER, description = "Language (es, en)", example = "en")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportDeliveryNotePdf(@PathVariable @Positive Integer id) {
        return PdfResponses.attachment(deliveryNoteService.exportDeliveryNotePdf(id));
    }
}
