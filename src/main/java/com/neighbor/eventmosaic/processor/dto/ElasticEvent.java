package com.neighbor.eventmosaic.processor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO события для Elasticsearch.
 */
@Data
@NoArgsConstructor
public class ElasticEvent {

    /**
     * Глобально уникальный идентификатор события
     */
    private Long globalEventId;

    /**
     * Дата в формате ГГГГ-ММ-ДД
     * Используется для создания индекса в elasticsearch/
     * В БД не сохранятся
     */
    private String elasticIndexDate;

    /**
     * Дата события в формате ГГГГ-ММ-ДД
     */
    private OffsetDateTime eventDate;

    /**
     * Полный код CAMEO для Actor1
     */
    private String actor1Code;

    /**
     * Имя Actor1
     */
    private String actor1Name;

    /**
     * Код страны Actor1 (3-символьный)
     */
    private String actor1CountryCode;

    /**
     * Код известной группы для Actor1
     */
    private String actor1KnownGroupCode;

    /**
     * Код этнической принадлежности Actor1
     */
    private String actor1EthnicCode;

    /**
     * Первичный код религиозной принадлежности Actor1
     */
    private String actor1Religion1Code;

    /**
     * Вторичный код религиозной принадлежности Actor1
     */
    private String actor1Religion2Code;

    /**
     * Первичный код типа/роли Actor1
     */
    private String actor1Type1Code;

    /**
     * Вторичный код типа/роли Actor1
     */
    private String actor1Type2Code;

    /**
     * Третичный код типа/роли Actor1
     */
    private String actor1Type3Code;

    /**
     * Полный код CAMEO для Actor2
     */
    private String actor2Code;

    /**
     * Имя Actor2
     */
    private String actor2Name;

    /**
     * Код страны Actor2 (3-символьный)
     */
    private String actor2CountryCode;

    /**
     * Код известной группы для Actor2
     */
    private String actor2KnownGroupCode;

    /**
     * Код этнической принадлежности Actor2
     */
    private String actor2EthnicCode;

    /**
     * Первичный код религиозной принадлежности Actor2
     */
    private String actor2Religion1Code;

    /**
     * Вторичный код религиозной принадлежности Actor2
     */
    private String actor2Religion2Code;

    /**
     * Первичный код типа/роли Actor2
     */
    private String actor2Type1Code;

    /**
     * Вторичный код типа/роли Actor2
     */
    private String actor2Type2Code;

    /**
     * Третичный код типа/роли Actor2
     */
    private String actor2Type3Code;

    /**
     * Флаг, указывающий является ли событие корневым
     */
    private Integer isRootEvent;

    /**
     * Необработанный код действия CAMEO
     */
    private String eventCode;

    /**
     * Код второго уровня в таксономии CAMEO
     */
    private String eventBaseCode;

    /**
     * Код первого (корневого) уровня в таксономии CAMEO
     */
    private String eventRootCode;

    /**
     * Классификация события (1-4)
     */
    private Integer quadClass;

    /**
     * Оценка влияния события по шкале Гольдштейна (-10 до +10)
     */
    private Double goldsteinScale;

    /**
     * Количество упоминаний события
     */
    private Integer numMentions;

    /**
     * Количество источников информации с упоминаниями события
     */
    private Integer numSources;

    /**
     * Количество документов с упоминаниями события
     */
    private Integer numArticles;

    /**
     * Средний тон всех документов (-100 до +100)
     */
    private Double avgTone;

    /**
     * Тип географического объекта Actor1
     */
    private Integer actor1GeoType;

    /**
     * Полное имя географического объекта Actor1
     */
    private String actor1GeoFullName;

    /**
     * Код страны географического объекта Actor1
     */
    private String actor1GeoCountryCode;

    /**
     * Код административного подразделения 1 для Actor1
     */
    private String actor1GeoAdm1Code;

    /**
     * Код административного подразделения 2 для Actor1
     */
    private String actor1GeoAdm2Code;

    /**
     * Географическая точка Actor1
     */
    private GeoPoint actor1Location;

    /**
     * Идентификатор географического объекта Actor1
     */
    private String actor1GeoFeatureId;

    /**
     * Тип географического объекта Actor2
     */
    private Integer actor2GeoType;

    /**
     * Полное имя географического объекта Actor2
     */
    private String actor2GeoFullName;

    /**
     * Код страны географического объекта Actor2
     */
    private String actor2GeoCountryCode;

    /**
     * Код административного подразделения 1 для Actor2
     */
    private String actor2GeoAdm1Code;

    /**
     * Код административного подразделения 2 для Actor2
     */
    private String actor2GeoAdm2Code;

    /**
     * Географическая точка Actor2
     */
    private GeoPoint actor2Location;

    /**
     * Идентификатор географического объекта Actor2
     */
    private String actor2GeoFeatureId;

    /**
     * Тип географического объекта действия
     */
    private Integer actionGeoType;

    /**
     * Полное имя географического объекта действия
     */
    private String actionGeoFullName;

    /**
     * Код страны географического объекта действия
     */
    private String actionGeoCountryCode;

    /**
     * Код административного подразделения 1 для действия
     */
    private String actionGeoAdm1Code;

    /**
     * Код административного подразделения 2 для действия
     */
    private String actionGeoAdm2Code;

    /**
     * Географическая точка действия
     */
    private GeoPoint actionLocation;

    /**
     * Идентификатор географического объекта действия
     */
    private String actionGeoFeatureId;

    /**
     * Дата добавления события в базу данных GDELT (ГГГГММДДЧЧММСС)
     */
    private OffsetDateTime dateAdded;

    /**
     * URL или цитата первого новостного сообщения с упоминанием события
     */
    private String sourceUrl;
}
