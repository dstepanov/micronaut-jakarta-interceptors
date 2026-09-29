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
package io.micronaut.interceptor.runtime;

import io.micronaut.core.annotation.Internal;

/**
 * The names of the annotations of the Jakarta Interceptors specification the runtime reads.
 *
 * <p>The processor reads them by the same names, and depends on this module, so they are declared here once for
 * both; the ones only the processor reads are declared by {@code JakartaInterceptors} of the processor.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class JakartaInterceptorSupport {

    /**
     * {@code jakarta.interceptor.InterceptorBinding}, the meta-annotation of the binding annotations.
     */
    public static final String INTERCEPTOR_BINDING = "jakarta.interceptor.InterceptorBinding";

    /**
     * {@code jakarta.interceptor.Interceptor}, declaring an interceptor class.
     */
    public static final String INTERCEPTOR = "jakarta.interceptor.Interceptor";

    /**
     * {@code jakarta.annotation.Priority}, ordering the interceptors bound by a binding annotation.
     */
    public static final String PRIORITY = "jakarta.annotation.Priority";

    /**
     * {@code io.micronaut.scheduling.annotation.Scheduled}, which declares the methods the scheduler invokes and
     * which this module reads as the timeout methods of the specification.
     */
    public static final String SCHEDULED = "io.micronaut.scheduling.annotation.Scheduled";

    private JakartaInterceptorSupport() {
    }
}
