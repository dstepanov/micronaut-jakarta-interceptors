package io.micronaut.interceptor.test.binding;

import java.util.ArrayList;
import java.util.List;

final class TypedLifecycleCalls {

    static final List<String> RECORDED = new ArrayList<>();

    private TypedLifecycleCalls() {
    }
}
