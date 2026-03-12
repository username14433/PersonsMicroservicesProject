package org.rockend.api.exception;

public class ApiException extends RuntimeException{
    public  ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Object ... args) {
        super(String.format(message, args));
    }
}
