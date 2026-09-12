package io.micronaut.interceptor.test.factory;

/**
 * A class a static factory method produces.
 */
public class Cog {

    public String work() {
        Calls.RECORDED.add("cog");
        return "cog";
    }
}
