package org.rockend.api.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

/**
 * KeycloakJwtAuthenticationConverter определяет логику получения прав пользователя на основе JWT токена.
 * По сути, данный класс - это переходник, который вытаскивает роли из специфичного для Keycloak формата JWT и
 * преобразует их в универсальный формат Spring Security (GrantedAuthority),
 * позволяя использовать стандартные механизмы авторизации
 */


public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, Flux<GrantedAuthority>> {

    //Этот метод вызывается автоматически, когда Spring Security получает JWT и нужно понять,
    // какие права есть у пользователя.
    @Override
    public Flux<GrantedAuthority> convert(Jwt source) {
        //source - JWT токен
        //Мы заглядываем внутрь JWT и ищем поле (claim) с названием "roles"
        var roles = source.getClaimAsStringList("roles");

        //Если поля roles нет или оно пустое, то у пользователя нет прав,
        //поэтому возвращаем пустой Flux поток
        if (CollectionUtils.isEmpty(roles)) {
            return Flux.empty();
        }
        //Возвращаем реактивный поток, в котором для каждой роли из списка roles
        // вызываем конструктор SimpleGrantedAuthority (например, new SimpleGrantedAuthority("roles"))
        return Flux.fromIterable(roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList());
    }
}
