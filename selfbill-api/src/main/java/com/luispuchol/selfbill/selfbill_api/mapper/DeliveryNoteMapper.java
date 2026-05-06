package com.luispuchol.selfbill.selfbill_api.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteArticlesRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteArticlesResponse;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Article;
import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNoteArticles;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.repository.ArticleRepository;
import com.luispuchol.selfbill.selfbill_api.repository.ClientRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeliveryNoteMapper {

    private final ClientRepository clientRepository;
    private final ArticleRepository articleRepository;

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
                        .total(item.getTotal())
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
                .clientName(deliveryNote.getClient().getName())
                .items(items)
                .createdAt(deliveryNote.getCreatedAt())
                .updatedAt(deliveryNote.getUpdatedAt())
                .build();
    }

    public DeliveryNote toEntity(DeliveryNoteRequest request) {
        if (request == null) {
            return null;
        }

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, request.getClientId()));

        DeliveryNote deliveryNote = DeliveryNote.builder()
                .code(request.getCode())
                .date(request.getDate())
                .total(request.getTotal())
                .client(client)
                .build();

        if (request.getLines() != null && !request.getLines().isEmpty()) {
            List<DeliveryNoteArticles> lines = new ArrayList<>();
            for (DeliveryNoteArticlesRequest lineRequest : request.getLines()) {
                lines.add(mapLineToEntity(lineRequest, deliveryNote));
            }
            deliveryNote.getDeliveryNoteArticles().addAll(lines);
        }

        return deliveryNote;
    }

    public void updateEntity(DeliveryNoteRequest request, DeliveryNote deliveryNote) {
        if (request == null || deliveryNote == null) {
            return;
        }

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, request.getClientId()));

        deliveryNote.setCode(request.getCode());
        deliveryNote.setDate(request.getDate());
        deliveryNote.setTotal(request.getTotal());
        deliveryNote.setClient(client);

        deliveryNote.getDeliveryNoteArticles().clear();
        if (request.getLines() != null && !request.getLines().isEmpty()) {
            for (DeliveryNoteArticlesRequest lineRequest : request.getLines()) {
                deliveryNote.getDeliveryNoteArticles().add(mapLineToEntity(lineRequest, deliveryNote));
            }
        }
    }

    private DeliveryNoteArticles mapLineToEntity(DeliveryNoteArticlesRequest lineRequest, DeliveryNote deliveryNote) {
        Article article = articleRepository.findById(lineRequest.getArticleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, lineRequest.getArticleId()));

        return DeliveryNoteArticles.builder()
                .article(article)
                .deliveryNote(deliveryNote)
                .trazabilityCode1(lineRequest.getTrazabilityCode1())
                .trazabilityCode2(lineRequest.getTrazabilityCode2())
                .trazabilityCode3(lineRequest.getTrazabilityCode3())
                .quantity(lineRequest.getQuantity())
                .price(lineRequest.getPrice())
                .total(lineRequest.getTotal())
                .build();
    }
}
