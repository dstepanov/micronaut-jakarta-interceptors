package io.micronaut.interceptor.test.variations;

import jakarta.interceptor.Interceptor;

/**
 * An interceptor class whose interceptor method is inherited from its superclass, which the specification allows.
 */
@Interceptor
@Inheriting
public class InheritedMethodInterceptor extends BaseInterceptor {
}
