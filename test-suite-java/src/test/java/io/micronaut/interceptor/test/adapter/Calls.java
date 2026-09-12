package io.micronaut.interceptor.test.adapter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** What the interceptor of this package recorded, from whichever thread published the event. */
public final class Calls {

    public static final List<String> RECORDED = new CopyOnWriteArrayList<>();

    private Calls() {
    }
}
