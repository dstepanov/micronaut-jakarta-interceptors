package io.micronaut.interceptor.test.introduction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Calls {

    public static final List<String> RECORDED = new CopyOnWriteArrayList<>();

    private Calls() {
    }
}
