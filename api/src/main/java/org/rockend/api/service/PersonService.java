package org.rockend.api.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rockend.api.mapper.PersonMapper;
import org.rockend.individual.dto.IndividualWriteDto;
import org.rockend.individual.dto.IndividualWriteResponseDto;
import org.rockend.person.api.PersonApiClient;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

/**
 *  Класс PersonService "оборачивает" обращения к person-service через feign-клиент - PersonApiClient
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonApiClient personApiClient;
    private final PersonMapper personMapper;

    //Метод для регистрации пользователя в person-service
    @WithSpan("personService.register")
    public Mono<IndividualWriteResponseDto> register(IndividualWriteDto request) {
        //fromCallable() не выполняет вызов registration() микросервиса person-api сразу,
        // а откладывает выполнение до подписки (то есть до начала выполнения данного потока),
        // так как метод registration - синхронный и если просто вызвать его
        // в реактивном потоке, то можно заблокировать весь реактивный pipeline
        return Mono.fromCallable(() -> personApiClient.registration(personMapper.from(request)))
                //Если body не null - пропускает дальше, иначе не передаёт null дальше
                .mapNotNull(HttpEntity::getBody)
                .map(personMapper::from)
                //Явно указываем Reactor на то, что источник(fromCallable) нужно выполнять на пуле потоков,
                // предназначенном для блокирующих операций
                .subscribeOn(Schedulers.boundedElastic())
                //Побочное действие = логирование
                .doOnNext(t -> log.info("Person registered id = [{}]", t.getId()));
    }

    //Данный метод нужен, чтобы выполнить компенсационную транзакцию, которая удалит пользователя
    // при возникновении ошибки при его регистрации
    //Задача данного метода - выполнить действие, завершиться успешно или с ошибкой, поэтому то он и возвращает Mono<Void>
    @WithSpan("personService.compensateRegistration")
    public Mono<Void> compensateRegistration(String id) {
        //fromRunnable отличается от fromCallable тем, что fromCallable используется, когда есть возвращаемое значение,
        // а fromRunnable - когда просто нужно выполнить действие
        return Mono.fromRunnable(() -> personApiClient.compensateRegistration(UUID.fromString(id)))
                //Метод subscribeOn "говорит" Reactor, что когда кто-то подпишется на этот Mono - выполняй его на другом потоке.
                .subscribeOn(Schedulers.boundedElastic())
                //then() - используем, когда нам не нужен промежуточный результат, а важно только успешное завершение метода
                .then();
    }
}
