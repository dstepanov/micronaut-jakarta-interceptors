package io.micronaut.interceptor.test.timeout;

import jakarta.inject.Singleton;

/** A bean an around-timeout interceptor is given, and calls as it interposes. */
@Singleton
public class TimeoutCollaborator {

    public void consulted() {
        VariedCalls.RECORDED.add("collaborator");
    }
}
