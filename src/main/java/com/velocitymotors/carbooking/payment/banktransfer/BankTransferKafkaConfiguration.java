package com.velocitymotors.carbooking.payment.banktransfer;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class BankTransferKafkaConfiguration {
    @Bean
    NewTopic bankTransferPaymentTopic() {
        return TopicBuilder.name("bank-transfer-payment-events").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic bankTransferPaymentDlt() {
        return TopicBuilder.name("bank-transfer-payment-events-dlt")
                .partitions(1).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler bankTransferErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class, IllegalStateException.class);
        return handler;
    }
}
