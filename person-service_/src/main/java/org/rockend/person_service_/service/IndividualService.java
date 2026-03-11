package org.rockend.person_service_.service;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.rockend.person.dto.IndividualDto;
import org.rockend.person.dto.IndividualPageDto;
import org.rockend.person.dto.IndividualWriteDto;
import org.rockend.person.dto.IndividualWriteResponseDto;
import org.rockend.person_service_.entity.Individual;
import org.rockend.person_service_.exception.PersonException;
import org.rockend.person_service_.mapper.IndividualMapper;
import org.rockend.person_service_.repository.IndividualRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Setter
@Service
@RequiredArgsConstructor
public class IndividualService {
    private final IndividualMapper individualMapper;

    private final IndividualRepository individualRepository;

    @Transactional
    public IndividualWriteResponseDto register(IndividualWriteDto writeDto) {
        Individual individual = individualMapper.toEntity(writeDto);
        individualRepository.save(individual);

        log.info("IN - register: individual: [{}] successfully registered", individual.getUser().getEmail());

        return new IndividualWriteResponseDto(individual.getId().toString());
    }

    public IndividualPageDto findByEmails(List<String> emails) {
        var individuals = individualRepository.findAllByEmails(emails);
        var from = individualMapper.from(individuals);
        var individualPageDto = new IndividualPageDto();
        individualPageDto.setItems(from);

        return individualPageDto;
    }

    public IndividualDto findById(UUID id) {
        var individual = individualRepository.findById(id)
                .orElseThrow(() -> new PersonException("Individual not found by id = [%s]", id));
        log.info("IN - findById: individual with id = [{}] successfully found", id);

        return individualMapper.fromEntity(individual);
    }

    @Transactional
    public void softDelete(UUID id) {
        log.info("IN - softDelete: individual with id = [{}] successfully deleted", id);
        individualRepository.softDelete(id);
    }

    @Transactional
    public void hardDelete(UUID id) {
        var individual = individualRepository.findById(id)
                        .orElseThrow(() -> new PersonException("Individual not found by id = [%s]", id));
        log.info("IN - hardDelete: individual with id = [{}] successfully deleted", id);
        individualRepository.delete(individual);
    }

    @Transactional
    public IndividualWriteResponseDto update(UUID id, IndividualWriteDto writeDto) {
        var individual = individualRepository.findById(id)
                .orElseThrow(() -> new PersonException("Individual not found by id = [%s]", id));
        //Обновляем данные пользователя
        individualMapper.update(individual, writeDto);
        //Сохраняем обновлённого пользователя
        individualRepository.save(individual);

        return new IndividualWriteResponseDto(individual.getId().toString());
    }
}
