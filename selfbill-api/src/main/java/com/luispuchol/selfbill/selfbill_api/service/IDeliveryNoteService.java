package com.luispuchol.selfbill.selfbill_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteFilter;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteResponse;

public interface IDeliveryNoteService {
    Page<DeliveryNoteResponse> getAllDeliveryNotes(DeliveryNoteFilter filter, Pageable pageable);

    DeliveryNoteResponse getDeliveryNoteById(Integer id);

    DeliveryNoteResponse createDeliveryNote(DeliveryNoteRequest deliveryNoteRequest);

    DeliveryNoteResponse updateDeliveryNote(Integer id, DeliveryNoteRequest deliveryNoteRequest);

    void deleteDeliveryNote(Integer id);
}
