
package io.micronaut.interceptor.test.construct;

import io.micronaut.context.annotation.Prototype;

import java.util.concurrent.atomic.AtomicInteger;

@Prototype
@Insistent
public class InsistentService {

    public static final AtomicInteger CONSTRUCTIONS = new AtomicInteger();

    public InsistentService() {
        CONSTRUCTIONS.incrementAndGet();
    }
}
