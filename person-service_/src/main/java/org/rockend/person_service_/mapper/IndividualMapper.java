package org.rockend.person_service_.mapper;

import lombok.Setter;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.rockend.person.dto.IndividualDto;
import org.rockend.person.dto.IndividualWriteDto;
import org.rockend.person_service_.entity.Individual;
import org.rockend.person_service_.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import static org.springframework.util.CollectionUtils.isEmpty;

@Mapper(
        componentModel = SPRING,
        injectionStrategy = CONSTRUCTOR,
        //Массив классов, которые используются для обработки специфических типов полей
        uses = {
                UserMapper.class,
                AddressMapper.class
        }
)
@Setter(onMethod_ = @Autowired)
public abstract class IndividualMapper {

    protected DateTimeUtil dateTimeUtil;


    @Mapping(target = "active", constant = "true")
    @Mapping(target = "created", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    //В MapStruct точка "." в source означает “весь исходный объект целиком”,
    // то есть в qualifiedByName передаётся весь объект из аргумента метода
    @Mapping(target = "user", source = ".", qualifiedByName = "toUser")
    public abstract Individual toEntity(IndividualWriteDto dto);

    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "address", source = "user.address", qualifiedByName = "fromAddress")
    public abstract IndividualDto fromEntity(Individual individual);

    public List<IndividualDto> from(List<Individual> individuals) {
        return isEmpty(individuals) ? Collections.emptyList() : individuals.stream()
                .map(this::fromEntity).toList();
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "passportNumber", source = "passportNumber")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "user", expression = "java(userMapper.update(individual, dto))")
    public abstract void update(@MappingTarget Individual individual, IndividualWriteDto dto);
}
