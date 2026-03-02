package org.rockend.person_service_.mapper;

import lombok.Setter;
import org.mapstruct.*;
import org.rockend.person.dto.AddressDto;
import org.rockend.person.dto.IndividualWriteDto;
import org.rockend.person_service_.entity.Address;
import org.rockend.person_service_.entity.Country;
import org.rockend.person_service_.entity.User;
import org.rockend.person_service_.exception.PersonException;
import org.rockend.person_service_.repository.CountryRepository;
import org.rockend.person_service_.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

//componentModel: для интеграции со Spring
//injectionStrategy: используется для определения способа внедрения зависимостей (DI) в сгенерированный код маппера
@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR)

//onMethod_: используется для добавления специфичных аннотаций
// (в данном случае @Autowired на сгенерированный метод-сеттер, а не на само поле
@Setter(onMethod_ = @Autowired)
public abstract class AddressMapper {

    protected CountryRepository countryRepository;
    protected DateTimeUtil dateTimeUtil;


    @Named("toAddress")

    //target: Имя поля в целевом классе (DTO), которое нужно заполнить
    //constant: Задает фиксированное значение для поля цели, игнорируя источник
    @Mapping(target = "active", constant = "true")

    //expression: Позволяет использовать Java-выражения
    @Mapping(target = "created", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")

    //source: Имя поля в исходном классе (Entity), значение которого используется
    //MapStruct читает source как путь к свойству, начиная от параметра метода, который считается “корнем”.
    // То есть под капотом это работает так: dto.getAddress().getCity()
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "zipCode", source = "address.zipCode")
    @Mapping(target = "address", source = "address.address")

    //qualifiedByName: Ссылка на именованный метод преобразования, если требуется сложная пользовательская логика
    //source в комбинации с qualifiedByName передаёт своё значение в метод из qualifiedByName,
    // а его результат кладёт в поле соответствующего класса (в данном случае в Address, в поле country)
    @Mapping(target = "country", source = "address.countryCode", qualifiedByName = "toCountry")
    public abstract Address toEntity(IndividualWriteDto dto);

    @Named("fromAddress")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "zipCode", source = "zipCode")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "countryCode", source = "country.code")
    public abstract AddressDto fromEntity(Address address);

    @BeanMapping(ignoreByDefault = true) //Теперь все поля игнорируются, если только мы явно не укажем для них правило
                                         // маппинга (например, через @Mapping)
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "zipCode", source = "address.zipCode")
    @Mapping(target = "address", source = "address.address")
    @Mapping(target = "country", source = "address.countryCode", qualifiedByName = "toCountry")
    //@MappingTarget: используется для обновления существующего объекта Java данными из другого источника,
    // а не создания нового объекта
    public abstract Address update(@MappingTarget Address address, IndividualWriteDto dto);

    @Named("toCountry")
    public Country toCountry(String countryCode) {
        return countryRepository.findByCode(countryCode)
                .orElseThrow(() -> new PersonException("Unknown country code: [%s]",  countryCode));
    }

    //Это просто “удобный фасад”, чтобы в сервисе можно было писать так: addressMapper.update(user, dto);
    public Address update(User user, IndividualWriteDto dto) {
        return update(user.getAddress(), dto);
    }
}
