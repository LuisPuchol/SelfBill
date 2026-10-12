package com.luispuchol.selfbill.selfbill_api.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.luispuchol.selfbill.selfbill_api.billing.invoice.InvoiceResponse;
import com.luispuchol.selfbill.selfbill_api.shared.BusinessException;
import com.luispuchol.selfbill.selfbill_api.shared.ErrorCode;
import com.luispuchol.selfbill.selfbill_api.support.DomainIntegrationTest;

@DisplayName("Document email")
class DocumentEmailTest extends DomainIntegrationTest {

    @Test
    @DisplayName("A mail server that never answers makes the send fail instead of hanging")
    void silentMailServerMakesTheSendFail() throws Exception {
        try (SilentMailServer server = new SilentMailServer()) {
            InvoiceResponse invoice = issuedInvoiceToSendThrough(server);

            assertTimeoutPreemptively(Duration.ofSeconds(20), () -> assertFails(ErrorCode.EMAIL_SEND_FAILED,
                    () -> invoiceService.sendInvoiceEmail(invoice.id())));
        }
    }

    @Test
    @DisplayName("The database stays writable while the mail server takes its time")
    void sendingDoesNotHoldTheDatabase() throws Exception {
        try (SilentMailServer server = new SilentMailServer()) {
            InvoiceResponse invoice = issuedInvoiceToSendThrough(server);
            ExecutorService sender = Executors.newSingleThreadExecutor();
            Future<?> sending = sender.submit(() -> invoiceService.sendInvoiceEmail(invoice.id()));
            server.awaitConnection();

            articleService.createArticle(article(2, "Pimiento"));

            server.hangUp();
            assertThatThrownBy(() -> sending.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .cause()
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EMAIL_SEND_FAILED));
            sender.shutdown();
        }
    }

    private InvoiceResponse issuedInvoiceToSendThrough(SilentMailServer server) {
        userProfileService.updateUserProfile(profileRequest().smtpHost("127.0.0.1").smtpPort(server.port()).build());
        jdbc.update("UPDATE clients SET email = 'pedidos@client1.es' WHERE id = ?", clientId);
        return issue(clientId, createDeliveryNote(clientId, "1", "1"));
    }

    // Accepts connections and never sends the SMTP greeting
    private static final class SilentMailServer implements AutoCloseable {

        private final ServerSocket server;
        private final List<Socket> connections = new CopyOnWriteArrayList<>();
        private final CountDownLatch connected = new CountDownLatch(1);

        SilentMailServer() throws IOException {
            server = new ServerSocket(0);
            Thread.ofVirtual().start(() -> {
                try {
                    while (true) {
                        connections.add(server.accept());
                        connected.countDown();
                    }
                } catch (IOException closed) {
                    // the server was closed
                }
            });
        }

        int port() {
            return server.getLocalPort();
        }

        void awaitConnection() throws InterruptedException {
            assertThat(connected.await(10, TimeUnit.SECONDS)).isTrue();
        }

        void hangUp() throws IOException {
            for (Socket connection : connections) {
                connection.close();
            }
        }

        @Override
        public void close() throws IOException {
            hangUp();
            server.close();
        }
    }
}
