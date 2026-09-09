package io.micronaut.interceptor.test.lifecycle;

import java.util.ArrayList;
import java.util.List;

/** What the halted lifecycle event recorded. */
public final class Calls {

    public static final List<String> RECORDED = new ArrayList<>();

    private Calls() {
    }
}
