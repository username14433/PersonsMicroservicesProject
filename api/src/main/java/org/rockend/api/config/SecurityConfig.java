package org.rockend.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *  SecurityConfig определяет какие именно роуты:
 *      - доступны только публично
 *      - требуют роли USER
 *      - требуют роли ADMIN
 *  Также здесь мы подключаем созданный ранее KeycloakJwtAuthenticationConverter
 */

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                //Здесь мы говорим: "Для каждого входящего обмена (HTTP-запроса) сделай то-то"
                .authorizeExchange(exchangeSpec -> exchangeSpec
                        //PUBLIC
                        .pathMatchers(
                                /*Актуатор является как бы приборной панелью приложения, он отправляет данные метрик,
                                  логов и состояние приложения по данным адресам, а alloy в свою очередь
                                  раз в какое то время дёргает эти эндпоинты и перенаправляет
                                  все эти данные в prometheus, loki, tempo
                                 */
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/actuator/info",

                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",

                                "/v1/auth/registration",
                                "/v1/auth/login",
                                "/v1/auth/refresh-token"
                        ).permitAll() //Разрешаем доступ всем без аутентификации
                        //USER
                        .pathMatchers("/v1/auth/me").hasAuthority("ROLE_individual.user")
                        // все остальные запросы, которые не подпали под правила выше требуют,
                        // чтобы пользователь был аутентифицирован, но без проверки конкретных ролей
                        .anyExchange().authenticated()
                )
                /*
                    Говорим Spring: "Мы будем принимать JWT-токены как доказательство аутентификации".
                    Приложение выступает в роли Resource Server (сервер ресурсов),
                    который проверяет токены, но сам их не выдает.
                    Внутри в лямбде подключаем свой конвертер JWT-токенов
                 */
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakAuthenticationConverter())))
                .build();
    }

    /*
        ReactiveJwtAuthenticationConverter — стандартный Spring-конвертер, который мы настраиваем и
        в нём в качестве конвертера JWT-токенов задаём наш собственный конвертер KeycloakJwtAuthenticationConverter
        Он берет Flux<GrantedAuthority> от нашего конвертера и превращает его в
        Mono<AbstractAuthenticationToken> (полноценный объект аутентификации)
     */
    private Converter<Jwt, ?extends Mono<?extends AbstractAuthenticationToken>> keycloakAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    //Реализует паттерн композиция - один объект встраивается в другой
    private Converter<Jwt, Flux<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return new KeycloakJwtAuthenticationConverter();
    }
}
