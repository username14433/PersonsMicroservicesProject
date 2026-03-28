package org.rockend.api.config;

import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
    В данном конфигурационном классе мы конфигурируем бины для:
        - RestTemplate (будем использовать в тестах)
        - WebClient (для обращений к Keycloak)
        - HttpMessageConverters - для корректной работы клиентов
 */

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebClient keycloakWebClient(KeycloakProperties keycloakProperties) {
        //WebClient в Spring Boot — это современный реактивный, неблокирующий HTTP-клиент,
        // пришедший на смену устаревающему RestTemplate. Он используется для эффективного выполнения
        // синхронных и асинхронных запросов к внешним API и между микросервисами,
        return WebClient.builder()
                //Чтобы, когда мы будем использовать этот WebClient для отправки запросов, нам не нужно было
                // каждый раз писать полный URL. Мы сможем писать относительные пути
                .baseUrl(keycloakProperties.realmUrl())
                //Здесь мы говорим клиенту: «Во всех запросах, которые ты будешь отправлять, автоматически добавляй
                // HTTP-заголовок Content-Type: application/x-www-form-urlencoded»
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }

    @Bean
    public HttpMessageConverters httpMessageConverters() {
        return new HttpMessageConverters();
    }
}
