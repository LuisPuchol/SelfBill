package com.luispuchol.selfbill.selfbill_api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientFilter;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientResponse;

public interface IClientService {
    Page<ClientResponse> getAllClients(ClientFilter filter, Pageable pageable);

    ClientResponse getClientById(Integer id);

    ClientResponse getClientByCode(Integer code);

    ClientResponse getClientByName(String name);

    ClientResponse getClientByNif(String nif);

    ClientResponse createClient(ClientRequest clientRequest);

    ClientResponse updateClient(Integer id, ClientRequest clientRequest);

    void deleteClient(Integer id);
}
