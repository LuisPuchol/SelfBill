package com.luispuchol.selfbill.selfbill_api.mapper;

import org.springframework.stereotype.Component;

import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Client;

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
                .customerType(client.getCustomerType())
                .equivalenceSurcharge(client.getEquivalenceSurcharge())
                .invoicePerDeliveryNote(client.getInvoicePerDeliveryNote())
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
                .nif(request.getNif())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .province(request.getProvince())
                .postalCode(request.getPostalCode())
                .phone(request.getPhone())
                .phone2(request.getPhone2())
                .customerType(request.getCustomerType())
                .equivalenceSurcharge(request.getEquivalenceSurcharge())
                .invoicePerDeliveryNote(request.getInvoicePerDeliveryNote())
                .build();
    }

    public void updateEntity(Client client, ClientRequest request) {
        if (client != null && request != null) {
            client.setCode(request.getCode());
            client.setName(request.getName());
            client.setNif(request.getNif());
            client.setEmail(request.getEmail());
            client.setAddress(request.getAddress());
            client.setCity(request.getCity());
            client.setProvince(request.getProvince());
            client.setPostalCode(request.getPostalCode());
            client.setPhone(request.getPhone());
            client.setPhone2(request.getPhone2());
            client.setCustomerType(request.getCustomerType());
            client.setEquivalenceSurcharge(request.getEquivalenceSurcharge());
            client.setInvoicePerDeliveryNote(request.getInvoicePerDeliveryNote());
        }
    }
}
