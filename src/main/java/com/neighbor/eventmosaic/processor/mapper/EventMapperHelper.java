package com.neighbor.eventmosaic.processor.mapper;

import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.processor.dto.GeoPoint;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class EventMapperHelper {

    /**
     * Преобразует объект Event в GeoPoint для actor1, actor2 и action.
     * Если координаты не заданы, возвращает null.
     *
     * @param event объект события
     * @return GeoPoint с координатами или null
     */
    @Named("actor1ToGeoPoint")
    public GeoPoint actor1ToGeoPoint(Event event) {
        if (event.getActor1GeoLat() != null && event.getActor1GeoLong() != null) {
            return new GeoPoint(event.getActor1GeoLat(), event.getActor1GeoLong());
        }
        return null;
    }

    /**
     * Преобразует объект Event в GeoPoint для actor2.
     * Если координаты не заданы, возвращает null.
     *
     * @param event объект события
     * @return GeoPoint с координатами или null
     */
    @Named("actor2ToGeoPoint")
    public GeoPoint actor2ToGeoPoint(Event event) {
        if (event.getActor2GeoLat() != null && event.getActor2GeoLong() != null) {
            return new GeoPoint(event.getActor2GeoLat(), event.getActor2GeoLong());
        }
        return null;
    }

    /**
     * Преобразует объект Event в GeoPoint для action.
     * Если координаты не заданы, возвращает null.
     *
     * @param event объект события
     * @return GeoPoint с координатами или null
     */
    @Named("actionToGeoPoint")
    public GeoPoint actionToGeoPoint(Event event) {
        if (event.getActionGeoLat() != null && event.getActionGeoLong() != null) {
            return new GeoPoint(event.getActionGeoLat(), event.getActionGeoLong());
        }
        return null;
    }

    /**
     * Преобразует целое число в строку в формате "YYYY-MM-DD".
     * Например, 20210101 -> "2021-01-01"
     *
     * @param day целое число в формате "YYYYMMDD"
     * @return строка в формате "YYYY-MM-DD"
     */
    @Named("integerToElasticIndexDateString")
    public String integerToElasticIndexDateString(Integer day) {
        if (day == null) {
            return null;
        }
        String dayStr = String.valueOf(day);
        if (dayStr.length() == 8) {
            return dayStr.substring(0, 4) + "-" + dayStr.substring(4, 6) + "-" + dayStr.substring(6, 8);
        }
        log.warn("Некорректный формат поля 'day' для ElasticEvent.elasticIndexDate: {}", day);
        return null;
    }

    /**
     * Преобразует целое число в OffsetDateTime на начало дня.
     * Например, 20210101 -> 2021-01-01T00:00:00Z
     *
     * @param day целое число в формате "YYYYMMDD"
     * @return OffsetDateTime на начало дня
     */
    @Named("integerToOffsetDate")
    public OffsetDateTime integerToOffsetDate(Integer day) {
        if (day == null) {
            return null;
        }
        String dayStr = String.valueOf(day);
        try {
            if (dayStr.length() == 8) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate localDate = LocalDate.parse(dayStr, formatter);
                return OffsetDateTime.of(localDate.atStartOfDay(), ZoneOffset.UTC);
            }
            log.warn("Некорректная длина поля 'day' для ElasticEvent.eventDate: {}", day);
        } catch (DateTimeParseException e) {
            log.warn("Некорректный формат поля 'day' для ElasticEvent.eventDate: {}. Ошибка: {}", day, e.getMessage());
        }
        return null;
    }

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
        try {
            if (tsStr.length() == 14) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                LocalDateTime ldt = LocalDateTime.parse(tsStr, formatter);
                return OffsetDateTime.of(ldt, ZoneOffset.UTC);
            }
            log.warn("Некорректная длина поля timestamp (Long) для OffsetDateTime: {}", timestamp);
        } catch (DateTimeParseException e) {
            log.warn("Некорректный формат поля timestamp (Long) для OffsetDateTime: {}. Ошибка: {}", timestamp, e.getMessage());
        }
        return null;
    }
}
