package io.micronaut.interceptor.test.chaincache;

import jakarta.inject.Singleton;

/** A bean bound to {@link MarkedInterceptor} by a binding annotation, and to nothing else. */
@Singleton
@Marked
public class MarkedService {

    public String work() {
        return "marked";
    }
}
