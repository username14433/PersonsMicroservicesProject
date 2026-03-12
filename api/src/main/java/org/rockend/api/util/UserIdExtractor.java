package org.rockend.api.util;

import lombok.experimental.UtilityClass;

/**
    Утилитный класс для получения USER_ID из ответа от Keycloak
 */


//Аннотация lombok - она делает класс final, создает приватный конструктор
// (бросающий исключение для защиты от рефлексии) и автоматически помечает все методы
// поля и внутренние классы как static
@UtilityClass
public class UserIdExtractor {
    public static final String REGEX_GET_SUBSTRING_AFTER_LAST_SLASH = ".*/([^/+])";

    public static String extractIdFromPath(String path) {
        return path.replaceAll(REGEX_GET_SUBSTRING_AFTER_LAST_SLASH, "$1");
    }
}
