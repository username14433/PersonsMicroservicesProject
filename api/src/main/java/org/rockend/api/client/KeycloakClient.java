package org.rockend.api.client;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rockend.api.ApiApplication;
import org.rockend.api.config.KeycloakProperties;
import org.rockend.api.dto.KeycloakCredentialsRepresentation;
import org.rockend.api.dto.KeycloakUserRepresentation;
import org.rockend.api.exception.ApiException;
import org.rockend.api.util.UserIdExtractor;
import org.rockend.keycloak.dto.TokenRefreshRequest;
import org.rockend.keycloak.dto.UserLoginRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.rockend.keycloak.dto.TokenResponse;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

/**
 * Компонент KeycloakClient необходим для инкапсуляции методов вызова Keycloak:<br>
 * - login пользователя<br>
 * - login админа<br>
 * - обновления токена<br>
 * - регистрации пользователя<br>
 * - сброса пароля<br>
 <br>
 * Определения:<br>
 *      - client_id - нужен для получения JWT-токена, чтобы keycloak знал от имени какого приложения идёт запрос<br>
 *      - JWT-токен - (Java Web Token) данный токен пользователь получает после логина,
 *          он говорит какой пользователь залогинился и какие у него роли<br>
 *      - grant_type - тип авторизации(например, 'password', 'refresh_toke'n)<br>
 *      - client_secret - секрет приложения, доказывает keycloak, что приложение,
 *      которое к нему обращается — это действительно наше приложение, а не какое-то другое<br>
 *      - Mono - тип данных для реактивных неблокирующих потоков, что означает,<br>
 *      что поток не простаивает во время ожидания ответа от, например, keycloak<br>
 *      - realm - это некое окружение со своими пользователями, приложениями и правилами.<br>
 *      Пользователи не могут попасть из одного realm в другой
 */


@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakClient {

    //Константа - обязательная часть для HTTP-заголовка Authorization (например, "Bearer eyJhbGci...")
    public static final String BEARER_PREFIX = "Bearer ";

    //Реактивный HTTP-клиент
    private final WebClient webClient;

    //Объект с настройками Keycloak (URL, realm, clientId и т.д.)
    private final KeycloakProperties props;

    private String userRegistrationUrl;
    private String userPasswordResetUrl;
    private String userByIdUrl;

    //Этот метод вызывается сразу после создания бина, но до того, как бин начнет использоваться
    @PostConstruct
    //Здесь формируются URL для административных операций с пользователями
    public void init() {
        this.userRegistrationUrl = props.serverUrl() + "/admin/realms/" + props.realm() + "/users";
        this.userByIdUrl = userRegistrationUrl + "/{id}";
        this.userPasswordResetUrl = userByIdUrl + "/reset-password";
    }

    //Для трассировки. Этот метод будет отдельным спаном(диапазоном) в трейсе
    @WithSpan("keycloakClient.login")
    public Mono<TokenResponse> login(UserLoginRequest loginRequest) {
        //Так как Keycloak требует по контракту APPLICATION_FORM_URLENCODED,
        // мы формируем тело запроса через LinkedMultiValueMap - специальная карта, где одному ключу можно
        // сопоставить несколько значений (словарь с данными для авторизации)
        var form = new LinkedMultiValueMap<String, String>();
        //Добавляем параметры формы для логина пользователя
        form.add("grant_type", "password");
        form.add("username", loginRequest.getEmail());
        form.add("password", loginRequest.getPassword());
        form.add("client_id", props.clientId());
        addIfNotBlank(form, "client_secret", props.clientSecret());

        return webClient.post()
                .uri(props.tokenUrl())
                //Формат кодирования данных, используемый в HTTP-запросах для отправки простых пар «ключ-значение»
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                //В тело запроса передаётся наша форма с полями ключ-значение
                .bodyValue(form)
                //Данный метод используется для немедленного выполнения HTTP-запроса и получения тела ответа,
                // преобразуя его в реактивные типы (Mono или Flux)
                .retrieve()
                //Если статус ответа 4xx или 5xx, вызываем метод toApiException для обработки ошибки
                .onStatus(HttpStatusCode::isError, this::toApiException)
                //Преобразуем JSON-ответ в объект TokenResponse (содержит access_token, refresh_token и т.д.)
                .bodyToMono(TokenResponse.class);
    }

    /*
        Аналогично обычному логину, но использует учетные данные администратора из настроек.
        Нужно для выполнения административных операций (создание пользователей, сброс паролей)
     */
    @WithSpan("keycloakClient.adminLogin")
    public Mono<TokenResponse> adminLogin() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("username", props.adminUsername());
        form.add("password", props.adminPassword());
        form.add("client_id", props.adminClientId());

        return webClient.post()
                .uri(props.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toApiException)
                .bodyToMono(TokenResponse.class);
    }

    //Метод, который обновляет JWT-токен, так как у него есть срок жизни и как только он истекает,
    // keycloak перестаёт принимать запросы с этим токеном
    public Mono<TokenResponse> refreshToken(TokenRefreshRequest tokenRefreshRequest) {
        var form = new LinkedMultiValueMap<String, String>();
        //Говорим Keycloak, что хотим обновить токен.
        form.add("grant_type", "refresh_token");
        //Передаем старый refresh token
        form.add("refresh_token", tokenRefreshRequest.getRefreshToken());
        form.add("client_id", props.clientId());
        addIfNotBlank(form, "client_secret", props.clientSecret());

        return webClient.post()
                .uri(props.tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toApiException)
                .bodyToMono(TokenResponse.class);
    }



    @WithSpan("keycloakClient.registerUser")
    public Mono<String> registerUser(String adminToken, KeycloakUserRepresentation user) {
        return webClient.post()
                .uri(userRegistrationUrl)
                //Добавляем JWT-токен в заголовок для авторизации
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                //Здесь отправляем JSON, а не form-urlencoded
                .contentType(MediaType.APPLICATION_JSON)
                //Добавляем DTO пользователя в тело запроса
                .bodyValue(user)
                //Метод используемый для обработки HTTP-ответа и преобразования его в реактивный тип Mono<T>
                //также он, в отличие от retrieve, даёт доступ к полному ответу (включая заголовки),
                //В нём вызываем метод, который нужен, чтобы вытащить ID созданного пользователя из заголовка Location
                .exchangeToMono(this::extractIdFromPath);
    }

    //Достаёт ID пользователя из URL из заголовка Location, который возвращается при успешном создании ресурса
    private Mono<String> extractIdFromPath(ClientResponse response) {
        //Если API вернул статус CREATED после регистрации пользователя
        if (response.statusCode().equals(HttpStatus.CREATED)) {
            var location = response.headers().asHttpHeaders().getLocation();
            if (location == null) {
                return Mono.error(new ApiException("Location header not found"));
            }
            //Метод just() создает реактивный поток (Mono), который испускает одно указанное значение, а затем завершается
            return Mono.just(UserIdExtractor.extractIdFromPath(location.getPath()));
        }
        return response.bodyToMono(String.class)
                .flatMap(body -> Mono.error(new ApiException("User registration failed: " + body)));
    }


    @WithSpan("keycloakClient.resetUserPassword")
    public Mono<Void> resetUserPassword(String userId, KeycloakCredentialsRepresentation dto, String adminToken) {
        return webClient.put()
                //Подставляем userId вместо {id} в URL
                .uri(userPasswordResetUrl, userId)
                //Добавляем токен в заголовок для авторизации
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new ApiException("User reset failed: " + body))))
                //Нам не нужен ответ от Keycloak (при успехе он пустой), поэтому превращаем в Mono<Void>
                .toBodilessEntity()
                .then();
    }

    //Данный метод выполняет компенсационное действие (rollback), если при регистрации пользователя
    // и установке ему пароля возникла ошибка
    @WithSpan("keycloakClient.resetUserPassword.executeOnError")
    public Mono<ResponseEntity<Void>> executeOnError(String userId, String adminAccessToken, Throwable exception) {
        return webClient.delete()
                .uri(userByIdUrl, userId)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminAccessToken)
                .retrieve()
                .toBodilessEntity()
                .then(Mono.error(exception));
    }

    //Добавляет параметр в форму только если значение не null и не пустое. Нужно для опционального client_secret
    private static void addIfNotBlank(LinkedMultiValueMap<String, String> form, String key, String value) {
        if (StringUtils.hasLength(value)) {
            form.add(key, value);
        }
    }

    //Преобразует ошибки Keycloak в ApiException
    private Mono<? extends Throwable> toApiException(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty(response.statusCode().toString())
                .map(body -> new ApiException("Keycloak error " + response.statusCode() + ": " + body));
    }

}
