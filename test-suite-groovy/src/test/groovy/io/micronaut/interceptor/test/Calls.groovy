package io.micronaut.interceptor.test

final class Calls {

    static final List<String> RECORDED = []

    private Calls() {
    }

    static void clear() {
        RECORDED.clear()
    }
}
