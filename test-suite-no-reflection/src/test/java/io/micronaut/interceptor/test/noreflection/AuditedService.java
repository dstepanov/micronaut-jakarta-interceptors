package io.micronaut.interceptor.test.noreflection;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

@Singleton
@Audited
public class AuditedService {

    public AuditedService() {
        Probe.CALLS.add("constructor");
    }

    @PostConstruct
    void init() {
        Probe.CALLS.add("init");
    }

    @PreDestroy
    void close() {
        Probe.CALLS.add("close");
    }

    public String work(String task) {
        Probe.CALLS.add("work " + task);
        return "done " + task;
    }
}
