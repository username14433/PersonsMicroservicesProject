package org.rockend.api.aspect;

/*
    Аспект, который будет вызывать созданный ранее компонент для подсчёта количества операций входа
 */

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.rockend.api.metric.LoginCountTotalMetric;
import org.springframework.stereotype.Component;


/**
    Аннотация @Aspect в Spring помечает Java-класс как аспект — компонент
    для реализации сквозной функциональности (логирование, безопасность, транзакции).
    Она указывает Spring-контейнеру, что класс содержит логику, которую нужно применить
    к другим методам (точкам среза), отделяя технический код от бизнес-логики.
 */


@Aspect
@Component
@RequiredArgsConstructor
public class LoginMetricAspect {

    private final LoginCountTotalMetric loginCountTotalMetric;

    //AfterReturning означает: «Выполнить этот код после того,
    // как целевой метод (login()) успешно вернул результат (не выбросил исключение)»
    @AfterReturning("execution(public * org.rockend.api.service.TokenService.login(..))")
    public void afterLogin() {
        loginCountTotalMetric.incrementLoginCount();
    }
}
