package com.luispuchol.selfbill.selfbill_api.mapper;

import java.util.List;

import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteArticlesResponse;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;

public class DeliveryNoteMapper {

    public DeliveryNoteResponse toResponse(DeliveryNote deliveryNote) {
        if (deliveryNote == null) {
            return null;
        }

        List<DeliveryNoteArticlesResponse> items = deliveryNote.getDeliveryNoteArticles().stream()
                .map(item -> DeliveryNoteArticlesResponse.builder()
                        .id(item.getId())
                        .deliveryNoteId(item.getDeliveryNote().getId())
                        .articleId(item.getArticle().getId())
                        .articleName(item.getArticle().getName())
                        .trazabilityCode1(item.getTrazabilityCode1())
                        .trazabilityCode2(item.getTrazabilityCode2())
                        .trazabilityCode3(item.getTrazabilityCode3())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .build())
                .toList();

        return DeliveryNoteResponse.builder()
                .id(deliveryNote.getId())
                .code(deliveryNote.getCode())
                .date(deliveryNote.getDate())
                .total(deliveryNote.getTotal())
                .clientId(deliveryNote.getClient().getId())
                .items(items)
                .createdAt(deliveryNote.getCreatedAt())
                .updatedAt(deliveryNote.getUpdatedAt())
                .build();
    }

    public DeliveryNote toEntity(DeliveryNoteRequest deliveryNote) {
        if (deliveryNote == null) {
            return null;
        }
        return DeliveryNote.builder()
                .code(deliveryNote.getCode())
                .date(deliveryNote.getDate())
                .total(deliveryNote.getTotal())
                .build();
    }

}
