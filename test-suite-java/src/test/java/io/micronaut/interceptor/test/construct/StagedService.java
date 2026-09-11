package io.micronaut.interceptor.test.construct;

import io.micronaut.context.annotation.Prototype;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

@Prototype
@Staged
public class StagedService {

    public StagedService() {
        Stages.RECORDED.add("target constructed");
    }

    @Inject
    void inject(Stages stages) {
        Stages.RECORDED.add("target injected");
    }

    @PostConstruct
    void start() {
        Stages.RECORDED.add("target postConstruct");
    }
}
