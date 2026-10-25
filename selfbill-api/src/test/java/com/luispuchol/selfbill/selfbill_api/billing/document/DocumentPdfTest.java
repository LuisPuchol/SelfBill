package com.luispuchol.selfbill.selfbill_api.billing.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.luispuchol.selfbill.selfbill_api.billing.deliverynote.DeliveryNoteResponse;
import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.catalog.client.DeliveryNoteFormat;
import com.luispuchol.selfbill.selfbill_api.mail.EmailMessage;
import com.luispuchol.selfbill.selfbill_api.mail.EmailService;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@AutoConfigureMockMvc
@DisplayName("Document PDF")
class DocumentPdfTest extends DomainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailService emailService;

    @Test
    @DisplayName("The delivery note PDF prints the note with the current profile and client")
    void deliveryNotePdf() throws Exception {
        DeliveryNoteResponse note = createDeliveryNote(clientId, "2", "1.5");

        String text = PdfText.text(pdf("/api/delivery-notes/{id}/pdf", note.id()));

        assertThat(text).contains("Albarán 1", "Luis SL · NIF B00000009", "Cliente: 1 Client 1 SL",
                PdfRendererTest.LEGEND);
    }

    @Test
    @DisplayName("The invoice PDF prints its delivery notes and totals")
    void invoicePdf() throws Exception {
        InvoiceResponse invoice = issue(clientId, createDeliveryNote(clientId, "2", "1.5"));

        String text = PdfText.text(pdf("/api/invoices/{id}/pdf", invoice.id()));

        assertThat(text).contains("Factura 1", "Albarán 1 de 01/10/2026", "Base imponible");
    }

    @Test
    @DisplayName("Reprinting after editing the client, the profile and the article prints what was issued")
    void reprintAfterEditsKeepsTheCopy() throws Exception {
        DeliveryNoteResponse note = createDeliveryNote(clientId, "2", "1.5");
        InvoiceResponse invoice = issue(clientId, note);

        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Renamed Client SL"));
        userProfileService.updateUserProfile(profileRequest().fiscalName("Renamed Issuer SL").build());
        articleService.updateArticle(articleId, article(1, "Pepino"));

        for (byte[] pdf : new byte[][] { pdf("/api/invoices/{id}/pdf", invoice.id()),
                pdf("/api/delivery-notes/{id}/pdf", note.id()) }) {
            String text = PdfText.text(pdf);
            assertThat(text).contains("Client 1 SL", "Luis SL", "Tomate");
            assertThat(text).doesNotContain("Renamed", "Pepino");
        }
    }

    @Test
    @DisplayName("Emailing a delivery note attaches the real PDF")
    void emailAttachesTheRealPdf() {
        DeliveryNoteResponse note = createDeliveryNote(clientId, "2", "1.5");
        jdbc.update("UPDATE clients SET email = 'pedidos@client1.es' WHERE id = ?", clientId);

        deliveryNoteService.sendDeliveryNoteEmail(note.id());

        ArgumentCaptor<EmailMessage> message = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).send(message.capture());
        assertThat(PdfText.text(message.getValue().attachment())).contains("Albarán 1", "Tomate");
    }

    @Test
    @DisplayName("The uploaded logo reaches the PDF; without one there is none")
    void uploadedLogoReachesThePdf() throws Exception {
        DeliveryNoteResponse note = createDeliveryNote(clientId, "2", "1.5");
        assertThat(PdfText.imageSizes(pdf("/api/delivery-notes/{id}/pdf", note.id()))).isEmpty();

        userProfileService.uploadLogo(new MockMultipartFile("file", "logo.png", "image/png", PdfText.pngBytes(120, 40)));

        assertThat(PdfText.imageSizes(pdf("/api/delivery-notes/{id}/pdf", note.id()))).hasSize(1);
    }

    @Test
    @DisplayName("A control character in a name prints as a space instead of failing the PDF")
    void controlCharacterInANameStillRenders() throws Exception {
        clientService.updateClient(clientId, clientRequest(1, DeliveryNoteFormat.VALUED, "Soft\u000Bbreak SL"));
        DeliveryNoteResponse note = createDeliveryNote(clientId, "2", "1.5");
        InvoiceResponse invoice = issue(clientId, note);

        assertThat(PdfText.text(pdf("/api/delivery-notes/{id}/pdf", note.id()))).contains("Soft break SL");
        assertThat(PdfText.text(pdf("/api/invoices/{id}/pdf", invoice.id()))).contains("Soft break SL");
    }

    private byte[] pdf(String path, Integer id) throws Exception {
        return mockMvc.perform(get(path, id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
    }
}
