package com.velocitymotors.carbooking.payment.creditcard;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class CreditCardValidationClient {
    private final RestClient restClient;

    public CreditCardValidationClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void requireApproved(String paymentReference) {
        PaymentStatusResponse response;
        try {
            response = restClient.post()
                    .uri("/payment-status")
                    .body(new PaymentStatusRequest(paymentReference))
                    .retrieve()
                    .body(PaymentStatusResponse.class);
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.value() == 400 || status.value() == 404) {
                throw new PaymentRejectedException("Payment reference was not accepted", ex);
            }
            throw new PaymentValidationUnavailableException("Credit-card validation service returned an error", ex);
        } catch (RestClientException ex) {
            throw new PaymentValidationUnavailableException("Credit-card validation service is unavailable", ex);
        }

        if (response == null || response.status() == null) {
            throw new PaymentValidationUnavailableException("Credit-card validation response is missing status");
        }
        switch (response.status()) {
            case "APPROVED" -> { return; }
            case "REJECTED" -> throw new PaymentRejectedException("Credit-card payment was rejected");
            default -> throw new PaymentValidationUnavailableException("Unexpected credit-card payment status");
        }
    }

    private record PaymentStatusRequest(String paymentReference) { }
    private record PaymentStatusResponse(String lastUpdateDate, String status) { }
}
