package com.luispuchol.selfbill.selfbill_api.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.luispuchol.selfbill.selfbill_api.dto.client.ClientFilter;
import com.luispuchol.selfbill.selfbill_api.dto.client.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.client.ClientResponse;
import com.luispuchol.selfbill.selfbill_api.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Client management endpoints")
@Validated
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @Operation(summary = "Get all clients", description = "Returns paginated and filtered list of non-deleted clients")
    @GetMapping
    public Page<ClientResponse> getAllClients(@ParameterObject ClientFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return clientService.getAllClients(filter, pageable);
    }

    @GetMapping("/{id}")
    public ClientResponse getClientById(@PathVariable @Positive Integer id) {
        return clientService.getClientById(id);
    }

    @GetMapping("/by-code/{code}")
    public ClientResponse getClientByCode(@PathVariable @Positive Integer code) {
        return clientService.getClientByCode(code);
    }

    @GetMapping("/by-name/{name}")
    public ClientResponse getClientByName(@PathVariable @NotBlank String name) {
        return clientService.getClientByName(name);
    }

    @GetMapping("/by-nif/{nif}")
    public ClientResponse getClientByNif(@PathVariable @NotBlank String nif) {
        return clientService.getClientByNif(nif);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse createClient(@RequestBody @Valid ClientRequest request) {
        return clientService.createClient(request);
    }

    @PutMapping("/{id}")
    public ClientResponse updateClient(@PathVariable @Positive Integer id,
            @RequestBody @Valid ClientRequest request) {
        return clientService.updateClient(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClient(@PathVariable @Positive Integer id) {
        clientService.deleteClient(id);
    }
}
