package io.micronaut.interceptor.test.binding;

import jakarta.inject.Singleton;

@Singleton
@Cached(region = "users")
public class RegionedService {

    /**
     * Declares the binding of its class again with a value only for the member excluded from the binding, so the
     * region it is bound by is the one {@link Cached} defaults to rather than the one of the class.
     */
    @Cached(comment = "a comment the binding ignores")
    public String replacedByAComment() {
        return "comment";
    }
}
