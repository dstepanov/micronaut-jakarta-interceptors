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

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.DefaultScope;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.processor.JakartaInterceptors;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Makes an interceptor class a bean.
 *
 * <p>An interceptor class of the specification is an ordinary class, which Micronaut has to know as a bean to
 * inject into it and to hand an instance of it to the advice. The scope is a default one, so that a scope the
 * interceptor class declares itself wins; without one, the specification's own rule applies and a new interceptor
 * instance is created for every object it intercepts.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class InterceptorAnnotationMapper implements NamedAnnotationMapper {

    @Override
    public String getName() {
        return JakartaInterceptors.INTERCEPTOR;
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        return List.of(
            AnnotationValue.builder(Bean.class).build(),
            AnnotationValue.builder(DefaultScope.class).value(Prototype.class).build()
        );
    }
}
