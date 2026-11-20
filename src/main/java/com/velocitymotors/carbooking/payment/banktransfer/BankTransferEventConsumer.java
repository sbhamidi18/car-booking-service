package com.velocitymotors.carbooking.payment.banktransfer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class BankTransferEventConsumer {
    private final BankTransferPaymentService service;
    private final JsonMapper json;

    public BankTransferEventConsumer(BankTransferPaymentService service, JsonMapper json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = "bank-transfer-payment-events", groupId = "${spring.kafka.consumer.group-id:car-booking-service}")
    public void consume(String payload) {
        BankTransferEvent event;
        try {
            event = json.readValue(payload, BankTransferEvent.class);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Malformed bank-transfer JSON event", exception);
        }
        service.process(event);
    }
}
