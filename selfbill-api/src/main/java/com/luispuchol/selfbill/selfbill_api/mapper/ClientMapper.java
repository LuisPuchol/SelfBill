package com.luispuchol.selfbill.selfbill_api.mapper;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.luispuchol.selfbill.selfbill_api.dto.client.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.client.ClientResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Client;

@Mapper
public interface ClientMapper {

    ClientResponse toResponse(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Client toEntity(ClientRequest request);

    @InheritConfiguration
    void updateEntity(@MappingTarget Client client, ClientRequest request);
}
