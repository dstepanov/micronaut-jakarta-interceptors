package io.micronaut.interceptor.test.factory;

/** A type produced by a factory method, whose destroy method the factory names. */
public class Disposable {

    public String use() {
        Calls.RECORDED.add("use");
        return "used";
    }

    public void close() {
        Calls.RECORDED.add("close");
    }
}
