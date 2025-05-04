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
     */
    void storeEvent(String batchId, Event event);

    /**
     * Сохраняет упоминание в Redis для последующей обработки.
     */
    void storeMention(String batchId, Mention mention);

    /**
     * Обрабатывает данные конкретного батча.
     */
    BatchData processBatch(String batchId);
}
