package org.rockend.person_service_.exception;

// Кастомное исключение PersonException для пробрасывания ошибок приложения
public class PersonException extends RuntimeException {

    public PersonException(String message) {
        super(message);
    }

    public PersonException(String message, Object ... args) {
        super(String.format(message, args));
    }
}
