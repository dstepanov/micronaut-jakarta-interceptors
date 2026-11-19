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
package io.micronaut.interceptor.internal.runtime;

import io.micronaut.core.convert.ConversionService;

import java.util.Map;

/**
 * Holds a builder registered with Micronaut for {@link BindingAnnotationAccessTest.Registered}, named as the ones
 * {@code @RegisterAnnotations} generates are, which is how the registry tells the annotation it builds.
 */
final class RegisteredBuilders {

    private RegisteredBuilders() {
    }

    @SuppressWarnings({"checkstyle:TypeName", "TypeName"})
    static final class io_micronaut_interceptor_internal_runtime_BindingAnnotationAccessTest_Registered {

        private io_micronaut_interceptor_internal_runtime_BindingAnnotationAccessTest_Registered() {
        }

        /**
         * Answers the type only: the tests that find it ask for nothing else.
         */
        public static final class AnnotationBuilder
            implements io.micronaut.core.annotation.AnnotationBuilder<BindingAnnotationAccessTest.Registered> {

            @Override
            public Class<BindingAnnotationAccessTest.Registered> annotationType() {
                return BindingAnnotationAccessTest.Registered.class;
            }

            @Override
            public BindingAnnotationAccessTest.Registered build(Map<? extends CharSequence, ?> values,
                                                               ConversionService conversionService) {
                throw new UnsupportedOperationException();
            }
        }
    }
}
