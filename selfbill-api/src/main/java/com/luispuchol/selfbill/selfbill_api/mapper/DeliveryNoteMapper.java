package com.luispuchol.selfbill.selfbill_api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteArticlesRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteArticlesResponse;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Article;
import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNoteArticles;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface DeliveryNoteMapper {

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    @Mapping(source = "invoice.id", target = "invoiceId")
    @Mapping(source = "deliveryNoteArticles", target = "items")
    DeliveryNoteResponse toResponse(DeliveryNote deliveryNote);

    @Mapping(source = "article.id", target = "articleId")
    @Mapping(source = "article.name", target = "articleName")
    @Mapping(source = "deliveryNote.id", target = "deliveryNoteId")
    DeliveryNoteArticlesResponse toArticleLineResponse(DeliveryNoteArticles item);

    @Mapping(source = "request.code", target = "code")
    @Mapping(source = "request.date", target = "date")
    @Mapping(source = "request.total", target = "total")
    @Mapping(source = "client", target = "client")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "deliveryNoteArticles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    DeliveryNote toEntity(DeliveryNoteRequest request, Client client);

    @Mapping(source = "request.trazabilityCode1", target = "trazabilityCode1")
    @Mapping(source = "request.trazabilityCode2", target = "trazabilityCode2")
    @Mapping(source = "request.trazabilityCode3", target = "trazabilityCode3")
    @Mapping(source = "request.quantity", target = "quantity")
    @Mapping(source = "request.price", target = "price")
    @Mapping(source = "request.total", target = "total")
    @Mapping(source = "article", target = "article")
    @Mapping(source = "deliveryNote", target = "deliveryNote")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    DeliveryNoteArticles lineToEntity(DeliveryNoteArticlesRequest request, Article article, DeliveryNote deliveryNote);

    @Mapping(source = "request.code", target = "code")
    @Mapping(source = "request.date", target = "date")
    @Mapping(source = "request.total", target = "total")
    @Mapping(source = "client", target = "client")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "deliveryNoteArticles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(@MappingTarget DeliveryNote deliveryNote, DeliveryNoteRequest request, Client client);
}
