package io.micronaut.interceptor.test.handoff;

import io.micronaut.context.annotation.Prototype;

/** What {@link MeddlingListener} creates in between, itself intercepted so that it has instances of its own. */
@Prototype
@Handed
public class Meddled {

    public String work() {
        return "done";
    }
}
