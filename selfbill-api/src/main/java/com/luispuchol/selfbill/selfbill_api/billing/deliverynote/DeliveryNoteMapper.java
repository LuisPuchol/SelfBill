package com.luispuchol.selfbill.selfbill_api.billing.deliverynote;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.luispuchol.selfbill.selfbill_api.catalog.article.Article;
import com.luispuchol.selfbill.selfbill_api.catalog.client.Client;

@Mapper
public interface DeliveryNoteMapper {

    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "invoiceId", source = "invoice.id")
    DeliveryNoteResponse toResponse(DeliveryNote deliveryNote);

    @Mapping(target = "articleId", source = "article.id")
    @Mapping(target = "articleName", source = "article.name")
    @Mapping(target = "deliveryNoteId", source = "deliveryNote.id")
    DeliveryNoteLineResponse toLineResponse(DeliveryNoteLine line);

    @Mapping(target = "code", source = "request.code")
    @Mapping(target = "date", source = "request.date")
    @Mapping(target = "client", source = "client")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "format", ignore = true)
    @Mapping(target = "vatPercentage", ignore = true)
    @Mapping(target = "surchargePercentage", ignore = true)
    @Mapping(target = "vatType", ignore = true)
    @Mapping(target = "surchargeType", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    DeliveryNote toEntity(DeliveryNoteRequest request, Client client);

    @InheritConfiguration
    void updateEntity(@MappingTarget DeliveryNote deliveryNote, DeliveryNoteRequest request, Client client);

    @Mapping(target = "article", source = "article")
    @Mapping(target = "deliveryNote", source = "deliveryNote")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    DeliveryNoteLine toLineEntity(DeliveryNoteLineRequest request, Article article, DeliveryNote deliveryNote);
}
