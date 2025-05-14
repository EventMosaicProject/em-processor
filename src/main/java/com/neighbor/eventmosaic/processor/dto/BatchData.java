package com.neighbor.eventmosaic.processor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Класс для хранения данных батча.
 * Содержит список обработанных событий и упоминаний, готовых для Elasticsearch.
 */
@Data
@AllArgsConstructor
public class BatchData {
    private List<ElasticEvent> events;
    private List<ElasticMention> mentions;
}
