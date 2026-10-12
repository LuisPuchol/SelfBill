package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.TAX_CONFIG_ALREADY_EXISTS;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.TAX_CONFIG_NOT_FOUND;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.luispuchol.selfbill.selfbill_api.shared.ApiErrors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tax-config")
@Tag(name = "Tax Config", description = "Global VAT and equivalence surcharge rates, shared by every article")
@RequiredArgsConstructor
public class TaxConfigController {

    private final TaxConfigService taxConfigService;

    @Operation(summary = "Get current tax configuration")
    @ApiErrors(TAX_CONFIG_NOT_FOUND)
    @GetMapping
    public TaxConfigResponse getTaxConfig() {
        return taxConfigService.getTaxConfig();
    }

    @Operation(summary = "Create tax configuration (only one allowed)")
    @ApiErrors(TAX_CONFIG_ALREADY_EXISTS)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxConfigResponse createTaxConfig(@RequestBody @Valid TaxConfigRequest request) {
        return taxConfigService.createTaxConfig(request);
    }

    @Operation(summary = "Update tax configuration", description = """
            The new rates apply to delivery notes from now on. Existing delivery notes and invoices keep \
            the rates they were valued with.""")
    @ApiErrors(TAX_CONFIG_NOT_FOUND)
    @PutMapping
    public TaxConfigResponse updateTaxConfig(@RequestBody @Valid TaxConfigRequest request) {
        return taxConfigService.updateTaxConfig(request);
    }
}
