package com.neighbor.eventmosaic.processor.mapper;

import com.neighbor.eventmosaic.library.common.dto.Mention;
import com.neighbor.eventmosaic.processor.dto.ElasticMention;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = MentionMapperHelper.class)
public interface MentionMapper {

    @Mapping(target = "elasticIndexDate", source = "mentionTimeDate", qualifiedByName = "longToElasticIndexDateString")
    @Mapping(target = "eventTimeDate", source = "eventTimeDate", qualifiedByName = "longToOffsetDateTime")
    @Mapping(target = "mentionTimeDate", source = "mentionTimeDate", qualifiedByName = "longToOffsetDateTime")
    ElasticMention toElasticMention(Mention mention);

    List<ElasticMention> toElasticMentionList(List<Mention> mentions);
}
