package io.micronaut.interceptor.test.introduction;

import jakarta.interceptor.Interceptors;

/**
 * An abstract class the introduction advice implements in part, naming an interceptor class directly.
 */
@Stubbed
@Interceptors(NamedWatchingInterceptor.class)
public abstract class PartlyIntroducedService {

    public abstract String hello();

    public String concrete() {
        Calls.RECORDED.add("concrete");
        return "concrete";
    }
}
