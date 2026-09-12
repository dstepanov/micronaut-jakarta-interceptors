package io.micronaut.interceptor.test.factory;

/**
 * A class a factory that interposes on its own business methods produces.
 */
public class Bolt {

    public String work() {
        Calls.RECORDED.add("bolt");
        return "bolt";
    }
}
