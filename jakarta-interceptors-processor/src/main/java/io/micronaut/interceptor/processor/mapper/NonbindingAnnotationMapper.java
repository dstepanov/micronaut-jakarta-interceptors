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
package io.micronaut.interceptor.processor.mapper;

import io.micronaut.context.annotation.NonBinding;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.processor.JakartaInterceptors;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Maps {@code jakarta.enterprise.util.Nonbinding} onto the Micronaut annotation of the same meaning, so that a
 * member excluded from an interceptor binding is recorded the way Micronaut records it and left out of the
 * comparison of the bindings.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class NonbindingAnnotationMapper implements NamedAnnotationMapper {

    @Override
    public String getName() {
        return JakartaInterceptors.NONBINDING;
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        return List.of(AnnotationValue.builder(NonBinding.class).build());
    }
}
