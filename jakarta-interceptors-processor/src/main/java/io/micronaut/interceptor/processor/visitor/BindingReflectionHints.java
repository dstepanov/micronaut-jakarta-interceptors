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
package io.micronaut.interceptor.processor.visitor;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.ReflectionConfig;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.AnnotationElement;
import io.micronaut.inject.ast.AnnotationMemberElement;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.processor.InterceptorClassScanner;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Permits building the binding annotations of an intercepted class as instances inside a native image, for
 * {@link JakartaInterceptorVisitor}, where the reflection module answers the accessors that return them.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class BindingReflectionHints {

    /**
     * A type of {@code io.micronaut:micronaut-reflection}, whose presence on the classpath of the application being
     * compiled is what makes the reflective accessors of {@code InvocationContext} available to it.
     */
    private static final String REFLECTION_MODULE_TYPE = "io.micronaut.reflection.ReflectionExecutables";

    private BindingReflectionHints() {
    }

    /**
     * Whether the accessors of {@code InvocationContext} that need the reflection module can be called in the
     * application being compiled: {@code getMethod()} and the ones that return the instances of the binding
     * annotations.
     *
     * <p>They are answered by {@code io.micronaut:micronaut-reflection} and fail without it, so an application
     * compiled without that module is not given the metadata that only they need, and keeps nothing for reflection
     * that nothing can reflect on. The {@code @ReflectiveAccess} of an interceptor method generated code cannot
     * call is another matter: that call is the interception itself, and is declared either way. So is the one of
     * an intercepted constructor, which {@code getConstructor()} looks up through Micronaut Core alone.</p>
     */
    static boolean reflectiveAccessors(VisitorContext context) {
        return context.getClassElement(REFLECTION_MODULE_TYPE).isPresent();
    }

    /**
     * Permits building the binding annotations of an intercepted class, its constructor and its business methods
     * as instances.
     *
     * <p>{@code getInterceptorBindings()} and the typed accessors beside it return the binding annotations
     * themselves, which Micronaut builds as a dynamic proxy of the annotation type and
     * {@code AnnotationValueProvider}. A native image defines such a proxy only when it was told to, and
     * {@code @ReflectionConfig(accessType = DYNAMIC_PROXY)} on an annotation type declares exactly that pair.
     * Declaring it here, where the bindings are read anyway, is what spares an application from declaring it by
     * hand for every binding annotation it has.</p>
     *
     * <p>A member of a binding annotation that is an annotation itself, or an array of them, returns an instance as
     * well, built the same way, so the annotation types its members return are permitted too, and theirs in turn.
     * The members are those the type declares rather than those a binding carries: a nonbinding member is left out
     * of the binding, and the value it returns may be its default.</p>
     */
    static void permitBindingSynthesis(ClassElement element,
                                               @Nullable MethodElement constructor,
                                               List<MethodElement> methods,
                                               List<BeanInterceptionWriter.Producer> producers,
                                               VisitorContext context) {
        Set<String> bindings = new LinkedHashSet<>();
        for (AnnotationValue<?> binding : InterceptorClassScanner.bindingsOf(element)) {
            bindings.add(binding.getAnnotationName());
        }
        if (constructor != null) {
            for (AnnotationValue<?> binding : InterceptorClassScanner.bindingsOf(constructor)) {
                bindings.add(binding.getAnnotationName());
            }
        }
        for (MethodElement method : methods) {
            for (AnnotationValue<?> binding : InterceptorClassScanner.bindingsOf(method)) {
                bindings.add(binding.getAnnotationName());
            }
        }
        for (BeanInterceptionWriter.Producer producer : producers) {
            for (AnnotationValue<?> binding : InterceptorClassScanner.bindingsOf(producer.element())) {
                bindings.add(binding.getAnnotationName());
            }
        }
        List<String> pending = new ArrayList<>(bindings);
        while (!pending.isEmpty()) {
            String name = pending.remove(pending.size() - 1);
            if (!(context.getClassElement(name).orElse(null) instanceof AnnotationElement annotationType)) {
                continue;
            }
            for (AnnotationMemberElement member : annotationType.getMembers()) {
                ClassElement returned = member.getType();
                if (returned.isArray()) {
                    returned = returned.fromArray();
                }
                if (context.getClassElement(returned.getName()).orElse(null) instanceof AnnotationElement
                    && bindings.add(returned.getName())) {
                    pending.add(returned.getName());
                }
            }
        }
        for (String binding : bindings) {
            element.annotate(ReflectionConfig.class, builder -> builder
                .member("type", new AnnotationClassValue<>(binding))
                .member("accessType", TypeHint.AccessType.DYNAMIC_PROXY));
        }
    }
}
