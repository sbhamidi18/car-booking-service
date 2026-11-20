package com.velocitymotors.carbooking.payment.creditcard;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CreditCardClientConfiguration {
    @Bean
    CreditCardValidationClient creditCardValidationClient(
            @Value("${credit-card.base-url}") String baseUrl,
            @Value("${credit-card.connect-timeout:2s}") Duration connectTimeout,
            @Value("${credit-card.read-timeout:3s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        RestClient client = RestClient.builder().baseUrl(baseUrl)
                .requestFactory(factory).build();
        return new CreditCardValidationClient(client);
    }
}
