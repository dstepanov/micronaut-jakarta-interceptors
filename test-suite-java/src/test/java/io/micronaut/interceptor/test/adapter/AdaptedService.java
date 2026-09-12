package io.micronaut.interceptor.test.adapter;

import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;

/**
 * A bean with a method Micronaut adapts: for each such method it generates a bean of its own, implementing the
 * interface the method is adapted to and invoking the method on this bean. The generated bean carries the
 * annotations of this class, this class's binding among them.
 */
@Singleton
@Traced
public class AdaptedService {

    @EventListener
    public void onSignal(Signal signal) {
        Calls.RECORDED.add("observed " + signal.text());
    }

    public String name() {
        return "adapted";
    }
}
