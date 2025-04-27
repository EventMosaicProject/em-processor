package com.neighbor.eventmosaic.processor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;


@Slf4j
@Configuration
public class KafkaConsumerConfig {

    private static final String BLOCKING_RETRY_TERMINAL_MESSAGE = "Достигнут предел повторных попыток для сообщения: {}. Ошибка: {}";

    @Value("${retry.max-retry-attempts}")
    private Long maxRetryAttempts;

    @Value("${retry.retry-interval-milliseconds}")
    private Long retryIntervalMilliseconds;


    /**
     * Настройка обработчика ошибок для Kafka Consumer.
     * При возникновении ошибок во время обработки сообщений
     * этот обработчик определяет поведение при повторных попытках.
     *
     * @return DefaultErrorHandler для обработки ошибок
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        FixedBackOff fixedBackOff = new FixedBackOff(retryIntervalMilliseconds, maxRetryAttempts);

        return new DefaultErrorHandler(
                (rec, ex) ->
                        log.error(BLOCKING_RETRY_TERMINAL_MESSAGE, rec.value(), ex.getMessage(), ex),
                fixedBackOff);
    }
}
