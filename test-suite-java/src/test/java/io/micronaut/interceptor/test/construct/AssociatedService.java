package io.micronaut.interceptor.test.construct;

import io.micronaut.context.annotation.Prototype;

@Prototype
@Associated
public class AssociatedService {

    public AssociatedService() {
        Stages.RECORDED.add("target constructed");
    }

    public String work() {
        return "done";
    }
}
