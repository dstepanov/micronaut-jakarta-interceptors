package io.micronaut.interceptor.test.defaults;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Log {

    public static final List<String> RECORDED = new CopyOnWriteArrayList<>();

    private Log() {
    }
}
