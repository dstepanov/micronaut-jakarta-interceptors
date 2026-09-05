package io.micronaut.interceptor.test.cdi;

/**
 * Declares a binding on a method rather than on itself. The binding annotation is {@code @Inherited}, which
 * applies to a type and never to a method.
 */
public class GuardedBase {

    @Secure
    public void guarded() {
        Calls.RECORDED.add("guarded");
    }
}
