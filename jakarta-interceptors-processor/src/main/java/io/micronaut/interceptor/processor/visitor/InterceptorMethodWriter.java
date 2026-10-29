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

import io.micronaut.context.annotation.Executable;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Secondary;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.Indexed;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.internal.metadata.BindingValue;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorIndex;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorMethods;
import io.micronaut.interceptor.metadata.InterceptionKind;
import io.micronaut.interceptor.processor.InterceptorBindingValues;
import io.micronaut.interceptor.processor.InterceptorClassModel;
import io.micronaut.interceptor.processor.JakartaInterceptors;

import java.util.List;
import java.util.Map;

/**
 * Records the interceptor methods of a class on it, and indexes a bound interceptor class, for
 * {@link JakartaInterceptorVisitor}.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class InterceptorMethodWriter {

    private InterceptorMethodWriter() {
    }

    /**
     * Records the interceptor methods of a class and makes them executable, which is what lets the advice invoke
     * them without reflection. A lifecycle callback that interposes on another object is no longer a callback of
     * the class that declares it, so the annotation that would make Micronaut invoke it as one is taken off.
     */
    static void declareInterceptorMethods(InterceptorClassModel model,
                                                 boolean declaredAsABean,
                                                 boolean isInterceptorClass,
                                                 VisitorContext context) {
        ClassElement interceptorClass = model.interceptorClass();
        if (!declaredAsABean) {
            // an interceptor class need not be a bean of its own, whether it declares @Interceptor or is named
            // directly by @Interceptors; it is made one, so that the advice can have an instance of it and so that
            // it may have things injected into it. The definition is a secondary one, so that a factory declaring
            // the same interceptor is what wins rather than the two of them being ambiguous
            interceptorClass.annotate(Prototype.class);
            interceptorClass.annotate(Secondary.class);
        }
        // an interceptor class is not itself intercepted: its bindings say what it intercepts
        List<BindingValue> bindings =
            isInterceptorClass ? InterceptorBindingValues.bindingsOf(interceptorClass, null, context) : List.of();
        interceptorClass.annotate(JakartaInterceptorMethods.class, builder -> {
            for (Map.Entry<InterceptionKind, List<MethodElement>> entry : model.methods().entrySet()) {
                builder.member(entry.getKey().member(), entry.getValue().stream()
                    .map(MethodElement::getName)
                    .toArray(String[]::new));
                // a name is not enough to find the method by: a class and its superclass may each declare a private
                // interceptor method of the same name and signature, and the specification invokes both
                builder.member(entry.getKey().declaringTypesMember(), entry.getValue().stream()
                    .map(method -> new AnnotationClassValue<>(method.getDeclaringType().getName()))
                    .toArray(AnnotationClassValue<?>[]::new));
            }
            if (!bindings.isEmpty()) {
                builder.member(JakartaInterceptorMethods.BINDINGS, InterceptorBindingValues.written(bindings));
            }
        });
        for (Map.Entry<InterceptionKind, List<MethodElement>> entry : model.methods().entrySet()) {
            for (MethodElement method : entry.getValue()) {
                method.annotate(Executable.class);
                if (method.isReflectionRequired(interceptorClass)) {
                    // the specification allows an interceptor method any access level. Micronaut generates the
                    // executable method beside the interceptor class, which cannot reach a private method, nor a
                    // protected or package private one declared by a superclass in another package; it reaches
                    // those reflectively once it is told that reflection is permitted
                    method.annotate(ReflectiveAccess.class);
                }
                if (entry.getKey() == InterceptionKind.POST_CONSTRUCT) {
                    method.removeAnnotation(JakartaInterceptors.POST_CONSTRUCT);
                } else if (entry.getKey() == InterceptionKind.PRE_DESTROY) {
                    method.removeAnnotation(JakartaInterceptors.PRE_DESTROY);
                }
            }
        }
    }

    /**
     * Puts a bound interceptor class into the bean index of the context, so that the runtime finds the interceptor
     * classes of an application without reading every bean definition there is.
     *
     * <p>An interceptor class of the specification implements nothing, and it does not have to: a bean is
     * enumerable by every type it is indexed by, whether or not it is one.</p>
     */
    static void index(InterceptorClassModel model) {
        model.interceptorClass().annotate(Indexed.class, builder -> builder.value(JakartaInterceptorIndex.class));
    }
}
