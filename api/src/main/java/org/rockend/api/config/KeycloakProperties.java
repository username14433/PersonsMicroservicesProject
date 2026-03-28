package org.rockend.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
    Данный компонент предоставляет конфигурационные свойства для Keycloak
 */

//Данная аннотация нужна для того, чтобы обозначить блок application.keycloak
// в файле application.properties
@ConfigurationProperties("application.keycloak")
public record KeycloakProperties (
    String serverUrl,
    String realmUrl,
    String tokenUrl,
    String clientSecret,
    String clientId,
    String realm,
    String adminUsername,
    String adminPassword,
    String adminClientId
) { }
