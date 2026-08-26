package io.micronaut.interceptor.test.binding;

import jakarta.inject.Singleton;

@Singleton
public class CachingService {

    @Cached
    public String withDefaultRegion() {
        return "default";
    }

    @Cached(region = "default", comment = "another comment")
    public String withDefaultRegionSpeltOut() {
        return "spelt out";
    }

    @Cached(region = "users")
    public String withUsersRegion() {
        return "users";
    }

    public String withoutCaching() {
        return "none";
    }
}
