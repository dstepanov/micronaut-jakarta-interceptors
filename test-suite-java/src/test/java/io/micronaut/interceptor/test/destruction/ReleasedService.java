
package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;
import jakarta.annotation.PreDestroy;

@Prototype
@Released
public class ReleasedService {

    public String work() {
        return "done";
    }

    @PreDestroy
    void stop() {
        Destruction.CALLS.add("target destroyed");
    }
}
