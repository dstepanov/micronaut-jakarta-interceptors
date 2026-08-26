package io.micronaut.interceptor.test.named;

import jakarta.inject.Singleton;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.Interceptors;

@Singleton
@Interceptors({AlphaInterceptor.class, BetaInterceptor.class})
public class NamedService {

    public String inherited() {
        Calls.RECORDED.add("inherited");
        return "inherited";
    }

    @Interceptors(GammaInterceptor.class)
    public String withOwn() {
        Calls.RECORDED.add("withOwn");
        return "withOwn";
    }

    @ExcludeClassInterceptors
    public String excluded() {
        Calls.RECORDED.add("excluded");
        return "excluded";
    }

    @ExcludeClassInterceptors
    @Interceptors(GammaInterceptor.class)
    public String excludedWithOwn() {
        Calls.RECORDED.add("excludedWithOwn");
        return "excludedWithOwn";
    }
}
