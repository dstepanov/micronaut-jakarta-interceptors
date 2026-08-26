package io.micronaut.interceptor.test.cdi;

import jakarta.inject.Singleton;

@Singleton
public class TransitiveService {

    @Critical // <1> declares @Monitored through @Critical
    public String critical() {
        Calls.RECORDED.add("critical");
        return "critical";
    }

    @Secure
    @Monitored
    public String both() {
        Calls.RECORDED.add("both method");
        return "both";
    }

    @Secure
    public String secureOnly() {
        Calls.RECORDED.add("secureOnly");
        return "secureOnly";
    }
}
