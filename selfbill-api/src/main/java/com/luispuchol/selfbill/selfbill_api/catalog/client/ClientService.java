package com.luispuchol.selfbill.selfbill_api.catalog.client;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.luispuchol.selfbill.selfbill_api.catalog.NextCodeResponse;
import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final ApplicationEventPublisher eventPublisher;

    public Page<ClientResponse> getAllClients(ClientFilter filter, Pageable pageable) {
        return clientRepository.findAll(ClientSpecification.withFilter(filter), pageable)
                .map(clientMapper::toResponse);
    }

    public ClientResponse getClientById(Integer id) {
        return clientMapper.toResponse(findClient(id));
    }

    public ClientResponse getClientByCode(Integer code) {
        return clientRepository.findByCodeAndDeletedAtIsNull(code)
                .map(clientMapper::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, code));
    }

    public List<ClientResponse> getClientsByName(String name) {
        return clientRepository.findByNameIgnoreCaseAndDeletedAtIsNullOrderByCode(name).stream()
                .map(clientMapper::toResponse)
                .toList();
    }

    public List<ClientResponse> getClientsByNif(String nif) {
        return clientRepository.findByNifAndDeletedAtIsNullOrderByCode(nif).stream()
                .map(clientMapper::toResponse)
                .toList();
    }

    @Transactional
    public ClientResponse createClient(ClientRequest request) {
        requireUnusedCode(request.code());
        return clientMapper.toResponse(clientRepository.save(clientMapper.toEntity(request)));
    }

    @Transactional
    public ClientResponse updateClient(Integer id, ClientRequest request) {
        Client client = findClient(id);
        if (!client.getCode().equals(request.code())) {
            requireUnusedCode(request.code());
        }
        boolean regimeChanged = client.getVatType() != request.vatType()
                || client.getSurchargeType() != request.surchargeType();
        clientMapper.updateEntity(client, request);
        Client saved = clientRepository.save(client);
        if (regimeChanged) {
            eventPublisher.publishEvent(new ClientTaxRegimeChanged(saved));
        }
        return clientMapper.toResponse(saved);
    }

    public NextCodeResponse getNextCode() {
        return new NextCodeResponse(clientRepository.findNextCode());
    }

    @Transactional
    public void deleteClient(Integer id) {
        clientRepository.delete(findClient(id));
    }

    private Client findClient(Integer id) {
        return clientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, id));
    }

    private void requireUnusedCode(Integer code) {
        if (clientRepository.existsByCodeAndDeletedAtIsNull(code)) {
            throw new BusinessException(ErrorCode.CLIENT_DUPLICATE_CODE, code);
        }
    }
}
