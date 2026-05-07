package com.luispuchol.selfbill.selfbill_api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.enums.InvoiceMode;
import com.luispuchol.selfbill.selfbill_api.enums.SurchargeType;
import com.luispuchol.selfbill.selfbill_api.enums.VatType;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    imports = {VatType.class, SurchargeType.class, InvoiceMode.class}
)
public interface ClientMapper {

    ClientResponse toResponse(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "vatType", expression = "java(VatType.valueOf(request.getVatType()))")
    @Mapping(target = "surchargeType", expression = "java(SurchargeType.valueOf(request.getSurchargeType()))")
    @Mapping(target = "invoiceMode", expression = "java(InvoiceMode.valueOf(request.getInvoiceMode()))")
    Client toEntity(ClientRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "vatType", expression = "java(VatType.valueOf(request.getVatType()))")
    @Mapping(target = "surchargeType", expression = "java(SurchargeType.valueOf(request.getSurchargeType()))")
    @Mapping(target = "invoiceMode", expression = "java(InvoiceMode.valueOf(request.getInvoiceMode()))")
    void updateEntity(@MappingTarget Client client, ClientRequest request);
}
