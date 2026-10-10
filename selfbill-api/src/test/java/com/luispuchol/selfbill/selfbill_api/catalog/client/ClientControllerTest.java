package com.luispuchol.selfbill.selfbill_api.catalog.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luispuchol.selfbill.selfbill_api.catalog.NextCodeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

        @MockitoBean
        private ClientService clientService;

        private ClientResponse clientResponse1;
        private ClientResponse clientResponse2;
        private ClientRequest clientRequest;

        @BeforeEach
        void setUp() {
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
                                .vatType(VatType.WITH_VAT)
                                .surchargeType(SurchargeType.WITHOUT_SURCHARGE)
                                .defaultDeliveryNoteFormat(DeliveryNoteFormat.VALUED)
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
                                .vatType(VatType.WITHOUT_VAT)
                                .surchargeType(SurchargeType.WITH_SURCHARGE)
                                .defaultDeliveryNoteFormat(DeliveryNoteFormat.PLAIN)
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
                                .vatType(VatType.WITH_VAT)
                                .surchargeType(SurchargeType.WITHOUT_SURCHARGE)
                                .defaultDeliveryNoteFormat(DeliveryNoteFormat.VALUED)
                                .build();
        }

        @Test
        @DisplayName("GET /api/clients - Debe retornar todos los clientes paginados")
        void getAllClients_ShouldReturnPageOfClients() throws Exception {
                List<ClientResponse> clients = Arrays.asList(clientResponse1, clientResponse2);
                Page<ClientResponse> page = new PageImpl<>(clients);
                when(clientService.getAllClients(any(ClientFilter.class), any(Pageable.class))).thenReturn(page);

                mockMvc.perform(get("/api/clients")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2)))
                                .andExpect(jsonPath("$.content[0].id", is(1)))
                                .andExpect(jsonPath("$.content[0].businessName", is("Acme Corporation SA")))
                                .andExpect(jsonPath("$.content[1].id", is(2)))
                                .andExpect(jsonPath("$.content[1].businessName", is("Tech Solutions SL")))
                                .andExpect(jsonPath("$.page.totalElements", is(2)));

                verify(clientService, times(1)).getAllClients(any(ClientFilter.class), any(Pageable.class));
        }

        @Test
        @DisplayName("GET /api/clients/{id} - Debe retornar un cliente por ID")
        void getClientById_ShouldReturnClient() throws Exception {
                when(clientService.getClientById(1)).thenReturn(clientResponse1);

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
                mockMvc.perform(get("/api/clients/0")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                verify(clientService, never()).getClientById(any());
        }

        @Test
        @DisplayName("GET /api/clients/{id} - Debe fallar con ID negativo")
        void getClientById_WithNegativeId_ShouldReturnBadRequest() throws Exception {
                mockMvc.perform(get("/api/clients/-1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                verify(clientService, never()).getClientById(any());
        }

        @Test
        @DisplayName("GET /api/clients/by-code/{code} - Debe retornar un cliente por código")
        void getClientByCode_ShouldReturnClient() throws Exception {
                when(clientService.getClientByCode(2001)).thenReturn(clientResponse1);

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
                when(clientService.getClientByName("Acme Corporation SA")).thenReturn(clientResponse1);

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
                                .vatType(VatType.WITH_VAT)
                                .surchargeType(SurchargeType.WITHOUT_SURCHARGE)
                                .defaultDeliveryNoteFormat(DeliveryNoteFormat.VALUED)
                                .build();

                when(clientService.createClient(any(ClientRequest.class))).thenReturn(createdResponse);

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
                ClientRequest invalidRequest = ClientRequest.builder().build();

                mockMvc.perform(post("/api/clients")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());

                verify(clientService, never()).createClient(any());
        }

        @Test
        @DisplayName("PUT /api/clients/{id} - Debe actualizar un cliente existente")
        void updateClient_ShouldReturnUpdatedClient() throws Exception {
                ClientResponse updatedResponse = ClientResponse.builder()
                                .id(1)
                                .code(2003)
                                .businessName("New Client Ltd")
                                .name("Mike Johnson")
                                .email("contact@newclient.com")
                                .phone("+34555555555")
                                .build();

                when(clientService.updateClient(eq(1), any(ClientRequest.class))).thenReturn(updatedResponse);

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
                mockMvc.perform(put("/api/clients/0")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(clientRequest)))
                                .andExpect(status().isBadRequest());

                verify(clientService, never()).updateClient(any(), any());
        }

        @Test
        @DisplayName("DELETE /api/clients/{id} - Debe eliminar un cliente")
        void deleteClient_ShouldReturnNoContent() throws Exception {
                doNothing().when(clientService).deleteClient(1);

                mockMvc.perform(delete("/api/clients/1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());

                verify(clientService, times(1)).deleteClient(1);
        }

        @Test
        @DisplayName("DELETE /api/clients/{id} - Debe fallar con ID inválido")
        void deleteClient_WithInvalidId_ShouldReturnBadRequest() throws Exception {
                mockMvc.perform(delete("/api/clients/-1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());

                verify(clientService, never()).deleteClient(any());
        }

    @Test
    @DisplayName("GET /api/clients/next-code - Debe devolver el código sugerido")
    void getNextCode_ShouldReturnSuggestedCode() throws Exception {
        when(clientService.getNextCode()).thenReturn(new NextCodeResponse(8));

        mockMvc.perform(get("/api/clients/next-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(8));
    }

    @Test
    @DisplayName("GET /api/clients - Debe enlazar los parámetros en el filtro")
    void getAllClients_ShouldBindFilterRecord() throws Exception {
        when(clientService.getAllClients(any(ClientFilter.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/clients").param("name", "fruta").param("codeFrom", "2"))
                .andExpect(status().isOk());

        ArgumentCaptor<ClientFilter> filter = ArgumentCaptor.forClass(ClientFilter.class);
        verify(clientService).getAllClients(filter.capture(), any(Pageable.class));
        assertThat(filter.getValue().name()).isEqualTo("fruta");
        assertThat(filter.getValue().codeFrom()).isEqualTo(2);
    }
}
