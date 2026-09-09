package io.micronaut.interceptor.test.cdi;

import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;

/**
 * Observes an event. The counterpart of a CDI observer method, which the specification intercepts as it does any
 * other business method.
 */
@Singleton
@Monitored
public class SignalListener {

    @EventListener
    public void onSignal(Signal signal) {
        Calls.RECORDED.add("observed " + signal.text());
    }
}
