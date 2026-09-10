package io.micronaut.interceptor.test.binding;

import io.micronaut.context.annotation.Prototype;

/** Carries a repeated binding and an ordinary one on the class, so construction and lifecycle see both. */
@Prototype
@TypedLifecycle
@Cached(region = "orders")
@Labelled("one")
@Labelled("two")
public class TypedLifecycleService {

    public String work() {
        return "done";
    }
}
