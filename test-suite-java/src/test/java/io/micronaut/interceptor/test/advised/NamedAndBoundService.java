package io.micronaut.interceptor.test.advised;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
public class NamedAndBoundService {

    @Interceptors(AdvisedCountingInterceptor.class)
    @Counted
    public String work() {
        return "done";
    }

    @Counted
    public String bound() {
        return "bound";
    }
}
