package com.luispuchol.selfbill.selfbill_api.billing.traceability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteLineRequest;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteRequest;
import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@AutoConfigureMockMvc
@DisplayName("Traceability queries")
class TraceabilityTest extends DomainIntegrationTest {

    private static final LocalDateTime APRIL_10 = LocalDateTime.of(2026, 4, 10, 8, 0);
    private static final String LOT_A = "02010426";
    private static final String LOT_B = "03020426";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int nextCode = 500;

    @Test
    @DisplayName("Lists the articles sold in the period, by code")
    void articlesSoldInThePeriod() throws Exception {
        Integer a20 = articleService.createArticle(article(20, "Pera")).id();
        Integer a10 = articleService.createArticle(article(10, "Manzana")).id();
        Integer may = articleService.createArticle(article(30, "Uva")).id();
        Integer gone = articleService.createArticle(article(40, "Kiwi")).id();
        note(clientId, LocalDateTime.of(2026, 4, 30, 18, 0), line(a20, LOT_A, null, null, "1"));
        note(clientId, APRIL_10, line(a10, LOT_A, null, null, "1"));
        note(clientId, LocalDateTime.of(2026, 5, 1, 0, 0), line(may, LOT_A, null, null, "1"));
        deliveryNoteService.deleteDeliveryNote(
                note(clientId, APRIL_10, line(gone, LOT_A, null, null, "1")).id());

        articles()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].articleId").value(a10))
                .andExpect(jsonPath("$[0].articleCode").value(10))
                .andExpect(jsonPath("$[0].articleName").value("Manzana"))
                .andExpect(jsonPath("$[1].articleCode").value(20))
                .andExpect(jsonPath("$[1].articleName").value("Pera"));
    }

    @Test
    @DisplayName("Counts the three slots of a line, a repeated lot once per line")
    void lotsCountEverySlot() throws Exception {
        Integer other = articleService.createArticle(article(2, "Pera")).id();
        note(clientId, APRIL_10,
                line(articleId, LOT_A, null, null, "5"),
                line(articleId, LOT_A, LOT_B, null, "3"),
                line(articleId, LOT_B, null, LOT_B, "2"),
                line(other, LOT_A, null, null, "9"));

        lots(articleId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value(LOT_A))
                .andExpect(jsonPath("$[0].quantity").value(8))
                .andExpect(jsonPath("$[1].code").value(LOT_B))
                .andExpect(jsonPath("$[1].quantity").value(5));
    }

    @Test
    @DisplayName("Lists the delivery notes of a lot, with the quantity of the lot in each")
    void deliveriesOfALot() throws Exception {
        Integer client2 = createClient(2, DeliveryNoteFormat.VALUED);
        DeliveryNoteResponse later = note(client2, LocalDateTime.of(2026, 4, 12, 8, 0),
                line(articleId, LOT_B, LOT_A, null, "7"));
        DeliveryNoteResponse first = note(clientId, APRIL_10,
                line(articleId, LOT_A, null, null, "4"),
                line(articleId, LOT_A, null, null, "6"));
        note(clientId, APRIL_10, line(articleId, LOT_B, null, null, "1"));
        DeliveryNoteResponse slot3 = note(clientId, LocalDateTime.of(2026, 4, 14, 8, 0),
                line(articleId, LOT_B, null, LOT_A, "2"));

        deliveries(articleId, LOT_A)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].deliveryNoteId").value(first.id()))
                .andExpect(jsonPath("$[0].deliveryNoteCode").value(first.code()))
                .andExpect(jsonPath("$[0].date").value("2026-04-10T08:00:00"))
                .andExpect(jsonPath("$[0].clientId").value(clientId))
                .andExpect(jsonPath("$[0].clientCode").value(1))
                .andExpect(jsonPath("$[0].clientName").value("Client 1"))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[1].deliveryNoteId").value(later.id()))
                .andExpect(jsonPath("$[1].clientCode").value(2))
                .andExpect(jsonPath("$[1].quantity").value(7))
                .andExpect(jsonPath("$[2].deliveryNoteId").value(slot3.id()))
                .andExpect(jsonPath("$[2].quantity").value(2));
    }

    @Test
    @DisplayName("Leaves out deleted notes and lines removed by editing")
    void leavesOutDeletedLinesAndNotes() throws Exception {
        Integer other = articleService.createArticle(article(2, "Pera")).id();
        deliveryNoteService.deleteDeliveryNote(note(clientId, APRIL_10, line(other, LOT_B, null, null, "5")).id());
        DeliveryNoteResponse edited = note(clientId, APRIL_10,
                line(articleId, LOT_A, null, null, "1"),
                line(other, LOT_B, null, null, "2"));
        deliveryNoteService.updateDeliveryNote(edited.id(), DeliveryNoteRequest.builder()
                .date(APRIL_10).clientId(clientId)
                .lines(List.of(line(articleId, LOT_A, null, null, "1")))
                .build());

        articles().andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].articleId").value(articleId));
        lots(other).andExpect(jsonPath("$.length()").value(0));
        deliveries(other, LOT_B).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Still traces articles and clients deleted after the note was saved")
    void deletedCatalogRowsStillTrace() throws Exception {
        Integer client2 = createClient(2, DeliveryNoteFormat.VALUED);
        Integer pear = articleService.createArticle(article(2, "Pera")).id();
        note(client2, APRIL_10, line(pear, LOT_A, null, null, "4"));
        articleService.deleteArticle(pear);
        clientService.deleteClient(client2);

        articles().andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].articleName").value("Pera"));
        lots(pear).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value(LOT_A));
        deliveries(pear, LOT_A).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientCode").value(2))
                .andExpect(jsonPath("$[0].clientName").value("Client 2"));
    }

    @Test
    @DisplayName("Traces the lots migrated from the old program, of other lengths, and leaves out the lines without one")
    void lotsOfTheOldProgram() throws Exception {
        DeliveryNoteResponse migrated = note(clientId, APRIL_10,
                line(articleId, LOT_A, null, null, "5"),
                line(articleId, LOT_B, null, null, "3"));
        // The API no longer accepts these shapes: only an import writes them
        jdbcTemplate.update("update delivery_notes_articles set traceability_code1 = '010301' where traceability_code1 = ?", LOT_A);
        jdbcTemplate.update("update delivery_notes_articles set traceability_code1 = '' where traceability_code1 = ?", LOT_B);

        lots(articleId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("010301"))
                .andExpect(jsonPath("$[0].quantity").value(5));
        deliveries(articleId, "010301")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].deliveryNoteId").value(migrated.id()));
    }

    @Test
    @DisplayName("Requires every parameter and a code of digits")
    void requiresItsParameters() throws Exception {
        for (String missing : List.of("dateFrom", "dateTo")) {
            mockMvc.perform(without("/api/traceability/articles", missing, "dateFrom", "dateTo"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
        for (String missing : List.of("articleId", "dateFrom", "dateTo")) {
            mockMvc.perform(without("/api/traceability/lots", missing, "articleId", "dateFrom", "dateTo"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
        for (String missing : List.of("articleId", "code", "dateFrom", "dateTo")) {
            mockMvc.perform(without("/api/traceability/deliveries", missing, "articleId", "code", "dateFrom",
                    "dateTo"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
        deliveries(articleId, "L-24").andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    private static MockHttpServletRequestBuilder without(String path, String missing, String... names) {
        MockHttpServletRequestBuilder request = get(path);
        for (String name : names) {
            if (name.equals(missing)) continue;
            request.param(name, switch (name) {
                case "articleId" -> "1";
                case "code" -> LOT_A;
                case "dateFrom" -> "2026-04-01";
                default -> "2026-04-30";
            });
        }
        return request;
    }

    private ResultActions articles() throws Exception {
        return mockMvc.perform(get("/api/traceability/articles")
                .param("dateFrom", "2026-04-01").param("dateTo", "2026-04-30"));
    }

    private ResultActions lots(Integer article) throws Exception {
        return mockMvc.perform(get("/api/traceability/lots").param("articleId", String.valueOf(article))
                .param("dateFrom", "2026-04-01").param("dateTo", "2026-04-30"));
    }

    private ResultActions deliveries(Integer article, String code) throws Exception {
        return mockMvc.perform(get("/api/traceability/deliveries").param("articleId", String.valueOf(article))
                .param("code", code).param("dateFrom", "2026-04-01").param("dateTo", "2026-04-30"));
    }

    private DeliveryNoteResponse note(Integer client, LocalDateTime date, DeliveryNoteLineRequest... lines) {
        return deliveryNoteService.createDeliveryNote(DeliveryNoteRequest.builder()
                .code(nextCode++).date(date).clientId(client).lines(List.of(lines)).build());
    }

    private static DeliveryNoteLineRequest line(Integer article, String code1, String code2, String code3,
            String quantity) {
        return DeliveryNoteLineRequest.builder()
                .articleId(article)
                .traceabilityCode1(code1).traceabilityCode2(code2).traceabilityCode3(code3)
                .quantity(new BigDecimal(quantity))
                .price(BigDecimal.ONE)
                .build();
    }
}
