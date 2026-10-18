package com.luispuchol.selfbill.selfbill_api.settings.taxconfig;

import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.TAX_CONFIG_ALREADY_EXISTS;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.TAX_CONFIG_NOT_FOUND;

import java.util.List;

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
@Tag(name = "Tax Config", description = """
        Global VAT and equivalence surcharge rates, shared by every article. Each configuration applies from its \
        date until the next one starts.""")
@RequiredArgsConstructor
public class TaxConfigController {

    private final TaxConfigService taxConfigService;

    @Operation(summary = "Get the tax configuration in force today")
    @ApiErrors(TAX_CONFIG_NOT_FOUND)
    @GetMapping
    public TaxConfigResponse getTaxConfig() {
        return taxConfigService.getTaxConfig();
    }

    @Operation(summary = "List every tax configuration, newest first")
    @GetMapping("/history")
    public List<TaxConfigResponse> getHistory() {
        return taxConfigService.getHistory();
    }

    @Operation(summary = "Add a tax configuration from a date", description = """
            Delivery notes take the rates in force on their own date. Existing delivery notes and invoices keep \
            the rates they were valued with.""")
    @ApiErrors(TAX_CONFIG_ALREADY_EXISTS)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxConfigResponse createTaxConfig(@RequestBody @Valid TaxConfigRequest request) {
        return taxConfigService.createTaxConfig(request);
    }

    @Operation(summary = "Correct the rates of the tax configuration that starts on a date",
            description = "Existing delivery notes and invoices keep the rates they were valued with.")
    @ApiErrors(TAX_CONFIG_NOT_FOUND)
    @PutMapping
    public TaxConfigResponse updateTaxConfig(@RequestBody @Valid TaxConfigRequest request) {
        return taxConfigService.updateTaxConfig(request);
    }
}
