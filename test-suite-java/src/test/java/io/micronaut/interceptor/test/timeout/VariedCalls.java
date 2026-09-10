package io.micronaut.interceptor.test.timeout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** What the varied around-timeout interceptors recorded, written from the thread of the scheduler. */
final class VariedCalls {

    static final List<String> RECORDED = new CopyOnWriteArrayList<>();

    private VariedCalls() {
    }
}
