package io.micronaut.interceptor.test.variations;

import jakarta.inject.Singleton;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.ExcludeDefaultInterceptors;
import jakarta.interceptor.Interceptors;

@Singleton
@ExcludeDefaultInterceptors // <1> accepted, and does nothing: there are no default interceptors to exclude
@Interceptors(ConstructionRecordingInterceptor.class)
public class ExcludingConstructorService {

    @ExcludeClassInterceptors // <2> the interceptors the class names do not interpose on this constructor
    public ExcludingConstructorService() {
        Calls.RECORDED.add("constructor");
    }

    public String work() {
        Calls.RECORDED.add("work");
        return "done";
    }
}
