package com.luispuchol.selfbill.selfbill_api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientFilter;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientResponse;
import com.luispuchol.selfbill.selfbill_api.service.IClientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Client management endpoints")
@Validated
@RequiredArgsConstructor
public class ClientController {

    private final IClientService clientService;

    @Operation(summary = "Get all clients", description = "Returns paginated and filtered list of non-deleted clients")
    @ApiResponse(responseCode = "200", description = "Successful operation")
    @GetMapping
    public ResponseEntity<Page<ClientResponse>> getAllClients(
            @ParameterObject @ModelAttribute ClientFilter filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(clientService.getAllClients(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(
            @PathVariable @NotNull @Positive Integer id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<ClientResponse> getClientByCode(
            @PathVariable @NotNull @Positive Integer code) {
        return ResponseEntity.ok(clientService.getClientByCode(code));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<ClientResponse> getClientByName(
            @PathVariable @NotBlank String name) {
        return ResponseEntity.ok(clientService.getClientByName(name));
    }

    @GetMapping("/by-nif/{nif}")
    public ResponseEntity<ClientResponse> getClientByNif(
            @PathVariable @NotBlank String nif) {
        return ResponseEntity.ok(clientService.getClientByNif(nif));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(
            @RequestBody @Valid ClientRequest clientRequest) {
        ClientResponse created = clientService.createClient(clientRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable @NotNull @Positive Integer id,
            @RequestBody @Valid ClientRequest clientRequest) {
        ClientResponse updated = clientService.updateClient(id, clientRequest);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(
            @PathVariable @NotNull @Positive Integer id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
