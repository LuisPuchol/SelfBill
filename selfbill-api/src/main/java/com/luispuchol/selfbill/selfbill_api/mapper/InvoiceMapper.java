package com.luispuchol.selfbill.selfbill_api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceLineResponse;
import com.luispuchol.selfbill.selfbill_api.dto.invoiceDTO.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Invoice;
import com.luispuchol.selfbill.selfbill_api.entity.InvoiceLine;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface InvoiceMapper {

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    @Mapping(target = "sections", ignore = true)
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(source = "article.id", target = "articleId")
    InvoiceLineResponse toLineResponse(InvoiceLine invoiceLine);
}
