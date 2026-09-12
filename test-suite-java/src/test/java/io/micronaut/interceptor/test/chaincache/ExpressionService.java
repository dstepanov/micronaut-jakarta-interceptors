package io.micronaut.interceptor.test.chaincache;

/**
 * A bean whose method carries an evaluated expression in an annotation of its own, beside the binding that
 * intercepts it.
 *
 * <p>Micronaut implements the method with introduction advice, which is what makes the invocation the first place
 * the chain of this method is resolved: the chain of a business method is resolved ahead of time, as the bean is
 * created, while the chain of a method an introduction advice implements is not.</p>
 */
@Implemented
@Marked
public interface ExpressionService {

    @Noted("#{ 1 + 1 }")
    String describe(Object token);
}
