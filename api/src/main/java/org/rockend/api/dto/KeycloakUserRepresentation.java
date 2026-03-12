package org.rockend.api.dto;

import java.util.Map;

/**
 *  KeycloakUserRepresentation - описывает пользователя внутри Keycloak
 */

public record KeycloakUserRepresentation(
        String id,
        String username,
        String email,
        Boolean enabled,
        Boolean emailVerified,
        Map<String, String> attributes
) {

}
