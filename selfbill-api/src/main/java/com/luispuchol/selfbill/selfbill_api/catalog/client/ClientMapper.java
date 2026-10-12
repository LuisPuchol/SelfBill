package com.luispuchol.selfbill.selfbill_api.catalog.client;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface ClientMapper {

    ClientResponse toResponse(Client client);

    Client toEntity(ClientRequest request);

    void updateEntity(@MappingTarget Client client, ClientRequest request);
}
