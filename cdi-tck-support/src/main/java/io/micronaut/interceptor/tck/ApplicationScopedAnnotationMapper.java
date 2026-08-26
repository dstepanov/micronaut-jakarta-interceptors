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
package io.micronaut.interceptor.tck;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Reads the {@code jakarta.enterprise.context.ApplicationScoped} scope of Jakarta Contexts and Dependency Injection as a Micronaut scope, so that the beans
 * of the technology compatibility kit are beans here: an application scoped bean lives as long as the application, which is what a Micronaut singleton does.
 *
 * <p>It belongs to the harness that runs those scenarios rather than to the module: a scope is the business of the
 * specification that defines it, and Jakarta Interceptors defines none.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ApplicationScopedAnnotationMapper implements NamedAnnotationMapper {

    @Override
    public String getName() {
        return "jakarta.enterprise.context.ApplicationScoped";
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        return List.of(AnnotationValue.builder(Singleton.class).build());
    }
}
