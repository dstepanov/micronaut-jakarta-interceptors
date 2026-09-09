package io.micronaut.interceptor.test.binding;

import jakarta.inject.Singleton;

@Singleton
public class TypedBindingService {

    @TypedRead
    @Cached(region = "users")
    @Labelled("one")
    @Labelled("two")
    public String work() {
        return "done";
    }
}
