package com.neighbor.eventmosaic.processor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO упоминания события для Elasticsearch.
 */
@Data
@NoArgsConstructor
public class ElasticMention {

    /**
     * Идентификатор события, которое было упомянуто
     */
    private Long globalEventId;

    /**
     * Дата в формате ГГГГ-ММ-ДД
     * Используется для создания индекса в elasticsearch/
     * В БД не сохранятся
     */
    private String elasticIndexDate;

    /**
     * Временная метка (ГГГГММДДЧЧММСС) первой регистрации события
     */
    private OffsetDateTime eventTimeDate;

    /**
     * Временная метка (ГГГГММДДЧЧММСС) текущего 15-ти минутного обновления
     */
    private OffsetDateTime mentionTimeDate;

    /**
     * Тип источника документа (1-6)
     */
    private Integer mentionType;

    /**
     * Идентификатор источника документа
     */
    private String mentionSourceName;

    /**
     * Уникальный внешний идентификатор документа
     */
    private String mentionIdentifier;

    /**
     * Номер предложения в статье, где было упомянуто событие
     */
    private Integer sentenceId;

    /**
     * Позиция Actor1 в статье (в символах)
     */
    private Integer actor1CharOffset;

    /**
     * Позиция Actor2 в статье (в символах)
     */
    private Integer actor2CharOffset;

    /**
     * Позиция действия в статье (в символах)
     */
    private Integer actionCharOffset;

    /**
     * Флаг, указывающий было ли событие найдено в необработанном тексте (1) или потребовалась обработка (0)
     */
    private Integer inRawText;

    /**
     * Уверенность в извлечении события (процент)
     */
    private Integer confidence;

    /**
     * Длина исходного документа в символах
     */
    private Integer mentionDocLen;

    /**
     * Тон документа (-100 до +100)
     */
    private Double mentionDocTone;

    /**
     * Информация о переводе документа
     */
    private String mentionDocTranslationInfo;
}
