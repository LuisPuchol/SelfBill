package com.luispuchol.selfbill.selfbill_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.mapper.DeliveryNoteMapper;
import com.luispuchol.selfbill.selfbill_api.repository.DeliveryNoteRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryNoteService implements IDeliveryNoteService {

    private static final String NOT_FOUND_MSG = "Delivery note not found: ";

    private final DeliveryNoteRepository deliveryNoteRepository;
    private final DeliveryNoteMapper deliveryNoteMapper;

    @Transactional
    @Override
    public List<DeliveryNoteResponse> getAllDeliveryNotes() {
        return deliveryNoteRepository.findAll().stream()
                .map(deliveryNoteMapper::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public DeliveryNoteResponse getDeliveryNoteById(Integer id) {
        DeliveryNote deliveryNote = deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MSG + id));
        return deliveryNoteMapper.toResponse(deliveryNote);
    }

    @Transactional
    @Override
    public DeliveryNoteResponse createDeliveryNote(DeliveryNoteRequest deliveryNoteRequest) {
        if (deliveryNoteRepository.findByCode(deliveryNoteRequest.getCode()).isPresent()) {
            throw new BusinessException("Already exists delivery note with code: " + deliveryNoteRequest.getCode());
        }
        DeliveryNote deliveryNote = deliveryNoteMapper.toEntity(deliveryNoteRequest);
        DeliveryNote saved = deliveryNoteRepository.save(deliveryNote);
        return deliveryNoteMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public DeliveryNoteResponse updateDeliveryNote(Integer id, DeliveryNoteRequest deliveryNoteRequest) {
        DeliveryNote existing = deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MSG + id));
        deliveryNoteMapper.updateEntity(deliveryNoteRequest, existing);
        DeliveryNote updated = deliveryNoteRepository.save(existing);
        return deliveryNoteMapper.toResponse(updated);
    }

    @Transactional
    @Override
    public void deleteDeliveryNote(Integer id) {
        DeliveryNote deliveryNote = deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MSG + id));
        deliveryNoteRepository.delete(deliveryNote);
    }
}
