package com.luispuchol.selfbill.selfbill_api.billing.invoice;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface InvoiceMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "sections", ignore = true)
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "articleId", source = "article.id")
    InvoiceLineResponse toLineResponse(InvoiceLine line);
}
