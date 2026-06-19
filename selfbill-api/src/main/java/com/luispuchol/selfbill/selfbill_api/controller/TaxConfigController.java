package com.luispuchol.selfbill.selfbill_api.controller;

import com.luispuchol.selfbill.selfbill_api.dto.taxConfigDTO.TaxConfigRequest;
import com.luispuchol.selfbill.selfbill_api.dto.taxConfigDTO.TaxConfigResponse;
import com.luispuchol.selfbill.selfbill_api.service.ITaxConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tax-config")
@Tag(name = "Tax Config", description = "Tax configuration management")
@Validated
@RequiredArgsConstructor
public class TaxConfigController {

    private final ITaxConfigService taxConfigService;

    @Operation(summary = "Get current tax configuration")
    @GetMapping
    public ResponseEntity<TaxConfigResponse> getTaxConfig() {
        return ResponseEntity.ok(taxConfigService.getTaxConfig());
    }

    @Operation(summary = "Create tax configuration (only one allowed)")
    @PostMapping
    public ResponseEntity<TaxConfigResponse> createTaxConfig(
            @RequestBody @Valid TaxConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxConfigService.createTaxConfig(request));
    }

    @Operation(summary = "Update tax configuration")
    @PutMapping
    public ResponseEntity<TaxConfigResponse> updateTaxConfig(
            @RequestBody @Valid TaxConfigRequest request) {
        return ResponseEntity.ok(taxConfigService.updateTaxConfig(request));
    }
}
