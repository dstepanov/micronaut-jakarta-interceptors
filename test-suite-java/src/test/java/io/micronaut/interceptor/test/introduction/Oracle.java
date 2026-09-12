
package io.micronaut.interceptor.test.introduction;

/**
 * A bean whose methods Micronaut introduces, and which a Jakarta interceptor binds to as well.
 */
@Oracular
@Traced
public interface Oracle {

    String answer();
}
