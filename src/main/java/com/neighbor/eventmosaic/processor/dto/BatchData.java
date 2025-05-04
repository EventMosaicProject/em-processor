package com.neighbor.eventmosaic.processor.dto;

import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.library.common.dto.Mention;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Класс для хранения данных батча.
 * Содержит список событий и упоминаний.
 */
@Data
@AllArgsConstructor
public class BatchData {
    private List<Event> events;
    private List<Mention> mentions;
}
