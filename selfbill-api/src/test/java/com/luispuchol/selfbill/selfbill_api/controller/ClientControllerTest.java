package com.luispuchol.selfbill.selfbill_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientRequest;
import com.luispuchol.selfbill.selfbill_api.dto.clientDTO.ClientResponse;
import com.luispuchol.selfbill.selfbill_api.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@DisplayName("Client Controller Tests")
class ClientControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private ClientService clientService;

        private ClientResponse clientResponse1;
        private ClientResponse clientResponse2;
        private ClientRequest clientRequest;

        @BeforeEach
        void setUp() {
                // Preparar datos de prueba
                clientResponse1 = ClientResponse.builder()
                                .id(1)
                                .code(2001)
                                .businessName("Acme Corporation SA")
                                .name("John Doe")
                                .nif("12345678A")
                                .email("contact@acme.com")
                                .phone("+34123456789")
                                .address("Calle Principal 123")
                                .city("Madrid")
                                .province("Madrid")
                                .postalCode("28001")
                                .vatType("WITH_VAT")
                                .surchargeType("WITHOUT_SURCHARGE")
                                .invoiceMode("PER_DELIVERY_NOTE")
                                .build();

                clientResponse2 = ClientResponse.builder()
                                .id(2)
                                .code(2002)
                                .businessName("Tech Solutions SL")
                                .name("Jane Smith")
                                .nif("87654321B")
                                .email("info@techsolutions.com")
                                .phone("+34987654321")
                                .address("Avenida Tecnología 456")
                                .city("Barcelona")
                                .province("Barcelona")
                                .postalCode("08001")
                                .vatType("WITHOUT_VAT")
                                .surchargeType("WITH_SURCHARGE")
                                .invoiceMode("GROUPED")
                                .build();

                clientRequest = ClientRequest.builder()
                                .code(2003)
                                .businessName("New Client Ltd")
                                .name("Mike Johnson")
                                .nif("11223344C")
                                .email("contact@newclient.com")
                                .phone("+34555555555")
                                .address("Plaza Nueva 789")
                                .city("Valencia")
                                .province("Valencia")
                                .postalCode("46001")
                                .vatType("WITH_VAT")
                                .surchargeType("WITHOUT_SURCHARGE")
                                .invoiceMode("PER_DELIVERY_NOTE")
                                .build();
        }

        @Test
        @DisplayName("GET /api/clients - Debe retornar todos los clientes")
        void getAllClients_ShouldReturnListOfClients() throws Exception {
                // Given
                List<ClientResponse> clients = Arrays.asList(clientResponse1, clientResponse2);
                when(clientService.getAllClients()).thenReturn(clients);

                // When & Then
                mockMvc.perform(get("/api/clients")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].id", is(1)))
                                .andExpect(jsonPath("$[0].businessName", is("Acme Corporation SA")))
                                .andExpect(jsonPath("$[1].id", is(2)))
                                .andExpect(jsonPath("$[1].businessName", is("Tech Solutions SL")));

                verify(clientService, times(1)).getAllClients();
        }

        @Test
        @DisplayName("GET /api/clients/{id} - Debe retornar un cliente por ID")
        void getClientById_ShouldReturnClient() throws Exception {
                // Given
                when(clientService.getClientById(1)).thenReturn(clientResponse1);

                // When & Then
                mockMvc.perform(get("/api/clients/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(1)))
                                .andExpect(jsonPath("$.code", is(2001)))
                                .andExpect(jsonPath("$.businessName", is("Acme Corporation SA")))
                                .andExpect(jsonPath("$.name", is("John Doe")))
                                .andExpect(jsonPath("$.email", is("contact@acme.com")))
                                .andExpect(jsonPath("$.phone", is("+34123456789")));

                verify(clientService, times(1)).getClientById(1);
        }

        @Test
        @DisplayName("GET /api/clients/{id} - Debe fallar con ID inválido (0)")
        void getClientById_WithInvalidId_ShouldReturnBadRequest() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/clients/0")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                verify(clientService, never()).getClientById(any());
        }

        @Test
        @DisplayName("GET /api/clients/{id} - Debe fallar con ID negativo")
        void getClientById_WithNegativeId_ShouldReturnBadRequest() throws Exception {
                // When & Then
                mockMvc.perform(get("/api/clients/-1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                verify(clientService, never()).getClientById(any());
        }

        @Test
        @DisplayName("GET /api/clients/by-code/{code} - Debe retornar un cliente por código")
        void getClientByCode_ShouldReturnClient() throws Exception {
                // Given
                when(clientService.getClientByCode(2001)).thenReturn(clientResponse1);

                // When & Then
                mockMvc.perform(get("/api/clients/by-code/2001")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code", is(2001)))
                                .andExpect(jsonPath("$.name", is("John Doe")));

                verify(clientService, times(1)).getClientByCode(2001);
        }

        @Test
        @DisplayName("GET /api/clients/by-name/{name} - Debe retornar un cliente por nombre")
        void getClientByName_ShouldReturnClient() throws Exception {
                // Given
                when(clientService.getClientByName("Acme Corporation SA")).thenReturn(clientResponse1);

                // When & Then
                mockMvc.perform(get("/api/clients/by-name/Acme Corporation SA")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.businessName", is("Acme Corporation SA")))
                                .andExpect(jsonPath("$.id", is(1)));

                verify(clientService, times(1)).getClientByName("Acme Corporation SA");
        }

        @Test
        @DisplayName("POST /api/clients - Debe crear un nuevo cliente")
        void createClient_ShouldReturnCreatedClient() throws Exception {
                // Given
                ClientResponse createdResponse = ClientResponse.builder()
                                .id(3)
                                .code(2003)
                                .businessName("New Client Ltd")
                                .name("Mike Johnson")
                                .nif("11223344C")
                                .email("contact@newclient.com")
                                .phone("+34555555555")
                                .address("Plaza Nueva 789")
                                .city("Valencia")
                                .province("Valencia")
                                .postalCode("46001")
                                .vatType("WITH_VAT")
                                .surchargeType("WITHOUT_SURCHARGE")
                                .invoiceMode("PER_DELIVERY_NOTE")
                                .build();

                when(clientService.createClient(any(ClientRequest.class))).thenReturn(createdResponse);

                // When & Then
                mockMvc.perform(post("/api/clients")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(clientRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id", is(3)))
                                .andExpect(jsonPath("$.code", is(2003)))
                                .andExpect(jsonPath("$.businessName", is("New Client Ltd")))
                                .andExpect(jsonPath("$.email", is("contact@newclient.com")));

                verify(clientService, times(1)).createClient(any(ClientRequest.class));
        }

        @Test
        @DisplayName("POST /api/clients - Debe fallar con body inválido (sin campos requeridos)")
        void createClient_WithInvalidBody_ShouldReturnBadRequest() throws Exception {
                // Given - ClientRequest vacío o con campos null
                ClientRequest invalidRequest = ClientRequest.builder().build();

                // When & Then
                mockMvc.perform(post("/api/clients")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());

                verify(clientService, never()).createClient(any());
        }

        @Test
        @DisplayName("PUT /api/clients/{id} - Debe actualizar un cliente existente")
        void updateClient_ShouldReturnUpdatedClient() throws Exception {
                // Given
                ClientResponse updatedResponse = ClientResponse.builder()
                                .id(1)
                                .code(2003)
                                .businessName("New Client Ltd")
                                .name("Mike Johnson")
                                .email("contact@newclient.com")
                                .phone("+34555555555")
                                .build();

                when(clientService.updateClient(eq(1), any(ClientRequest.class))).thenReturn(updatedResponse);

                // When & Then
                mockMvc.perform(put("/api/clients/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(clientRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(1)))
                                .andExpect(jsonPath("$.businessName", is("New Client Ltd")))
                                .andExpect(jsonPath("$.email", is("contact@newclient.com")));

                verify(clientService, times(1)).updateClient(eq(1), any(ClientRequest.class));
        }

        @Test
        @DisplayName("PUT /api/clients/{id} - Debe fallar con ID inválido")
        void updateClient_WithInvalidId_ShouldReturnBadRequest() throws Exception {
                // When & Then
                mockMvc.perform(put("/api/clients/0")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(clientRequest)))
                                .andExpect(status().isBadRequest());

                verify(clientService, never()).updateClient(any(), any());
        }

        @Test
        @DisplayName("DELETE /api/clients/{id} - Debe eliminar un cliente")
        void deleteClient_ShouldReturnNoContent() throws Exception {
                // Given
                doNothing().when(clientService).deleteClient(1);

                // When & Then
                mockMvc.perform(delete("/api/clients/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());

                verify(clientService, times(1)).deleteClient(1);
        }

        @Test
        @DisplayName("DELETE /api/clients/{id} - Debe fallar con ID inválido")
        void deleteClient_WithInvalidId_ShouldReturnBadRequest() throws Exception {
                // When & Then
                mockMvc.perform(delete("/api/clients/-1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                verify(clientService, never()).deleteClient(any());
        }
}