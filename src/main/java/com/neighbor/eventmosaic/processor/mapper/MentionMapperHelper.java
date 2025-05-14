package com.neighbor.eventmosaic.processor.mapper;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class MentionMapperHelper {

    /**
     * Преобразует длинное целое число в OffsetDateTime.
     * Например, 20210101123456 -> 2021-01-01T12:34:56Z
     *
     * @param timestamp длинное целое число в формате "YYYYMMDDHHMMSS"
     * @return OffsetDateTime
     */
    @Named("longToOffsetDateTime")
    public OffsetDateTime longToOffsetDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }

        String tsStr = String.valueOf(timestamp);
        if (tsStr.length() == 14) { // YYYYMMDDHHMMSS
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                LocalDateTime ldt = LocalDateTime.parse(tsStr, formatter);
                return OffsetDateTime.of(ldt, ZoneOffset.UTC);
            } catch (DateTimeParseException e) {
                log.warn("Некорректный формат поля timestamp (Long) для OffsetDateTime: {}. Ошибка: {}", timestamp, e.getMessage());
                return null;
            }
        }

        log.warn("Некорректная длина поля timestamp (Long) для OffsetDateTime: {}", timestamp);
        return null;
    }

    /**
     * Преобразует длинное целое число в строку в формате "YYYY-MM-DD".
     * Например, 20210101123456 -> "2021-01-01"
     *
     * @param timestamp длинное целое число в формате "YYYYMMDDHHMMSS"
     * @return строка в формате "YYYY-MM-DD"
     */
    @Named("longToElasticIndexDateString")
    public String longToElasticIndexDateString(Long timestamp) {
        if (timestamp == null) return null;
        String tsStr = String.valueOf(timestamp);
        if (tsStr.length() >= 8) { // Убедимся, что есть хотя бы YYYYMMDD
            // Берем только дату YYYY-MM-DD
            return tsStr.substring(0, 4) + "-" + tsStr.substring(4, 6) + "-" + tsStr.substring(6, 8);
        }
        log.warn("Некорректная длина поля timestamp (Long) для ElasticMention.elasticIndexDate: {}", timestamp);
        return null;
    }
}
