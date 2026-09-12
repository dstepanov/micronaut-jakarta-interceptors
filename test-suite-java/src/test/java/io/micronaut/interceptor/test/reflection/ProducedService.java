package io.micronaut.interceptor.test.reflection;

/**
 * A class no bean declaration of its own is on: a factory method produces it and binds it, so the business method
 * the interception describes belongs to this class while the element the processor visits is the factory.
 */
public class ProducedService {

    public String work() {
        return "done";
    }
}
