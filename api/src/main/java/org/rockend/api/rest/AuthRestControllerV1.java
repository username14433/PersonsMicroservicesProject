package org.rockend.api.rest;

import lombok.RequiredArgsConstructor;
import org.rockend.api.service.TokenService;
import org.rockend.api.service.UserService;
import org.rockend.individual.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

/**
 *  AuthRestControllerV1 имеет 4 эндпоинта:
 *      - получение данных пользователя на основе аутентификации
 *      - логин
 *      - обновление токена (refresh token)
 *      - регистрация
 */

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/v1/auth")
public class AuthRestControllerV1 {

    private final UserService userService;
    private final TokenService tokenService;

    @GetMapping("/me")
    public Mono<ResponseEntity<UserInfoResponse>> getMe() {
        return userService.getUserInfo().map(ResponseEntity::ok);
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<TokenResponse>> login(@Valid @RequestBody Mono<UserLoginRequest> request) {
        return request.flatMap(tokenService::login).map(ResponseEntity::ok);
    }

    @PostMapping("/refresh-token")
    public Mono<ResponseEntity<TokenResponse>> refreshToken(@Valid @RequestBody Mono<TokenRefreshRequest> request) {
        return request.flatMap(tokenService::refreshToken).map(ResponseEntity::ok);
    }

    @PostMapping("/registration")
    public Mono<ResponseEntity<TokenResponse>> register(@Valid @RequestBody Mono<IndividualWriteDto> request) {
        return request.flatMap(userService::register)
                .map(t -> ResponseEntity.status(HttpStatus.CREATED).body(t));
    }
}
