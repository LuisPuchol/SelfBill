package com.luispuchol.selfbill.selfbill_api.catalog.client;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ClientMapper {

    ClientResponse toResponse(Client client);

    @Mapping(target = "searchText", ignore = true)
    Client toEntity(ClientRequest request);

    @Mapping(target = "searchText", ignore = true)
    void updateEntity(@MappingTarget Client client, ClientRequest request);
}
