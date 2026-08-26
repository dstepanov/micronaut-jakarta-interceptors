package io.micronaut.interceptor.test.cdi;

@Secure
public abstract class BaseService {

    public String inheritedBinding() {
        Calls.RECORDED.add("inheritedBinding");
        return "inherited";
    }
}
