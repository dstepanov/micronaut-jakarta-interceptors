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

import io.micronaut.aop.ConstructorInterceptor;
import io.micronaut.aop.ConstructorInvocationContext;
import io.micronaut.aop.InterceptorBinding;
import io.micronaut.aop.InterceptorKind;
import io.micronaut.aop.InvocationContext;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanDependencyResolver;
import io.micronaut.context.InterceptionTarget;
import io.micronaut.context.Qualifier;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.beans.BeanConstructor;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.BeanType;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.internal.metadata.JakartaInterception;
import jakarta.interceptor.Interceptor;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The single Micronaut interceptor of the Jakarta Interceptors implementation.
 *
 * <p>Every element the specification intercepts is bound to this one advice, which resolves the interceptor classes
 * that apply to it and runs them as one chain. Whatever the chain proceeds into - another Micronaut interceptor, or
 * the intercepted element itself - runs after all of them, which is the order the specification asks for.</p>
 *
 * <p>The advice is created for each object it intercepts rather than shared, because the interceptor instances it
 * holds belong to that one object: an interceptor may keep state for the life of the object it intercepts. Micronaut
 * creates it as a dependent of that object, uses the same advice for the construction, the lifecycle events and the
 * business methods of the object - through a proxy with a separate target as well, whose {@code @Around} the
 * processor declares {@code lazyInterceptorsPerTarget = true} on - and destroys it with the object, after the
 * pre-destroy event. The interceptor instances go with it, as section 2.3 has them go.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Prototype
@Internal
@InterceptorBinding(value = JakartaInterception.class, kind = InterceptorKind.AROUND)
@InterceptorBinding(value = JakartaInterception.class, kind = InterceptorKind.AROUND_CONSTRUCT)
@InterceptorBinding(value = JakartaInterception.class, kind = InterceptorKind.POST_CONSTRUCT)
@InterceptorBinding(value = JakartaInterception.class, kind = InterceptorKind.PRE_DESTROY)
public final class JakartaInterceptorAdvice implements MethodInterceptor<Object, Object>, ConstructorInterceptor<Object> {

    private final InterceptorChainResolver resolver;
    /**
     * What the interceptor instances are resolved through. What Micronaut creates through it rather than finds in a
     * scope belongs to this advice, and Micronaut destroys it with the advice, latest first - also when the object
     * fails to be created. An interceptor with a scope of its own - a {@code @Singleton}, or a bean of a custom scope,
     * shared by every object it intercepts - stays in its scope.
     */
    private final BeanDependencyResolver dependencies;
    private final Map<Class<?>, Object> instances = new HashMap<>(4);
    /**
     * The business method chain of each method of the object, by the identity of the method, so that a call reads
     * neither the interception of the method nor the resolver's map. Copied on write: a method is added once, and
     * two calls adding at once only cost the one that loses resolving again on its next call. A call the scheduler
     * makes to a timeout method resolves its own chain, see {@link #intercept(MethodInvocationContext)}.
     */
    private volatile IdentityHashMap<ExecutableMethod<?, ?>, MethodChain> businessChains = new IdentityHashMap<>(0);

    /**
     * Creates the advice of one object.
     *
     * <p>Section 2.3 has an interceptor instance created for each interceptor class when the object it intercepts
     * is created, whether or not anything is ever invoked on that object. Where Micronaut hands the advice the bean it
     * is created for - the target of a proxy that resolves its interceptors for each target, which a proxy resolving
     * its target lazily first resolves on its first call, and the bean whose lifecycle is intercepted - every
     * interceptor instance that will interpose on the object is created here, from that definition, which carries the
     * qualifier of the bean: two beans of one class, two of one {@code @EachBean} among them, are told apart.
     * Micronaut hands none to the advice of a subclass it generates to intercept the methods of a class, whose
     * instances {@link InterceptorCreationListener} creates as the object is created.</p>
     *
     * @param resolver     The resolver of the interceptor chains
     * @param dependencies What the interceptor instances are resolved through, which owns those it creates
     * @param target       The bean the advice is created for, where Micronaut hands it one
     */
    public JakartaInterceptorAdvice(InterceptorChainResolver resolver, BeanDependencyResolver dependencies,
                                    @Nullable InterceptionTarget target) {
        this.resolver = resolver;
        this.dependencies = dependencies;
        if (target != null) {
            createInterceptorInstances(target.definition());
        }
    }

    @Override
    public int getOrder() {
        return Interceptor.Priority.APPLICATION;
    }

    // implementing both MethodInterceptor and ConstructorInterceptor inherits two declarations of this method,
    // of which the constructor one returns a non-null instance; an intercepted method may return null
    @SuppressWarnings("NullAway")
    @Override
    public @Nullable Object intercept(InvocationContext<Object, Object> context) {
        if (context instanceof ConstructorInvocationContext<Object> constructorContext) {
            return intercept(constructorContext);
        }
        if (context instanceof MethodInvocationContext<Object, Object> methodContext) {
            return intercept(methodContext);
        }
        throw new IllegalArgumentException("Unsupported invocation context: " + context);
    }

    @Override
    public @Nullable Object intercept(MethodInvocationContext<Object, Object> context) {
        InterceptorKind kind = context.getKind();
        if (kind == InterceptorKind.POST_CONSTRUCT || kind == InterceptorKind.PRE_DESTROY) {
            return interceptLifecycle(context, kind);
        }
        MethodChain business = businessChainOf(context, kind);
        // a method declaring @Scheduled is a timeout method only while the scheduler invokes it: a direct call by
        // the application is a business method invocation
        AnnotationValue<?> schedule = business.timeout() ? ScheduledInvocation.scheduleOf(context) : null;
        List<InterceptorReference> chain = schedule == null
            ? business.chain()
            : resolver.resolve(kind, context.getAnnotationMetadata(), true);
        if (chain.isEmpty()) {
            return context.proceed();
        }
        try {
            return new BusinessMethodInvocationContext(context, chain, this, schedule, business).proceed();
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }

    /**
     * The chain of a business method invocation of a method, resolved on its first call.
     *
     * @param context The invocation
     * @param kind    The kind Micronaut intercepts the method as
     * @return The chain, and whether the method is a timeout method when the scheduler invokes it
     */
    private MethodChain businessChainOf(MethodInvocationContext<Object, Object> context, InterceptorKind kind) {
        ExecutableMethod<?, ?> method = context.getExecutableMethod();
        MethodChain cached = businessChains.get(method);
        if (cached != null) {
            return cached;
        }
        AnnotationMetadata metadata = context.getAnnotationMetadata();
        MethodChain resolved = new MethodChain(resolver.resolve(kind, metadata, false),
            metadata.isTrue(JakartaInterception.class, JakartaInterception.TIMEOUT));
        IdentityHashMap<ExecutableMethod<?, ?>, MethodChain> added = new IdentityHashMap<>(businessChains);
        added.put(method, resolved);
        businessChains = added;
        return resolved;
    }

    /**
     * Interposes on a {@code @PostConstruct} or {@code @PreDestroy} event of the object this advice was created for.
     *
     * <p>Micronaut invokes this once for the event, and proceeding the chain runs every callback of the bean in
     * order, superclass first - which is the interception the specification describes. An interceptor that does
     * not proceed keeps all of them from running.</p>
     */
    private @Nullable Object interceptLifecycle(MethodInvocationContext<Object, Object> context, InterceptorKind kind) {
        List<InterceptorReference> chain = resolver.resolve(kind, context.getAnnotationMetadata());
        if (chain.isEmpty()) {
            context.proceed();
            return context.getTarget();
        }
        LifecycleInvocationContext invocation = new LifecycleInvocationContext(context, chain, this);
        try {
            invocation.proceed();
        } catch (Exception e) {
            throw lifecycleFailure(e);
        }
        // the chain of a lifecycle callback carries the bean itself, which an interceptor may neither replace nor
        // discard: an interceptor that does not proceed only keeps the rest of the chain from running
        return context.getTarget();
    }

    @Override
    public Object intercept(ConstructorInvocationContext<Object> context) {
        BeanConstructor<Object> constructor = context.getConstructor();
        // the annotation metadata of a constructor invocation is carried by the constructor rather than by the
        // context, which has none of its own
        List<InterceptorReference> chain =
            resolver.resolve(InterceptorKind.AROUND_CONSTRUCT, constructor.getAnnotationMetadata());
        if (chain.isEmpty()) {
            return context.proceed();
        }
        ConstructorInvocationContextAdapter invocation = new ConstructorInvocationContextAdapter(
            context, chain, this, associatedWithTheClass(constructor.getAnnotationMetadata()));
        try {
            invocation.proceed();
        } catch (Exception e) {
            // unlike a lifecycle callback, an around-construct interceptor method may throw what the constructor
            // declares, and lets through what the constructor throws. Both leave as they are, so that Micronaut
            // reports them as it reports a constructor that is not intercepted: a runtime exception of the advice
            // reaches the caller unchanged, and anything else is the cause of the failure to create the bean
            throw sneakyThrow(e);
        }
        Object constructed = invocation.constructed();
        if (constructed == null) {
            throw new IllegalStateException("An @AroundConstruct interceptor of ["
                + constructor.getDeclaringBeanType().getName() + "] returned without calling "
                + "InvocationContext.proceed(), so no instance was created");
        }
        return constructed;
    }

    /**
     * The interceptor classes the class of an object being constructed is bound to, other than those of its
     * around-construct chain.
     *
     * <p>Section 2.3 da) has injection completed on the instances of all the interceptor classes associated with the
     * target class before an {@code @AroundConstruct} method runs, so an interceptor class that declares nothing but
     * an {@code @AroundInvoke} method, and therefore takes no part in the construction, is created then too. The
     * association rules of the construction itself are untouched: what is created here interposes on nothing, and the
     * chain the construction runs is still only the one resolved for it.</p>
     *
     * <p>Read from what the processor records on the constructor apart from its chain: the interceptor classes the
     * class names, whether or not the constructor excludes them with {@code @ExcludeClassInterceptors}, and the
     * bindings in effect on it. Every kind of interceptor method counts, an {@code @AroundTimeout} one included. An
     * interceptor class bound to one method of the object alone is not among them: the methods of a bean are not reachable from a
     * constructor interception, and it is created as the object finishes being created. See the guide.</p>
     *
     * @param metadata The annotation metadata of the constructor
     * @return The interceptors, which may repeat those of the chain
     */
    private List<InterceptorReference> associatedWithTheClass(AnnotationMetadata metadata) {
        return resolver.resolveAssociated(metadata);
    }

    /**
     * Creates now every interceptor instance that will interpose on the given object.
     *
     * <p>The specification creates an interceptor instance when the object it intercepts is created, whether or
     * not anything is ever invoked on that object, so that what an interceptor class does as it is constructed
     * happens then. Resolving a chain the first time it is proceeded would instead create the instances of a
     * method's interceptors only once that method is called, and never for a method nobody calls.</p>
     *
     * <p>Called as the advice is created, from the definition Micronaut hands it of the bean it is created for, and
     * otherwise by {@link InterceptorCreationListener} as the object is created, on the advice bound to it, which is
     * the advice whose instances every interception of that object goes on to use. An instance created already is
     * not created again.</p>
     *
     * @param definition The definition of the object
     */
    void createInterceptorInstances(BeanDefinition<?> definition) {
        AnnotationMetadata classMetadata = definition.getAnnotationMetadata();
        // only what this module intercepts is asked for: an element it does not intercept has no chain, and
        // resolving one would put an empty chain in the resolver's map for nothing
        if (classMetadata.hasAnnotation(JakartaInterception.class)) {
            createInstancesOf(InterceptorKind.POST_CONSTRUCT, classMetadata);
            createInstancesOf(InterceptorKind.PRE_DESTROY, classMetadata);
        }
        for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
            AnnotationMetadata methodMetadata = method.getAnnotationMetadata();
            if (methodMetadata.hasAnnotation(JakartaInterception.class)) {
                createInstancesOf(InterceptorKind.AROUND, methodMetadata);
            }
        }
    }

    /**
     * Whether this module intercepts anything of the object the given definition creates: its lifecycle, read from
     * the class, or one of its methods.
     *
     * <p>It is the filter {@link #createInterceptorInstances(BeanDefinition)} applies element by element, asked of
     * the whole definition. Where it is {@code false} there is no interceptor instance to create, so
     * {@link InterceptorCreationListener}, which is handed every bean the context creates, need not look among the
     * dependents of the object for an advice to ask.</p>
     *
     * @param definition The definition of the object
     * @return Whether the class or one of its executable methods declares the interception of this module
     */
    static boolean intercepts(BeanDefinition<?> definition) {
        if (definition.getAnnotationMetadata().hasAnnotation(JakartaInterception.class)) {
            return true;
        }
        for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
            if (method.getAnnotationMetadata().hasAnnotation(JakartaInterception.class)) {
                return true;
            }
        }
        return false;
    }

    private void createInstancesOf(InterceptorKind kind, AnnotationMetadata metadata) {
        createInterceptorInstances(resolver.resolve(kind, metadata));
    }

    /**
     * Returns the instance of an interceptor class, creating it the first time it is asked for.
     *
     * @param reference The interceptor
     * @return The instance
     */
    synchronized Object interceptorInstance(InterceptorReference reference) {
        Class<?> interceptorClass = reference.interceptorClass();
        Object instance = instances.get(interceptorClass);
        if (instance == null) {
            instance = create(interceptorClass);
            instances.put(interceptorClass, instance);
        }
        return instance;
    }

    /**
     * Creates the instance of every interceptor class of the given references that has none yet.
     *
     * @param references The interceptors
     */
    void createInterceptorInstances(List<InterceptorReference> references) {
        for (InterceptorReference reference : references) {
            // an interceptor method the intercepted class declares itself runs on the object, and has no
            // instance of its own to create
            if (!reference.self()) {
                interceptorInstance(reference);
            }
        }
    }

    /**
     * Creates the instance of an interceptor class.
     *
     * <p>Resolved by type rather than from one definition: an interceptor class may also be produced by a factory,
     * and the instance the application configured there is the one to intercept with. The type is the class itself:
     * a subclass that is a bean too is another interceptor class, whose instance would run the interceptor methods
     * it overrides. Resolved through {@link #dependencies}, which keeps what Micronaut created rather than found in a
     * scope as a dependent of this advice, exactly as it keeps the dependencies of any bean, so nothing here has to
     * know the scopes.</p>
     *
     * <p>That holds while the context shuts down as well: an object first created then - by the destruction
     * callback of another bean - and an interceptor of a pre-destroy event first reached as its object is destroyed
     * are resolved on the thread running the shutdown or the destruction, which the resolver accepts, and what it
     * creates then is destroyed before the shutdown completes.</p>
     */
    private <T> T create(Class<T> interceptorClass) {
        return dependencies.getBean(Argument.of(interceptorClass), new ExactBeanType<>(interceptorClass));
    }

    /**
     * Reports what a lifecycle callback interceptor method threw.
     *
     * <p>The specification lets such a method throw a runtime exception but not a checked one, and there is
     * nowhere for a checked exception to go: what invoked the callback is the container, and it declares none. A
     * runtime exception travels as it is, and anything else is wrapped in one.</p>
     *
     * @param e The exception
     * @return The exception to throw
     */
    private static RuntimeException lifecycleFailure(Exception e) {
        if (e instanceof RuntimeException runtime) {
            return runtime;
        }
        return new IllegalStateException("A lifecycle callback interceptor method threw a checked exception, which "
            + "the specification does not allow it to throw: " + e, e);
    }

    /**
     * Rethrows the exception of an interceptor method as it is.
     *
     * <p>An interceptor method is declared to throw {@link Exception}, and the checked exceptions the intercepted
     * method itself declares travel through it. The intercepted method is what the caller sees, and it does declare
     * them, so the exception is rethrown unchanged rather than wrapped in something the caller cannot catch.</p>
     *
     * @param e   The exception
     * @param <E> The type the exception is rethrown as
     * @return Never returns; declared so that the call sites can be written as a {@code throw}
     * @throws E The exception
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    /**
     * The chain of the business method invocations of a method, and the bindings its invocations report, which
     * depend on nothing but the compiled metadata of the method and are read once, the first time an interceptor
     * asks for them, rather than for every invocation. Two invocations asking at once may both read them, and read
     * the same.
     */
    static final class MethodChain {

        private final List<InterceptorReference> chain;
        private final boolean timeout;
        private volatile @Nullable List<AnnotationValue<?>> bindingValues;
        private volatile @Nullable Set<Annotation> bindings;

        /**
         * Remembers the chain of a method.
         *
         * @param chain   The interceptors, in the order they are invoked in
         * @param timeout Whether the method is a timeout method when the scheduler invokes it, which then has a
         *                chain of its own
         */
        MethodChain(List<InterceptorReference> chain, boolean timeout) {
            this.chain = chain;
            this.timeout = timeout;
        }

        List<InterceptorReference> chain() {
            return chain;
        }

        boolean timeout() {
            return timeout;
        }

        List<AnnotationValue<?>> bindingValues(Supplier<List<AnnotationValue<?>>> read) {
            List<AnnotationValue<?>> values = bindingValues;
            if (values == null) {
                values = read.get();
                bindingValues = values;
            }
            return values;
        }

        Set<Annotation> bindings(Supplier<Set<Annotation>> build) {
            Set<Annotation> built = bindings;
            if (built == null) {
                built = build.get();
                bindings = built;
            }
            return built;
        }
    }

    /**
     * Qualifies the beans of a class that are of the class itself, leaving out the ones of its subclasses.
     *
     * @param type The class
     * @param <T>  The bean type
     */
    private record ExactBeanType<T>(Class<T> type) implements Qualifier<T> {

        @Override
        public <B extends BeanType<T>> Stream<B> reduce(Class<T> beanType, Stream<B> candidates) {
            // the class may be the proxy Micronaut generated for a scoped bean, which resolves the bean it
            // proxies with this same qualifier: a bean of a class the proxy extends is that one, and stays
            return candidates.filter(candidate -> InterceptorChainResolver.isOfClass(candidate, type)
                || candidate.getBeanType().isAssignableFrom(type));
        }
    }
}
