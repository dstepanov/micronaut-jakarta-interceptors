package io.micronaut.interceptor.test.graalvm;

/**
 * A bean only a factory declares.
 */
public class ProducedService {
    public String work() {
        return "done";
    }
}
