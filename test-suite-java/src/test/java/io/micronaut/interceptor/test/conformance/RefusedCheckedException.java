package io.micronaut.interceptor.test.conformance;

public class RefusedCheckedException extends Exception {

    private static final long serialVersionUID = 1L;

    public RefusedCheckedException(String message) {
        super(message);
    }
}
