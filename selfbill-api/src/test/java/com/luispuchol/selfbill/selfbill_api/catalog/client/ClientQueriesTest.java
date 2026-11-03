package com.luispuchol.selfbill.selfbill_api.catalog.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

class ClientQueriesTest extends DomainIntegrationTest {

    private Integer hotelId;

    @BeforeEach
    void createClients() {
        create(10, "Frutería García", "Frutas Levante SL", "B11111111");
        create(11, "Bar Pepe", "Hostelería GARCÍA SC", "B22222222");
        hotelId = create(12, "Hotel Sol", "Turismo SA", "A33333333");
    }

    private Integer create(int code, String name, String businessName, String nif) {
        return clientService.createClient(ClientRequest.builder().code(code).name(name)
                .businessName(businessName).nif(nif).vatType(VatType.WITH_VAT)
                .surchargeType(SurchargeType.WITHOUT_SURCHARGE)
                .defaultDeliveryNoteFormat(DeliveryNoteFormat.PLAIN).build()).id();
    }

    private List<Integer> codes(ClientFilter filter) {
        return clientService.getAllClients(filter, PageRequest.of(0, 20)).map(ClientResponse::code).getContent();
    }

    @Test
    void findsByTradeNameBusinessNameOrNif() {
        assertThat(codes(ClientFilter.builder().q("garcia").build())).containsExactlyInAnyOrder(10, 11);
        assertThat(codes(ClientFilter.builder().q("B2222").build())).containsExactly(11);
        assertThat(codes(ClientFilter.builder().q("HOTEL").build())).containsExactly(12);
    }

    @Test
    void ignoresAccentsAndCase() {
        assertThat(codes(ClientFilter.builder().q("GARCÍA").build())).containsExactlyInAnyOrder(10, 11);
        assertThat(codes(ClientFilter.builder().q("frutería").build())).containsExactly(10);
    }

    @Test
    void combinesWithTheCodeRange() {
        assertThat(codes(ClientFilter.builder().q("garcia").codeFrom(11).build())).containsExactly(11);
    }

    @Test
    void ignoresABlankQ() {
        assertThat(codes(ClientFilter.builder().q("  ").build())).containsExactlyInAnyOrder(1, 10, 11, 12);
    }

    @Test
    void keepsSearchTextUpToDateOnUpdate() {
        clientService.updateClient(hotelId, ClientRequest.builder().code(12).name("Hotel Mar")
                .businessName("Turismo SA").nif("A33333333").vatType(VatType.WITH_VAT)
                .surchargeType(SurchargeType.WITHOUT_SURCHARGE)
                .defaultDeliveryNoteFormat(DeliveryNoteFormat.PLAIN).build());
        assertThat(codes(ClientFilter.builder().q("mar").build())).containsExactly(12);
    }

    @Test
    void byNameListsEveryMatch() {
        Integer first = create(20, "Dup Name", "Dup A SL", "B50000001");
        create(21, "dup NAME", "Dup B SL", "B50000002");
        Integer deleted = create(22, "DUP name", "Dup C SL", "B50000003");
        clientService.deleteClient(deleted);

        assertThat(clientService.getClientsByName("DUP NAME")).extracting(ClientResponse::code)
                .containsExactly(20, 21);
        assertThat(clientService.getClientsByName("DUP NAME").get(0).id()).isEqualTo(first);
        assertThat(clientService.getClientsByName("Nobody")).isEmpty();
    }

    @Test
    void byNifListsEveryMatch() {
        create(31, "Nif B", "Nif B SL", "B60000000");
        create(30, "Nif A", "Nif A SL", "B60000000");
        Integer deleted = create(32, "Nif C", "Nif C SL", "B60000000");
        clientService.deleteClient(deleted);

        assertThat(clientService.getClientsByNif("B60000000")).extracting(ClientResponse::code)
                .containsExactly(30, 31);
        assertThat(clientService.getClientsByNif("B69999999")).isEmpty();
    }
}
