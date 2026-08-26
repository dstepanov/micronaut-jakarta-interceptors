package io.micronaut.interceptor.test.edge;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
@MemberBound("service")
public class MemberBoundService {

    @Interceptors(NamedFirstInterceptor.class)
    public String greet(String name) {
        Log.RECORDED.add("target got " + name);
        return "Hello " + name;
    }
}
