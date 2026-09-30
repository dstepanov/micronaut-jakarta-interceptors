package io.micronaut.interceptor.test.noreflection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * What the interceptor methods of this suite saw: the order they ran in, what the accessors that need no
 * reflection answered, and what the ones that do failed with.
 */
final class Probe {

    static final List<String> CALLS = new ArrayList<>();
    static final Map<String, Object> ANSWERS = new LinkedHashMap<>();
    static final Map<String, Throwable> FAILURES = new LinkedHashMap<>();

    private Probe() {
    }

    static void clear() {
        CALLS.clear();
        ANSWERS.clear();
        FAILURES.clear();
    }

    /**
     * Calls an accessor, and keeps what it answered or what it threw.
     */
    static void attempt(String accessor, Supplier<?> call) {
        try {
            ANSWERS.put(accessor, call.get());
        } catch (RuntimeException | Error e) {
            FAILURES.put(accessor, e);
        }
    }
}
