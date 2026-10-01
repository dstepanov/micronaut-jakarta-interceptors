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
package io.micronaut.interceptor.spi;

import io.micronaut.interceptor.internal.metadata.JakartaInterceptorSupport;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.metadata.InterceptionKind;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorMethods;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The interceptor methods of one interceptor class that interpose on one kind of interception, in the order the
 * specification invokes them: the ones its superclasses declare first, its own last.
 *
 * <p>This is how the runtime reads an interceptor class, and it is public for the modules that have to read one the
 * same way without a chain being resolved - Contexts and Dependency Injection, whose bean manager hands out an
 * interceptor to be asked what it interposes on and to be invoked directly. Reading it here rather than again there
 * is what keeps the two answers the same.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public final class InterceptorMethods {

    private static final InterceptorMethods NONE = new InterceptorMethods(List.of());

    private final List<ExecutableMethod<Object, Object>> methods;

    private InterceptorMethods(List<ExecutableMethod<Object, Object>> methods) {
        this.methods = methods;
    }

    /**
     * Reads the interceptor methods of an interceptor class that interpose on a kind of interception.
     *
     * <p>They are read from what the processor recorded on the definition, and each is the executable method
     * Micronaut generated for it. Only the methods the class declares of the kind are read: that a chain has the
     * {@code @AroundInvoke} methods of a class without an {@code @AroundTimeout} method interpose on a timeout is a
     * decision of the chain, and not something the class declares.</p>
     *
     * @param definition The definition of the interceptor class, which has to be the one Micronaut generated from
     *                   the class for it to carry the executable methods
     * @param kind       The kind of interception
     * @return The interceptor methods, which are none where the class declares none of the kind
     * @throws IllegalStateException if a recorded interceptor method has no executable method
     */
    public static InterceptorMethods of(BeanDefinition<?> definition, InterceptionKind kind) {
        AnnotationValue<JakartaInterceptorMethods> recorded =
            definition.getAnnotation(JakartaInterceptorMethods.class);
        if (recorded == null) {
            return NONE;
        }
        String[] names = recorded.stringValues(kind.member());
        if (names.length == 0) {
            return NONE;
        }
        AnnotationClassValue<?>[] declaringTypes = recorded.annotationClassValues(kind.declaringTypesMember());
        List<ExecutableMethod<Object, Object>> methods = new ArrayList<>(names.length);
        for (int i = 0; i < names.length; i++) {
            String declaringType = i < declaringTypes.length ? declaringTypes[i].getName() : null;
            methods.add(interceptorMethod(definition, names[i], declaringType));
        }
        return new InterceptorMethods(List.copyOf(methods));
    }

    /**
     * The priority an interceptor class is ordered by, which the specification takes from
     * {@code jakarta.annotation.Priority}. Micronaut maps that annotation onto its own {@code @Order}, which is
     * read as a fallback so that an interceptor ordered the Micronaut way is ordered the same.
     *
     * @param definition The definition of the interceptor class
     * @return The priority, lowest first in a chain
     */
    public static int priorityOf(BeanDefinition<?> definition) {
        AnnotationMetadata metadata = definition.getAnnotationMetadata();
        OptionalInt priority = metadata.intValue(JakartaInterceptorSupport.PRIORITY, AnnotationMetadata.VALUE_MEMBER);
        if (priority.isPresent()) {
            return priority.getAsInt();
        }
        return metadata.intValue(Order.class).orElse(Interceptor.Priority.APPLICATION);
    }

    /**
     * Whether the interceptor class interposes on the kind at all.
     *
     * @return Whether there is no interceptor method
     */
    public boolean isEmpty() {
        return methods.isEmpty();
    }

    /**
     * Invokes the interceptor methods on an interceptor instance, around an invocation that is not driven by a
     * chain of this runtime.
     *
     * <p>Every method is given the same context, as the specification has the interceptor methods of one invocation
     * given: one that is the given context in everything but {@code proceed}, which leads to the next method, and
     * from the last into the given context itself. What a method or the invocation throws travels
     * on as it was thrown, checked or not, including from a method Micronaut reaches reflectively, such as a
     * private one.</p>
     *
     * @param interceptor The interceptor instance
     * @param context     The context of the invocation
     * @return Whatever the first interceptor method returned
     * @throws Exception whatever an interceptor method or the invocation threw
     */
    public @Nullable Object invoke(Object interceptor, InvocationContext context) throws Exception {
        if (methods.isEmpty()) {
            return context.proceed();
        }
        return new DirectInvocation(methods, interceptor, context).proceed();
    }

    /**
     * The executable methods, for the chains this runtime resolves.
     *
     * @return The methods, in the order they are invoked in
     */
    @Internal
    public List<ExecutableMethod<Object, Object>> methods() {
        return methods;
    }

    /**
     * The executable method of one interceptor method.
     *
     * <p>It is found by the class that declares it as well as by its name. The executable methods of a class
     * include the ones it inherits, and a class and its superclass may each declare a private interceptor method of
     * the same name and signature; looked up by name alone, both would be whichever of them came first.</p>
     *
     * @param definition    The definition of the interceptor class
     * @param name          The name of the method
     * @param declaringType The name of the class that declares it, or {@code null} where it was not recorded
     */
    @SuppressWarnings("unchecked")
    private static ExecutableMethod<Object, Object> interceptorMethod(BeanDefinition<?> definition,
                                                                     String name,
                                                                     @Nullable String declaringType) {
        for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
            Class<?>[] argumentTypes = method.getArgumentTypes();
            if (method.getMethodName().equals(name)
                && argumentTypes.length == 1
                && argumentTypes[0] == InvocationContext.class
                && (declaringType == null || method.getDeclaringType().getName().equals(declaringType))) {
                return (ExecutableMethod<Object, Object>) method;
            }
        }
        throw new IllegalStateException("The interceptor method [" + name + "] of ["
            + (declaringType == null ? definition.getBeanType().getName() : declaringType)
            + "] has no executable method. The interceptor class has to be compiled with "
            + "micronaut-jakarta-interceptors-processor on the annotation processor path");
    }

    /**
     * The context the interceptor methods of one direct invocation see: everything of the invocation, with proceed
     * leading from each method to the next, and from the last into the invocation.
     *
     * <p>It is one object for every method. Where proceeding leads is the position it keeps, which is moved on for
     * the method proceeded into and put back once that method returns or throws, so that a method proceeding a
     * second time runs the rest again.</p>
     */
    private static final class DirectInvocation implements InvocationContext {

        private final List<ExecutableMethod<Object, Object>> methods;
        private final Object interceptor;
        private final InvocationContext invocation;
        private int next;

        DirectInvocation(List<ExecutableMethod<Object, Object>> methods, Object interceptor, InvocationContext invocation) {
            this.methods = methods;
            this.interceptor = interceptor;
            this.invocation = invocation;
        }

        @Override
        public Object getTarget() {
            return invocation.getTarget();
        }

        @Override
        public Object getTimer() {
            return invocation.getTimer();
        }

        @Override
        public Method getMethod() {
            return invocation.getMethod();
        }

        @Override
        public Constructor<?> getConstructor() {
            return invocation.getConstructor();
        }

        @Override
        public Object[] getParameters() {
            return invocation.getParameters();
        }

        @Override
        public void setParameters(Object[] params) {
            invocation.setParameters(params);
        }

        @Override
        public Map<String, Object> getContextData() {
            return invocation.getContextData();
        }

        @Override
        public Set<Annotation> getInterceptorBindings() {
            return invocation.getInterceptorBindings();
        }

        @Override
        public @Nullable Object proceed() throws Exception {
            int position = next;
            if (position == methods.size()) {
                return invocation.proceed();
            }
            next = position + 1;
            try {
                return methods.get(position).invoke(interceptor, this);
            } finally {
                next = position;
            }
        }
    }
}
