/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.interceptor.processor;

import io.micronaut.core.annotation.Internal;
import io.micronaut.interceptor.runtime.JakartaInterceptorSupport;

/**
 * The names of the annotations of the Jakarta Interceptors and the Jakarta Annotations specifications.
 *
 * <p>The names are used rather than the classes so that neither specification has to be on the annotation
 * processor classpath of a build that does not use it. The names the runtime reads as well are declared once, by
 * {@link JakartaInterceptorSupport}.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class JakartaInterceptors {

    /**
     * {@code jakarta.interceptor.Interceptors}, naming interceptor classes directly.
     */
    public static final String INTERCEPTORS = "jakarta.interceptor.Interceptors";

    /**
     * {@code jakarta.interceptor.AroundInvoke}, the business method interceptor method.
     */
    public static final String AROUND_INVOKE = "jakarta.interceptor.AroundInvoke";

    /**
     * {@code jakarta.interceptor.AroundConstruct}, the constructor interceptor method.
     */
    public static final String AROUND_CONSTRUCT = "jakarta.interceptor.AroundConstruct";

    /**
     * {@code jakarta.interceptor.AroundTimeout}, the timeout method interceptor method.
     */
    public static final String AROUND_TIMEOUT = "jakarta.interceptor.AroundTimeout";

    /**
     * {@code jakarta.interceptor.ExcludeClassInterceptors}.
     */
    public static final String EXCLUDE_CLASS_INTERCEPTORS = "jakarta.interceptor.ExcludeClassInterceptors";

    /**
     * {@code jakarta.interceptor.ExcludeDefaultInterceptors}.
     */
    public static final String EXCLUDE_DEFAULT_INTERCEPTORS = "jakarta.interceptor.ExcludeDefaultInterceptors";

    /**
     * {@code jakarta.annotation.PostConstruct}.
     */
    public static final String POST_CONSTRUCT = "jakarta.annotation.PostConstruct";

    /**
     * {@code jakarta.annotation.PreDestroy}.
     */
    public static final String PRE_DESTROY = "jakarta.annotation.PreDestroy";

    /**
     * {@code io.micronaut.scheduling.annotation.Schedules}, the repeatable container of
     * {@link JakartaInterceptorSupport#SCHEDULED}, which is how a single schedule is recorded as well.
     */
    public static final String SCHEDULES = "io.micronaut.scheduling.annotation.Schedules";

    /**
     * {@code jakarta.enterprise.util.Nonbinding}, excluding a member of a binding annotation from the binding.
     */
    public static final String NONBINDING = "jakarta.enterprise.util.Nonbinding";

    /**
     * {@code jakarta.interceptor.InvocationContext}, the single parameter of every interceptor method.
     */
    public static final String INVOCATION_CONTEXT = "jakarta.interceptor.InvocationContext";

    private JakartaInterceptors() {
    }
}
