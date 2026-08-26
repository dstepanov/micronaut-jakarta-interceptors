package io.micronaut.interceptor.test.cdi;

/**
 * A type produced by a factory method rather than declared as a bean of its own.
 */
public class Reporter {

    public String report() {
        Calls.RECORDED.add("report");
        return "report";
    }
}
