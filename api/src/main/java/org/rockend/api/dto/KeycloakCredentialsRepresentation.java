package org.rockend.api.dto;

/**
 *  KeycloakCredentialsRepresentation - представляет "креды" пользователя
 */

public record KeycloakCredentialsRepresentation(
        String type,
        String value,
        Boolean temporary
) {

}
