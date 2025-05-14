package com.neighbor.eventmosaic.processor.component;

import com.neighbor.eventmosaic.processor.dto.BatchData;
import com.neighbor.eventmosaic.processor.dto.ElasticEvent;
import com.neighbor.eventmosaic.processor.dto.ElasticMention;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Класс для обработки батча данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchProcessor {

    /**
     * Метод для обработки батча данных.
     * Обрабатывает события и упоминания, которые были собраны в течение временного окна.
     * В случае ошибки возвращает исходные данные.
     *
     * @param events   список событий
     * @param mentions список упоминаний
     * @return объект BatchData
     */
    public BatchData process(List<ElasticEvent> events,
                             List<ElasticMention> mentions) {

        try {

            // При необходимости добавить логику обработки данных, например, фильтрация, агрегация и тд.
            // Упоминания могут содержать идентификатор события которого нет в файле событий текущего батча

            return new BatchData(events, mentions);

        } catch (Exception e) {
            log.error("Ошибка при обработке батча: {}", e.getMessage(), e);
            // Возвращаем исходные данные, если что-то пошло не так
            return new BatchData(events, mentions);
        }
    }
}
