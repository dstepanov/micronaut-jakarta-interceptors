package io.micronaut.interceptor.test.destruction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** What the objects of these tests recorded as they were created and destroyed. */
public final class Destructions {

    public static final List<String> RECORDED = Collections.synchronizedList(new ArrayList<>());

    private Destructions() {
    }
}
