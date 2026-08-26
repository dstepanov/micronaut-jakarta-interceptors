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

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.List;
import java.util.Set;

/**
 * Reads a producer method of Jakarta Contexts and Dependency Injection as the Micronaut factory method of the same
 * meaning, so that a scenario of the technology compatibility kit which produces one of its beans that way can be
 * deployed here.
 *
 * <p>It belongs to the harness that runs those scenarios rather than to the module: a producer method is the
 * business of the specification that defines it, and Jakarta Interceptors defines none.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ProducesVisitor implements TypeElementVisitor<Object, Object> {

    private static final String PRODUCES = "jakarta.enterprise.inject.Produces";

    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of(PRODUCES);
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        List<MethodElement> producers = element.getEnclosedElements(ElementQuery.ALL_METHODS)
            .stream()
            .filter(method -> method.hasDeclaredAnnotation(PRODUCES))
            .toList();
        if (producers.isEmpty()) {
            return;
        }
        element.annotate(Factory.class);
        for (MethodElement producer : producers) {
            producer.annotate(Bean.class);
            // a producer method is dependent scoped unless it says otherwise, as a Micronaut prototype is
            producer.annotate(Prototype.class);
        }
    }
}
