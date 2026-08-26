package io.micronaut.interceptor.test.variations;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
@Interceptors(NamedEveryKindInterceptor.class)
public class NamedEveryKindService {

    public String work() {
        Calls.RECORDED.add("target work");
        return "done";
    }
}
