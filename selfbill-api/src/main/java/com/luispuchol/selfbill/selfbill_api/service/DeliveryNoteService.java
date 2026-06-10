package com.luispuchol.selfbill.selfbill_api.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteArticlesRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteFilter;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.entity.Article;
import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.mapper.DeliveryNoteMapper;
import com.luispuchol.selfbill.selfbill_api.repository.ArticleRepository;
import com.luispuchol.selfbill.selfbill_api.repository.ClientRepository;
import com.luispuchol.selfbill.selfbill_api.repository.DeliveryNoteRepository;
import com.luispuchol.selfbill.selfbill_api.specification.DeliveryNoteSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryNoteService implements IDeliveryNoteService {

    private final DeliveryNoteRepository deliveryNoteRepository;
    private final ClientRepository clientRepository;
    private final ArticleRepository articleRepository;
    private final DeliveryNoteMapper deliveryNoteMapper;

    @Transactional(readOnly = true)
    @Override
    public Page<DeliveryNoteResponse> getAllDeliveryNotes(DeliveryNoteFilter filter, Pageable pageable) {
        return deliveryNoteRepository.findAll(DeliveryNoteSpecification.withFilter(filter), pageable)
                .map(deliveryNoteMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public DeliveryNoteResponse getDeliveryNoteById(Integer id) {
        DeliveryNote deliveryNote = deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOTE_NOT_FOUND, id));
        return deliveryNoteMapper.toResponse(deliveryNote);
    }

    @Transactional
    @Override
    public DeliveryNoteResponse createDeliveryNote(DeliveryNoteRequest request) {
        if (deliveryNoteRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException(ErrorCode.DELIVERY_NOTE_DUPLICATE_CODE, request.getCode());
        }

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, request.getClientId()));

        DeliveryNote note = deliveryNoteMapper.toEntity(request, client);
        addLines(request, note);

        return deliveryNoteMapper.toResponse(deliveryNoteRepository.save(note));
    }

    @Transactional
    @Override
    public DeliveryNoteResponse updateDeliveryNote(Integer id, DeliveryNoteRequest request) {
        DeliveryNote existing = deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOTE_NOT_FOUND, id));

        if (!existing.getCode().equals(request.getCode())) {
            Optional<DeliveryNote> noteWithSameCode = deliveryNoteRepository.findByCode(request.getCode());
            if (noteWithSameCode.isPresent()) {
                throw new BusinessException(ErrorCode.DELIVERY_NOTE_DUPLICATE_CODE, request.getCode());
            }
        }

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, request.getClientId()));

        deliveryNoteMapper.updateEntity(existing, request, client);
        existing.getDeliveryNoteArticles().clear();
        addLines(request, existing);

        return deliveryNoteMapper.toResponse(deliveryNoteRepository.save(existing));
    }

    @Transactional
    @Override
    public void deleteDeliveryNote(Integer id) {
        DeliveryNote deliveryNote = deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOTE_NOT_FOUND, id));
        deliveryNoteRepository.delete(deliveryNote);
    }

    private void addLines(DeliveryNoteRequest request, DeliveryNote note) {
        if (request.getLines() == null) return;
        for (DeliveryNoteArticlesRequest lineReq : request.getLines()) {
            Article article = articleRepository.findById(lineReq.getArticleId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND, lineReq.getArticleId()));
            note.getDeliveryNoteArticles().add(deliveryNoteMapper.lineToEntity(lineReq, article, note));
        }
    }
}
