package com.luispuchol.selfbill.selfbill_api.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientFilter;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientResponse;
import com.luispuchol.selfbill.selfbill_api.specification.ClientSpecification;
import com.luispuchol.selfbill.selfbill_api.entity.Client;
import com.luispuchol.selfbill.selfbill_api.exception.BusinessException;
import com.luispuchol.selfbill.selfbill_api.exception.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.mapper.ClientMapper;
import com.luispuchol.selfbill.selfbill_api.repository.ClientRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientService implements IClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Transactional(readOnly = true)
    @Override
    public Page<ClientResponse> getAllClients(ClientFilter filter, Pageable pageable) {
        return clientRepository.findAll(ClientSpecification.withFilter(filter), pageable)
                .map(clientMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public ClientResponse getClientById(Integer id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, id));
        return clientMapper.toResponse(client);
    }

    @Transactional(readOnly = true)
    @Override
    public ClientResponse getClientByCode(Integer code) {
        Client client = clientRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, code));
        return clientMapper.toResponse(client);
    }

    @Transactional(readOnly = true)
    @Override
    public ClientResponse getClientByName(String name) {
        Client client = clientRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, name));
        return clientMapper.toResponse(client);
    }

    @Transactional(readOnly = true)
    @Override
    public ClientResponse getClientByNif(String nif) {
        Client client = clientRepository.findByNif(nif)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, nif));
        return clientMapper.toResponse(client);
    }

    @Transactional
    @Override
    public ClientResponse createClient(ClientRequest clientRequest) {
        Optional<Client> existingByCode = clientRepository.findByCode(clientRequest.getCode());
        if (existingByCode.isPresent()) {
            throw new BusinessException(ErrorCode.CLIENT_DUPLICATE_CODE, clientRequest.getCode());
        }

        Client client = clientMapper.toEntity(clientRequest);
        Client saved = clientRepository.save(client);
        return clientMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public ClientResponse updateClient(Integer id, ClientRequest clientRequest) {
        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, id));

        if (!existingClient.getCode().equals(clientRequest.getCode())) {
            Optional<Client> existingByCode = clientRepository.findByCode(clientRequest.getCode());
            if (existingByCode.isPresent()) {
                throw new BusinessException(ErrorCode.CLIENT_DUPLICATE_CODE, clientRequest.getCode());
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
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, id));

        clientRepository.delete(client);
    }
}
