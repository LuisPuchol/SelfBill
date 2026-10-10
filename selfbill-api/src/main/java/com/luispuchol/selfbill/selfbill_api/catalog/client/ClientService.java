package com.luispuchol.selfbill.selfbill_api.catalog.client;

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

    public ClientResponse getClientByName(String name) {
        return clientRepository.findByNameIgnoreCaseAndDeletedAtIsNull(name)
                .map(clientMapper::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, name));
    }

    public ClientResponse getClientByNif(String nif) {
        return clientRepository.findByNifAndDeletedAtIsNull(nif)
                .map(clientMapper::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND, nif));
    }

    @Transactional
    public ClientResponse createClient(ClientRequest request) {
        requireUnusedCode(request.getCode());
        return clientMapper.toResponse(clientRepository.save(clientMapper.toEntity(request)));
    }

    @Transactional
    public ClientResponse updateClient(Integer id, ClientRequest request) {
        Client client = findClient(id);
        if (!client.getCode().equals(request.getCode())) {
            requireUnusedCode(request.getCode());
        }
        boolean regimeChanged = client.getVatType() != request.getVatType()
                || client.getSurchargeType() != request.getSurchargeType();
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
