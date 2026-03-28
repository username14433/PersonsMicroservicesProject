package org.rockend.api.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rockend.api.client.KeycloakClient;
import org.rockend.api.dto.KeycloakCredentialsRepresentation;
import org.rockend.api.dto.KeycloakUserRepresentation;
import org.rockend.api.exception.ApiException;
import org.rockend.api.mapper.TokenResponseMapper;
import org.rockend.individual.dto.IndividualWriteDto;
import org.rockend.individual.dto.TokenResponse;
import org.rockend.individual.dto.UserInfoResponse;
import org.rockend.keycloak.dto.UserLoginRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;

/**
 * UserService - Описывает основную логику приложения:
 * - получение данных по пользователю
 * - регистрация пользователя (SAGA-оркестрация)
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final PersonService personService;
    private final KeycloakClient keycloakClient;
    private final TokenResponseMapper tokenResponseMapper;

    //Возвращает информацию о текущем авторизованном пользователе
    @WithSpan("userService.getUserInfo")
    public Mono<UserInfoResponse> getUserInfo() {
        //ReactiveSecurityContextHolder.getContext() - это реактивный способ получить текущий SecurityContext
        return ReactiveSecurityContextHolder.getContext()
                //Из SecurityContext достаётся Authentication - который описывает текущую аутентификацию пользователя
                .map(SecurityContext::getAuthentication)
                .flatMap(UserService::getUserInfoResponseMono)
                //Если SecurityContext пустой, возвращается ошибка
                .switchIfEmpty(Mono.error(new ApiException("Unable to get current user info")));
    }

    private static Mono<UserInfoResponse> getUserInfoResponseMono(Authentication authentication) {
        //Проверяем, что пользователь аутентифицирован через JWT
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            var userInfoResponse = new UserInfoResponse();
            //sub из JWT обычно содержит идентификатор пользователя
            userInfoResponse.setId(jwt.getSubject());
            //Claim в JWT - это утверждение о пользователе или токене, представленное в виде JSON-пары «ключ-значение»
            // внутри полезной нагрузки
            userInfoResponse.setEmail(jwt.getClaimAsString("email"));
            userInfoResponse.setRoles(jwt.getClaimAsStringList("roles"));

            if (jwt.getIssuedAt() != null) {
                userInfoResponse.setCreatedAt(jwt.getIssuedAt().atOffset(ZoneOffset.UTC));
            }

            log.info("User = [email={}] was successfully processed", userInfoResponse.getEmail());
            //Возвращаем реактивный поток, внутри которого dto ответа
            return Mono.just(userInfoResponse);
        }

        log.info("Unable to get current user info");
        return Mono.error(new ApiException("Unable to get current user info"));
    }

    //Данный метод делает полный сценарий регистрации пользователя
    @WithSpan("userService.register")
    public Mono<TokenResponse> register(IndividualWriteDto dto) {
        //Создание пользователя в person-service
        return personService.register(dto)
                .flatMap(personId ->
                        //Для создания пользователей нужен админский токен,
                        // поэтому сначала API логинится в Keycloak как сервис/админ и получает админский токен
                        keycloakClient.adminLogin()
                                //Создание репрезентации пользователя для Keycloak
                                .flatMap(adminToken -> {
                                    var keycloakUserRepresentation = new KeycloakUserRepresentation(
                                            null,
                                            dto.getEmail(),
                                            dto.getEmail(),
                                            true,
                                            true,
                                            null
                                    );

                                    //Создание пользователя в Keycloak
                                    return keycloakClient.registerUser(adminToken.getAccessToken(), keycloakUserRepresentation)
                                            .flatMap(keycloakUserId -> {
                                                var creds = new KeycloakCredentialsRepresentation(
                                                        //Установка "постоянного" пароля
                                                        "password",
                                                        dto.getPassword(),
                                                        false
                                                );
                                                return keycloakClient.resetUserPassword(keycloakUserId, creds, adminToken.getAccessToken());
                                            }).flatMap(r ->
                                                    //Логинимся под новым пользователем
                                                    keycloakClient
                                                            .login(new UserLoginRequest(dto.getEmail(), dto.getPassword()))
                                                            //В случае ошибки логина "откатываем" изменения в person-service
                                                            .onErrorResume(err ->
                                                                    personService.compensateRegistration(personId.getId().toString())
                                                                            //Здесь then() означает:
                                                                            // сначала дождись завершения compensation
                                                                            // потом верни ошибку
                                                                            .then(Mono.error(err)))
                                            ).map(tokenResponseMapper::toTokenResponse);
                                }));
    }
}
