package com.luispuchol.selfbill.selfbill_api.mapper;

import org.springframework.stereotype.Component;

import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.enums.InvoiceMode;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;

@Component
public class ClientMapper {

    public ClientResponse toResponse(Client client) {
        if (client == null) {
            return null;
        }
        return ClientResponse.builder()
                .id(client.getId())
                .code(client.getCode())
                .businessName(client.getBusinessName())
                .name(client.getName())
                .nif(client.getNif())
                .email(client.getEmail())
                .address(client.getAddress())
                .city(client.getCity())
                .province(client.getProvince())
                .postalCode(client.getPostalCode())
                .phone(client.getPhone())
                .phone2(client.getPhone2())
                .vatType(client.getVatType().name())
                .surchargeType(client.getSurchargeType().name())
                .invoiceMode(client.getInvoiceMode().name())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }

    public Client toEntity(ClientRequest request) {
        if (request == null) {
            return null;
        }
        return Client.builder()
                .code(request.getCode())
                .name(request.getName())
                .businessName(request.getBusinessName())
                .nif(request.getNif())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .province(request.getProvince())
                .postalCode(request.getPostalCode())
                .phone(request.getPhone())
                .phone2(request.getPhone2())
                .vatType(VatType.valueOf(request.getVatType()))
                .surchargeType(SurchargeType.valueOf(request.getSurchargeType()))
                .invoiceMode(InvoiceMode.valueOf(request.getInvoiceMode()))
                .build();
    }

    public void updateEntity(Client client, ClientRequest request) {
        if (client == null || request == null) {
            return;
        }
        client.setCode(request.getCode());
        client.setBusinessName(request.getBusinessName());
        client.setName(request.getName());
        client.setNif(request.getNif());
        client.setEmail(request.getEmail());
        client.setAddress(request.getAddress());
        client.setCity(request.getCity());
        client.setProvince(request.getProvince());
        client.setPostalCode(request.getPostalCode());
        client.setPhone(request.getPhone());
        client.setPhone2(request.getPhone2());
        client.setVatType(VatType.valueOf(request.getVatType()));
        client.setSurchargeType(SurchargeType.valueOf(request.getSurchargeType()));
        client.setInvoiceMode(InvoiceMode.valueOf(request.getInvoiceMode()));
    }
}
