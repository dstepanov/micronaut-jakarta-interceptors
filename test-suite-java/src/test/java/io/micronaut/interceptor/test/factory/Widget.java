package io.micronaut.interceptor.test.factory;

/**
 * A class two factory methods produce, each binding it to an interceptor of its own.
 */
public class Widget {

    public String work() {
        Calls.RECORDED.add("work");
        return "worked";
    }
}
