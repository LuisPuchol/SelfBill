package com.luispuchol.selfbill.selfbill_api.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.*;
import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.mapper.ClientMapper;
import com.luispuchol.selfbill.selfbill_api.repository.ClientRepository;

import jakarta.transaction.Transactional;

import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class DeliveryNoteService implements IDeliveryNoteService {

    private final DeliveryNoteRepository deliveryNoteRepository;
    private final DeliveryNoteMapper deliveryNoteMapper;

    @Transactional
    @Override
    public List<DeliveryNoteResponse> getAllDeliveryNotes() {
        return deliveryNoteRepository.findAll().stream()
                .map(deliveryNoteMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ClientResponse getClientById(Integer id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Client not found: " + id));
        return clientMapper.toResponse(client);
    }

    @Transactional
    @Override
    public ClientResponse getClientByCode(Integer code) {
        Client client = clientRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Client not found with code: " + code));
        return clientMapper.toResponse(client);
    }

    @Transactional
    @Override
    public ClientResponse getClientByName(String name) {
        Client client = clientRepository.findByName(name)
                .orElseThrow(() -> new BusinessException("Client not found with name: " + name));
        return clientMapper.toResponse(client);
    }

    @Transactional
    @Override
    public ClientResponse getClientByNif(String nif) {
        Client client = clientRepository.findByNif(nif)
                .orElseThrow(() -> new BusinessException("Client not found with NIF: " + nif));
        return clientMapper.toResponse(client);
    }

    @Transactional
    @Override
    public ClientResponse createClient(ClientRequest clientRequest) {
        Optional<Client> existingByCode = clientRepository.findByCode(clientRequest.getCode());
        if (existingByCode.isPresent()) {
            throw new BusinessException("Already exists client with code: " + clientRequest.getCode());
        }

        Client client = clientMapper.toEntity(clientRequest);
        Client saved = clientRepository.save(client);
        return clientMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public ClientResponse updateClient(Integer id, ClientRequest clientRequest) {
        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Client not found: " + id));

        if (!existingClient.getCode().equals(clientRequest.getCode())) {
            Optional<Client> existingByCode = clientRepository.findByCode(clientRequest.getCode());
            if (existingByCode.isPresent()) {
                throw new BusinessException("Already exists client with code: " + clientRequest.getCode());
            }
        }

        clientMapper.updateEntity(existingClient, clientRequest);
        Client updated = clientRepository.save(existingClient);
        return clientMapper.toResponse(updated);
    }

    @Transactional
    @Override
    public void deleteClient(Integer id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Client not found: " + id));

        clientRepository.delete(client);
    }
}
