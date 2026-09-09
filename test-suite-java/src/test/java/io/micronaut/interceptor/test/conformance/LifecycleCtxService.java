package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.annotation.Prototype;

/** A prototype, so that a second instance is a second set of lifecycle events. */
@Prototype
@LifecycleCtx
public class LifecycleCtxService {
}
