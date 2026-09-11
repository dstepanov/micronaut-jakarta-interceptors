package io.micronaut.interceptor.test.construct;

import io.micronaut.context.annotation.Prototype;

import java.io.IOException;

/**
 * A bean whose constructor declares a checked exception, and throws it when the test asks it to.
 */
@Prototype
@FragileBuilt
public class FragileBuiltService {

    static volatile IOException failure;

    public FragileBuiltService() throws IOException {
        if (failure != null) {
            throw failure;
        }
    }
}
