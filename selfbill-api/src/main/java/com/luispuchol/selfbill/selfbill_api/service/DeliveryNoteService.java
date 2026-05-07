package com.luispuchol.selfbill.selfbill_api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.entity.DeliveryNote;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.mapper.DeliveryNoteMapper;
import com.luispuchol.selfbill.selfbill_api.repository.DeliveryNoteRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryNoteService implements IDeliveryNoteService {

    private final DeliveryNoteRepository deliveryNoteRepository;
    private final DeliveryNoteMapper deliveryNoteMapper;

    @Transactional(readOnly = true)
    @Override
    public List<DeliveryNoteResponse> getAllDeliveryNotes() {
        return deliveryNoteRepository.findAll().stream()
                .map(deliveryNoteMapper::toResponse)
                .toList();
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
    public DeliveryNoteResponse createDeliveryNote(DeliveryNoteRequest deliveryNoteRequest) {
        if (deliveryNoteRepository.findByCode(deliveryNoteRequest.getCode()).isPresent()) {
            throw new BusinessException(ErrorCode.DELIVERY_NOTE_DUPLICATE_CODE, deliveryNoteRequest.getCode());
        }
        DeliveryNote deliveryNote = deliveryNoteMapper.toEntity(deliveryNoteRequest);
        DeliveryNote saved = deliveryNoteRepository.save(deliveryNote);
        return deliveryNoteMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public DeliveryNoteResponse updateDeliveryNote(Integer id, DeliveryNoteRequest deliveryNoteRequest) {
        DeliveryNote existing = deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOTE_NOT_FOUND, id));

        if (!existing.getCode().equals(deliveryNoteRequest.getCode())) {
            Optional<DeliveryNote> noteWithSameCode = deliveryNoteRepository.findByCode(deliveryNoteRequest.getCode());
            if (noteWithSameCode.isPresent()) {
                throw new BusinessException(ErrorCode.DELIVERY_NOTE_DUPLICATE_CODE, deliveryNoteRequest.getCode());
            }
        }

        deliveryNoteMapper.updateEntity(deliveryNoteRequest, existing);
        DeliveryNote updated = deliveryNoteRepository.save(existing);
        return deliveryNoteMapper.toResponse(updated);
    }

    @Transactional
    @Override
    public void deleteDeliveryNote(Integer id) {
        DeliveryNote deliveryNote = deliveryNoteRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_NOTE_NOT_FOUND, id));
        deliveryNoteRepository.delete(deliveryNote);
    }
}
