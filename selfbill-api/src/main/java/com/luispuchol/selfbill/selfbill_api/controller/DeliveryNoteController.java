package com.luispuchol.selfbill.selfbill_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.service.IDeliveryNoteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/deliveryNotes")
@Tag(name = "Delivery Notes", description = "Delivery note management endpoints")
@Validated
@RequiredArgsConstructor
public class DeliveryNoteController {

    private final IDeliveryNoteService deliveryNoteService;

    @Operation(summary = "Get all delivery notes", description = "Returns list of all non-deleted delivery notes")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping
    public ResponseEntity<List<DeliveryNoteResponse>> getAllDeliveryNotes() {
        return ResponseEntity.ok(deliveryNoteService.getAllDeliveryNotes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryNoteResponse> getDeliveryNoteById(
            @PathVariable @NotNull @Positive Integer id) {
        return ResponseEntity.ok(deliveryNoteService.getDeliveryNoteById(id));
    }

    @PostMapping
    public ResponseEntity<DeliveryNoteResponse> createDeliveryNote(
            @RequestBody @Valid DeliveryNoteRequest deliveryNoteRequest) {
        DeliveryNoteResponse created = deliveryNoteService.createDeliveryNote(deliveryNoteRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryNoteResponse> updateDeliveryNote(
            @PathVariable @NotNull @Positive Integer id,
            @RequestBody @Valid DeliveryNoteRequest deliveryNoteRequest) {
        DeliveryNoteResponse updated = deliveryNoteService.updateDeliveryNote(id, deliveryNoteRequest);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeliveryNote(
            @PathVariable @NotNull @Positive Integer id) {
        deliveryNoteService.deleteDeliveryNote(id);
        return ResponseEntity.noContent().build();
    }
}
