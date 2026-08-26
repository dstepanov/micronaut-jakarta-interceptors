package io.micronaut.interceptor.test.named;

import java.util.ArrayList;
import java.util.List;

public final class Calls {

    public static final List<String> RECORDED = new ArrayList<>();

    private Calls() {
    }

    public static void clear() {
        RECORDED.clear();
    }
}
