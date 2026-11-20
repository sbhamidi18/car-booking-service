package com.velocitymotors.carbooking.payment.creditcard;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class CreditCardValidationClientHttpTest {
    private HttpServer server;
    private CreditCardValidationClient client;
    private final AtomicReference<String> body = new AtomicReference<>();
    private volatile int statusCode;
    private volatile String responseBody;

    @BeforeEach
    void startServer() throws IOException {
        statusCode = 200;
        responseBody = "{\"status\":\"APPROVED\",\"lastUpdateDate\":\"2017-07-21T17:32:28Z\"}";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/host/credit-card-payment-api/payment-status", exchange -> {
            try (exchange) {
                assertEquals("POST", exchange.getRequestMethod());
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(statusCode, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        client = new CreditCardValidationClient(RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()
                        + "/host/credit-card-payment-api").build());
    }

    @AfterEach void stopServer() { if (server != null) server.stop(0); }

    @Test void approvedResponseAndExactRequestContract() {
        assertDoesNotThrow(() -> client.requireApproved("DL123456789"));
        assertEquals("{\"paymentReference\":\"DL123456789\"}", body.get());
    }

    @Test void rejectedPaymentFails() {
        responseBody = "{\"status\":\"REJECTED\"}";
        assertThrows(PaymentRejectedException.class, () -> client.requireApproved("ref"));
    }

    @Test void notFoundPaymentFails() {
        statusCode = 404;
        responseBody = "{\"error\":\"Payment not found\"}";
        assertThrows(PaymentRejectedException.class, () -> client.requireApproved("ref"));
    }

    @Test void badRequestFails() {
        statusCode = 400;
        responseBody = "{\"error\":\"Invalid input\"}";
        assertThrows(PaymentRejectedException.class, () -> client.requireApproved("ref"));
    }

    @Test void upstreamServerFailureDoesNotApprove() {
        statusCode = 500;
        responseBody = "{\"error\":\"Internal server error\"}";
        assertThrows(PaymentValidationUnavailableException.class, () -> client.requireApproved("ref"));
    }

    @Test void unknownStatusFailsClosed() {
        responseBody = "{\"status\":\"PENDING\"}";
        assertThrows(PaymentValidationUnavailableException.class, () -> client.requireApproved("ref"));
    }

    @Test void missingStatusFailsClosed() {
        responseBody = "{}";
        assertThrows(PaymentValidationUnavailableException.class, () -> client.requireApproved("ref"));
    }
}
