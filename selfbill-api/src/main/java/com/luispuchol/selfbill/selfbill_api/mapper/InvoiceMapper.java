package com.luispuchol.selfbill.selfbill_api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceLineResponse;
import com.luispuchol.selfbill.selfbill_api.dto.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Invoice;
import com.luispuchol.selfbill.selfbill_api.entity.InvoiceLine;

@Mapper
public interface InvoiceMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "sections", ignore = true)
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "articleId", source = "article.id")
    InvoiceLineResponse toLineResponse(InvoiceLine line);
}
