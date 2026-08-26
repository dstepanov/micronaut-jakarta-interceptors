package io.micronaut.interceptor.test.errors;

public class RefusedException extends Exception {

    private static final long serialVersionUID = 1L;

    public RefusedException(String message) {
        super(message);
    }
}
