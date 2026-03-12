package org.rockend.api.mapper;

import org.rockend.individual.dto.IndividualDto;
import org.rockend.individual.dto.IndividualWriteDto;
import org.rockend.individual.dto.IndividualWriteResponseDto;
import org.mapstruct.Mapper;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR)
public interface PersonMapper {

    org.rockend.person.dto.IndividualWriteDto from(IndividualWriteDto dto);

    org.rockend.person.dto.IndividualDto from(IndividualDto dto);

    IndividualDto from(org.rockend.person.dto.IndividualDto dto);

    IndividualWriteResponseDto from(org.rockend.person.dto.IndividualWriteResponseDto dto);
}