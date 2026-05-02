package com.luispuchol.selfbill.selfbill_api.service;

import java.util.List;

import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.dto.deliveryNoteDTO.DeliveryNoteResponse;

public interface IDeliveryNoteService {
    List<DeliveryNoteResponse> getAllDeliveryNotes();

    DeliveryNoteResponse getDeliveryNoteById(Integer id);

    DeliveryNoteResponse createDeliveryNote(DeliveryNoteRequest deliveryNoteRequest);

    DeliveryNoteResponse updateDeliveryNote(Integer id, DeliveryNoteRequest deliveryNoteRequest);

    void deleteDeliveryNote(Integer id);
}
