package com.luispuchol.selfbill.selfbill_api.service;

import java.util.List;

import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientResponse;

public interface IClientService {
    List<ClientResponse> getAllClients();

    ClientResponse getClientById(Integer id);

    ClientResponse getClientByCode(Integer code);

    ClientResponse getClientByName(String name);

    ClientResponse getClientByNif(String nif);

    ClientResponse createClient(ClientRequest clientRequest);

    ClientResponse updateClient(Integer id, ClientRequest clientRequest);

    void deleteClient(Integer id);

}
