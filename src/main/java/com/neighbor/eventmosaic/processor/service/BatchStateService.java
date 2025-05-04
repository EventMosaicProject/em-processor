package com.neighbor.eventmosaic.processor.service;


/**
 * Интерфейс сервиса для управления состоянием обработки батчей (пакетов) данных.
 */
public interface BatchStateService {

    /**
     * Регистрирует новый батч или обновляет существующий.
     */
    boolean registerBatch(String batchId);

    /**
     * Проверяет батчи, время ожидания которых истекло, и помечает их как готовые к обработке.
     */
    int checkExpiredBatchWindows();

    /**
     * Получает и удаляет один готовый для обработки батч из списка.
     */
    String getNextReadyBatch();
}
