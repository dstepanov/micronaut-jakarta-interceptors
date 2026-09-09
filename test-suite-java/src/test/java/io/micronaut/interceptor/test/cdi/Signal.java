package io.micronaut.interceptor.test.cdi;

/** The event a listener observes, standing for the event a CDI observer method takes. */
public record Signal(String text) {
}
