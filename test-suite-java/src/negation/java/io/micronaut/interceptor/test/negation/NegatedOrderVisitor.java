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
package io.micronaut.interceptor.test.negation;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Order;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.Set;

/**
 * Rewrites the {@code @Order} of a class into its {@code jakarta.annotation.Priority} negated, after Micronaut mapped
 * the one onto the other, as Micronaut CDI does for a selected alternative, an alternative of a higher priority
 * winning where an {@code @Order} of a lower one does. It acts on the classes of the tests that ask for it alone.
 */
public final class NegatedOrderVisitor implements TypeElementVisitor<Object, Object> {

    private static final String MARKER = "io.micronaut.interceptor.test.ordering.NegatesItsPriority";
    private static final String PRIORITY = "jakarta.annotation.Priority";

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of(MARKER);
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!element.hasDeclaredAnnotation(MARKER)) {
            return;
        }
        int order = -element.intValue(PRIORITY, AnnotationMetadata.VALUE_MEMBER).orElseThrow();
        element.removeAnnotation(Order.class);
        element.annotate(Order.class, builder -> builder.value(order));
    }

    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }
}
