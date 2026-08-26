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

/**
 * The names of the annotations of the Jakarta Interceptors and the Jakarta Annotations specifications.
 *
 * <p>The names are used rather than the classes so that neither specification has to be on the annotation
 * processor classpath of a build that does not use it.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class JakartaInterceptors {

    /**
     * {@code jakarta.interceptor.Interceptor}, declaring an interceptor class.
     */
    public static final String INTERCEPTOR = "jakarta.interceptor.Interceptor";

    /**
     * {@code jakarta.interceptor.InterceptorBinding}, the meta-annotation of the binding annotations.
     */
    public static final String INTERCEPTOR_BINDING = "jakarta.interceptor.InterceptorBinding";

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
     * {@code jakarta.annotation.Priority}, ordering the interceptors bound through a binding annotation.
     */
    public static final String PRIORITY = "jakarta.annotation.Priority";

    /**
     * {@code io.micronaut.scheduling.annotation.Scheduled}, which declares the methods the scheduler invokes and
     * which this module reads as the timeout methods of the specification.
     */
    public static final String SCHEDULED = "io.micronaut.scheduling.annotation.Scheduled";

    /**
     * {@code io.micronaut.scheduling.annotation.Schedules}, the repeatable container of {@link #SCHEDULED}, which
     * is how a single schedule is recorded as well.
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
