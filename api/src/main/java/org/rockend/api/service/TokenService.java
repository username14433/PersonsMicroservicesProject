package org.rockend.api.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rockend.api.client.KeycloakClient;
import org.rockend.api.mapper.KeycloakMapper;
import org.rockend.api.mapper.TokenResponseMapper;
import org.rockend.individual.dto.TokenRefreshRequest;
import org.rockend.individual.dto.TokenResponse;
import org.rockend.individual.dto.UserLoginRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 *  TokenService "оборачивает" логику работы с токенами:
 *          - получение access_token по кредам
 *          - получение access_token по refresh_token
 *          - получение "админского" токена
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final KeycloakClient keycloakClient;
    private final KeycloakMapper keycloakMapper;
    private final TokenResponseMapper tokenResponseMapper;

    @WithSpan("tokenService.login")
    public Mono<TokenResponse> login(UserLoginRequest userLoginRequest) {
        var kcUserLoginRequest = keycloakMapper.toKeycloakUserLoginRequest(userLoginRequest);
        return keycloakClient.login(kcUserLoginRequest)
                //В качестве побочного эффекта потока выполняем логирование
                .doOnNext(t -> log.info("Token successfully generated for email = [{}]", userLoginRequest.getEmail()))
                //Если Keycloak вернул ошибку, она попадёт сюда
                .doOnError(e -> log.error("Failed to generate token for email = [{}]", userLoginRequest.getEmail()))
                //Преобразуем ответ keycloak в DTO нашего API - TokenResponse
                .map(tokenResponseMapper::toTokenResponse);
    }

    //Этот метод используется когда access_token истёк и его нужно обновить
    @WithSpan("tokenService.refreshToken")
    public Mono<TokenResponse> refreshToken(TokenRefreshRequest tokenRefreshRequest) {
        var kcTokenRefreshRequest = keycloakMapper.toKeycloakTokenRefreshRequest(tokenRefreshRequest);
        return keycloakClient.refreshToken(kcTokenRefreshRequest)
                .doOnNext(r -> log.info("Token refreshed successfully "))
                .map(tokenResponseMapper::toTokenResponse);
    }

    //Этот метод нужен для админских операций
    @WithSpan("tokenService.obtainAdminToken")
    public Mono<TokenResponse> obtainAdminToken() {
        return keycloakClient.adminLogin()
                .doOnNext(t -> log.info("Admin token obtained"))
                .map(tokenResponseMapper::toTokenResponse);
    }
}
