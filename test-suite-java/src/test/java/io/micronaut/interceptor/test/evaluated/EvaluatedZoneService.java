package io.micronaut.interceptor.test.evaluated;

import jakarta.inject.Singleton;

/**
 * A bean whose method declares the binding of its class again, leaving its member to the default, beside an
 * unrelated annotation containing an evaluated expression.
 */
@Singleton
@Zone("class")
public class EvaluatedZoneService {

    @Zone
    @Noted("#{ 1 + 1 }")
    public String replacedByDefault() {
        return "by default";
    }

    public String inherited() {
        return "inherited";
    }
}
