package com.neighbor.eventmosaic.processor.mapper;

import com.neighbor.eventmosaic.library.common.dto.Event;
import com.neighbor.eventmosaic.processor.dto.ElasticEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = EventMapperHelper.class)
public interface EventMapper {

    @Mapping(target = "elasticIndexDate", source = "day", qualifiedByName = "integerToElasticIndexDateString")
    @Mapping(target = "eventDate", source = "day", qualifiedByName = "integerToOffsetDate")
    @Mapping(target = "dateAdded", source = "dateAdded", qualifiedByName = "longToOffsetDateTime")
    ElasticEvent toElasticEvent(Event event);

    List<ElasticEvent> toElasticEvents(List<Event> events);
}
