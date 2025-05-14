package com.neighbor.eventmosaic.processor.service;

import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.library.common.dto.Mention;
import com.neighbor.eventmosaic.processor.dto.BatchData;

/**
 * Интерфейс сервиса для хранения и предварительной обработки событий и упоминаний.
 */
public interface EventProcessingService {

    /**
     * Сохраняет событие в Redis для последующей обработки.
     * Принимает оригинальный Event DTO.
     */
    void storeEvent(String batchId, Event event);

    /**
     * Сохраняет упоминание в Redis для последующей обработки.
     * Принимает оригинальный Mention DTO.
     */
    void storeMention(String batchId, Mention mention);

    /**
     * Обрабатывает данные конкретного батча, извлекает их из Redis,
     * маппит в ElasticEvent/ElasticMention и передает на дальнейшую обработку.
     *
     * @param batchId идентификатор батча
     * @return объект BatchData, содержащий списки ElasticEvent и ElasticMention
     */
    BatchData processBatch(String batchId);
}
