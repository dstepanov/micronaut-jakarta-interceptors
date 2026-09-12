package io.micronaut.interceptor.test.evaluated;

import java.util.ArrayList;
import java.util.List;

/** What the interceptors of this package recorded. */
public final class Calls {

    public static final List<String> RECORDED = new ArrayList<>();

    private Calls() {
    }
}
