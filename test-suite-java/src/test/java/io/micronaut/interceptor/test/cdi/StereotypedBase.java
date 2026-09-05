package io.micronaut.interceptor.test.cdi;

/** Carries the binding only through the stereotype, and only on the superclass. */
@Audited
public abstract class StereotypedBase {

    public String stereotyped() {
        Calls.RECORDED.add("stereotyped");
        return "done";
    }
}
