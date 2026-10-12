package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tax-config")
@Tag(name = "Tax Config", description = "Tax configuration management")
@RequiredArgsConstructor
public class TaxConfigController {

    private final TaxConfigService taxConfigService;

    @Operation(summary = "Get current tax configuration")
    @GetMapping
    public TaxConfigResponse getTaxConfig() {
        return taxConfigService.getTaxConfig();
    }

    @Operation(summary = "Create tax configuration (only one allowed)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxConfigResponse createTaxConfig(@RequestBody @Valid TaxConfigRequest request) {
        return taxConfigService.createTaxConfig(request);
    }

    @Operation(summary = "Update tax configuration")
    @PutMapping
    public TaxConfigResponse updateTaxConfig(@RequestBody @Valid TaxConfigRequest request) {
        return taxConfigService.updateTaxConfig(request);
    }
}
