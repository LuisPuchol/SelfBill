package com.luispuchol.selfbill.selfbill_api.catalog.client;

import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.CLIENT_DUPLICATE_CODE;
import static com.luispuchol.selfbill.selfbill_api.shared.ErrorCode.CLIENT_NOT_FOUND;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.luispuchol.selfbill.selfbill_api.catalog.NextCodeResponse;
import com.luispuchol.selfbill.selfbill_api.shared.ApiErrors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Client catalog, with the tax regime and print format applied to their delivery notes")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @Operation(summary = "Get all clients", description = "Returns paginated and filtered list of non-deleted clients")
    @GetMapping
    public Page<ClientResponse> getAllClients(@ParameterObject ClientFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return clientService.getAllClients(filter, pageable);
    }

    @Operation(summary = "Suggest the next free code", description = "Highest code among non-deleted clients plus one")
    @GetMapping("/next-code")
    public NextCodeResponse getNextCode() {
        return clientService.getNextCode();
    }

    @Operation(summary = "Get a client by id")
    @ApiErrors(CLIENT_NOT_FOUND)
    @GetMapping("/{id}")
    public ClientResponse getClientById(@PathVariable @Positive Integer id) {
        return clientService.getClientById(id);
    }

    @Operation(summary = "Get a client by code")
    @ApiErrors(CLIENT_NOT_FOUND)
    @GetMapping("/by-code/{code}")
    public ClientResponse getClientByCode(@PathVariable @Positive Integer code) {
        return clientService.getClientByCode(code);
    }

    @Operation(summary = "Get a client by name", description = "Exact match on `name`, ignoring case")
    @ApiErrors(CLIENT_NOT_FOUND)
    @GetMapping("/by-name/{name}")
    public ClientResponse getClientByName(@PathVariable @NotBlank String name) {
        return clientService.getClientByName(name);
    }

    @Operation(summary = "Get a client by NIF")
    @ApiErrors(CLIENT_NOT_FOUND)
    @GetMapping("/by-nif/{nif}")
    public ClientResponse getClientByNif(@PathVariable @NotBlank String nif) {
        return clientService.getClientByNif(nif);
    }

    @Operation(summary = "Create a client")
    @ApiErrors(CLIENT_DUPLICATE_CODE)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse createClient(@RequestBody @Valid ClientRequest request) {
        return clientService.createClient(request);
    }

    @Operation(summary = "Update a client", description = """
            Issued invoices keep the fiscal data they were issued with.
            Changing the VAT or surcharge type reapplies it to the client's delivery notes that are not in an \
            issued invoice, and recalculates the client's draft invoices.""")
    @ApiErrors({ CLIENT_NOT_FOUND, CLIENT_DUPLICATE_CODE })
    @PutMapping("/{id}")
    public ClientResponse updateClient(@PathVariable @Positive Integer id,
            @RequestBody @Valid ClientRequest request) {
        return clientService.updateClient(id, request);
    }

    @Operation(summary = "Delete a client", description = """
            Logical deletion: documents of the client keep showing it, and its code becomes free.
            Its pending delivery notes and draft invoices cannot be edited or invoiced until it is restored.""")
    @ApiErrors(CLIENT_NOT_FOUND)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClient(@PathVariable @Positive Integer id) {
        clientService.deleteClient(id);
    }
}
