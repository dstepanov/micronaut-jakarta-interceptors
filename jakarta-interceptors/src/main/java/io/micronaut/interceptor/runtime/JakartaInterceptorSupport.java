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
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class JakartaInterceptorSupport {

    /**
     * {@code jakarta.interceptor.InterceptorBinding}, the meta-annotation of the binding annotations.
     */
    static final String INTERCEPTOR_BINDING = "jakarta.interceptor.InterceptorBinding";

    /**
     * {@code jakarta.interceptor.Interceptor}, declaring an interceptor class.
     */
    static final String INTERCEPTOR = "jakarta.interceptor.Interceptor";

    /**
     * {@code jakarta.annotation.Priority}, ordering the interceptors bound by a binding annotation.
     */
    static final String PRIORITY = "jakarta.annotation.Priority";

    /**
     * {@code io.micronaut.scheduling.annotation.Scheduled}, which declares the methods the scheduler invokes and
     * which this module reads as the timeout methods of the specification.
     */
    static final String SCHEDULED = "io.micronaut.scheduling.annotation.Scheduled";

    private JakartaInterceptorSupport() {
    }
}
