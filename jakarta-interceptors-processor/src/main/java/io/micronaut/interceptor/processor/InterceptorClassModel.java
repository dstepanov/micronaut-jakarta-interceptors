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
package io.micronaut.interceptor.processor;

import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.interceptor.annotation.InterceptionKind;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The interceptor methods a class declares, by the kind of interception each of them interposes on.
 *
 * <p>There may be more than one of a kind: a class declares at most one, but the classes it inherits from declare
 * their own, and the specification invokes all of them, the most general superclass first. The methods of each
 * kind are held in that order.</p>
 *
 * @param interceptorClass The class
 * @param methods          The interceptor methods of each kind, most general superclass first
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public record InterceptorClassModel(ClassElement interceptorClass,
                                    Map<InterceptionKind, List<MethodElement>> methods) {

    public InterceptorClassModel {
        methods = methods.isEmpty() ? Map.of() : new EnumMap<>(methods);
    }

    /**
     * Tells whether the class intercepts anything at all, which it does as soon as it declares one interceptor
     * method.
     *
     * @return Whether the class declares an interceptor method
     */
    public boolean intercepts() {
        return !methods.isEmpty();
    }

    /**
     * Tells whether a method is one of the interceptor methods of the class.
     *
     * @param method The method
     * @return Whether it interposes on something
     */
    public boolean isInterceptorMethod(MethodElement method) {
        return methods.values().stream().anyMatch(methodsOfAKind -> methodsOfAKind.contains(method));
    }
}
