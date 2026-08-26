package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;

@Singleton
public class RolesService {

    @Roles({"admin", "root"})
    public String matching() {
        return "matching";
    }

    @Roles({"guest"})
    public String notMatching() {
        return "notMatching";
    }
}
