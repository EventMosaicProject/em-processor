package com.neighbor.eventmosaic.processor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для хранения географической точки.
 * Содержит широту и долготу.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoPoint {

    /**
     * Широта географического объекта
     */
    private Double lat;

    /**
     * Долгота географического объекта
     */
    private Double lon;
}
