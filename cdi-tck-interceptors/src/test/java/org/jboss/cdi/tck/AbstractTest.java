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
package org.jboss.cdi.tck;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.interceptor.annotation.InterceptionKind;
import io.micronaut.interceptor.annotation.JakartaInterceptorMethods;
import io.micronaut.interceptor.runtime.InterceptorBindingValues;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs the unchanged CDI TCK tests against a Micronaut application context instead of Arquillian.
 *
 * <p>The TCK test bytecode extends a class of this name. Keeping that contract lets the tests and their assertions
 * come directly from the TCK artifact; only the container operations they use are adapted here.</p>
 */
public abstract class AbstractTest {

    private static final String INTERCEPTOR = "jakarta.interceptor.Interceptor";
    private static final String INTERCEPTOR_BINDING = "jakarta.interceptor.InterceptorBinding";

    private ApplicationContext context;
    private BeanManager beanManager;

    @BeforeClass(alwaysRun = true)
    public void startMicronaut() throws IllegalAccessException {
        context = ApplicationContext.run();
        beanManager = beanManager();
        injectTestFields();
    }

    @AfterClass(alwaysRun = true)
    public void stopMicronaut() {
        if (context != null) {
            context.close();
        }
    }

    @DataProvider(name = "ARQUILLIAN_DATA_PROVIDER")
    public Object[][] arquillianArgumentProvider(Method method) {
        Type[] parameterTypes = method.getGenericParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            arguments[i] = argument(parameterTypes[i]);
        }
        return new Object[][]{arguments};
    }

    protected BeanManager getCurrentManager() {
        return beanManager;
    }

    protected <T> T getContextualReference(Class<T> beanType, Annotation... qualifiers) {
        return context.getBean(beanType);
    }

    protected <T> Bean<T> getUniqueBean(Class<T> type, Annotation... bindings) {
        return bean(type);
    }

    protected boolean isThrowablePresent(Class<? extends Throwable> throwableType, Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (throwableType.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private Object argument(Type type) {
        if (type instanceof ParameterizedType parameterized
            && parameterized.getRawType() == Instance.class
            && parameterized.getActualTypeArguments()[0] instanceof Class<?> beanType) {
            return instance(beanType);
        }
        if (type instanceof Class<?> beanType) {
            return context.getBean(beanType);
        }
        throw new IllegalArgumentException("Unsupported TCK test argument: " + type);
    }

    private <T> Instance<T> instance(Class<T> beanType) {
        return new MicronautInstance<>(beanType);
    }

    @SuppressWarnings("unchecked")
    private <T> Bean<T> bean(Class<T> beanType) {
        return (Bean<T>) Proxy.newProxyInstance(Bean.class.getClassLoader(), new Class<?>[]{Bean.class},
            new BeanHandler(beanType));
    }

    private BeanManager beanManager() {
        return (BeanManager) Proxy.newProxyInstance(BeanManager.class.getClassLoader(),
            new Class<?>[]{BeanManager.class}, (proxy, method, args) -> switch (method.getName()) {
                case "createCreationalContext" -> creationalContext();
                case "getReference" -> referenceOf((Bean<?>) args[0]);
                case "resolveInterceptors" -> resolveInterceptors((InterceptionType) args[0], (Annotation[]) args[1]);
                case "toString" -> "Micronaut TCK BeanManager bridge";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw unsupported(method);
            });
    }

    private CreationalContext<?> creationalContext() {
        return (CreationalContext<?>) Proxy.newProxyInstance(CreationalContext.class.getClassLoader(),
            new Class<?>[]{CreationalContext.class}, (proxy, method, args) -> switch (method.getName()) {
                case "push", "release" -> null;
                default -> throw unsupported(method);
            });
    }

    private Object referenceOf(Bean<?> bean) {
        InvocationHandler handler = Proxy.getInvocationHandler(bean);
        if (handler instanceof BeanHandler beanHandler) {
            return context.createBean(beanHandler.beanType);
        }
        throw new IllegalArgumentException("Not a bean created by the Micronaut TCK bridge: " + bean);
    }

    private List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation[] annotations) {
        Set<InterceptorBindingValues.Binding> requested = new LinkedHashSet<>();
        Arrays.stream(annotations).map(AbstractTest::bindingValue).forEach(requested::add);
        List<Interceptor<?>> resolved = new ArrayList<>();
        for (BeanDefinition<?> definition : context.getAllBeanDefinitions()) {
            AnnotationMetadata metadata = definition.getAnnotationMetadata();
            if (!metadata.hasAnnotation(INTERCEPTOR) || !supports(metadata, type)) {
                continue;
            }
            Set<InterceptorBindingValues.Binding> bindings = new LinkedHashSet<>();
            for (String name : metadata.getAnnotationNamesByStereotype(INTERCEPTOR_BINDING)) {
                if (!INTERCEPTOR_BINDING.equals(name)) {
                    metadata.findAnnotation(name).map(InterceptorBindingValues::of).ifPresent(bindings::add);
                }
            }
            if (!bindings.isEmpty() && requested.containsAll(bindings)) {
                resolved.add(interceptor(definition.getBeanType()));
            }
        }
        return resolved;
    }

    private static boolean supports(AnnotationMetadata metadata, InterceptionType type) {
        AnnotationValue<JakartaInterceptorMethods> methods = metadata.getAnnotation(JakartaInterceptorMethods.class);
        if (methods == null) {
            return false;
        }
        InterceptionKind kind = switch (type) {
            case AROUND_INVOKE -> InterceptionKind.AROUND_INVOKE;
            case AROUND_TIMEOUT -> InterceptionKind.AROUND_TIMEOUT;
            case AROUND_CONSTRUCT -> InterceptionKind.AROUND_CONSTRUCT;
            case POST_CONSTRUCT -> InterceptionKind.POST_CONSTRUCT;
            case PRE_DESTROY -> InterceptionKind.PRE_DESTROY;
            default -> null;
        };
        return kind != null && methods.stringValues(kind.member()).length > 0;
    }

    private static InterceptorBindingValues.Binding bindingValue(Annotation annotation) {
        AnnotationValueBuilder<?> builder = AnnotationValue.builder(annotation.annotationType().getName());
        List<String> nonBinding = new ArrayList<>();
        Map<CharSequence, Object> values = new LinkedHashMap<>();
        for (Method member : annotation.annotationType().getDeclaredMethods()) {
            try {
                values.put(member.getName(), member.invoke(annotation));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read binding " + annotation, e);
            }
            if (member.isAnnotationPresent(jakarta.enterprise.util.Nonbinding.class)) {
                nonBinding.add(member.getName());
            }
        }
        builder.members(values);
        if (!nonBinding.isEmpty()) {
            builder.member(AnnotationUtil.NON_BINDING_ATTRIBUTE, nonBinding.toArray(String[]::new));
        }
        return InterceptorBindingValues.of(builder.build());
    }

    @SuppressWarnings("unchecked")
    private static Interceptor<?> interceptor(Class<?> beanType) {
        return (Interceptor<?>) Proxy.newProxyInstance(Interceptor.class.getClassLoader(),
            new Class<?>[]{Interceptor.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getBeanClass" -> beanType;
                case "toString" -> beanType.getName();
                case "hashCode" -> beanType.hashCode();
                case "equals" -> proxy == args[0];
                default -> throw unsupported(method);
            });
    }

    private void injectTestFields() throws IllegalAccessException {
        for (Class<?> type = getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    field.set(this, context.getBean(field.getType()));
                }
            }
        }
    }

    private static UnsupportedOperationException unsupported(Method method) {
        return new UnsupportedOperationException("The direct CDI TCK bridge does not implement " + method);
    }

    private final class BeanHandler implements InvocationHandler {

        private final Class<?> beanType;

        private BeanHandler(Class<?> beanType) {
            this.beanType = beanType;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "create" -> context.createBean(beanType);
                case "destroy" -> {
                    context.destroyBean(args[0]);
                    yield null;
                }
                case "getBeanClass" -> beanType;
                case "toString" -> "Micronaut Bean<" + beanType.getName() + ">";
                case "hashCode" -> beanType.hashCode();
                case "equals" -> proxy == args[0];
                default -> throw unsupported(method);
            };
        }
    }

    private final class MicronautInstance<T> implements Instance<T> {

        private final Class<T> beanType;

        private MicronautInstance(Class<T> beanType) {
            this.beanType = beanType;
        }

        @Override
        public T get() {
            try {
                return context.createBean(beanType);
            } catch (RuntimeException e) {
                // A failed Micronaut construction is reported as a BeanInstantiationException. CDI's Instance.get
                // exposes the exception of the constructor/interceptor itself, which is what the unchanged TCK
                // assertion catches.
                if (e.getCause() != null) {
                    return sneakyThrow(e.getCause());
                }
                throw e;
            }
        }

        @Override
        public Instance<T> select(Annotation... qualifiers) {
            return this;
        }

        @Override
        public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
            return new MicronautInstance<>(subtype);
        }

        @Override
        public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
            throw new UnsupportedOperationException("The direct CDI TCK bridge does not select by TypeLiteral");
        }

        @Override
        public boolean isUnsatisfied() {
            return false;
        }

        @Override
        public boolean isAmbiguous() {
            return false;
        }

        @Override
        public void destroy(T instance) {
            context.destroyBean(instance);
        }

        @Override
        public Handle<T> getHandle() {
            throw new UnsupportedOperationException("The direct CDI TCK bridge does not expose handles");
        }

        @Override
        public Iterable<? extends Handle<T>> handles() {
            return List.of();
        }

        @Override
        public Iterator<T> iterator() {
            return List.of(get()).iterator();
        }

        @Override
        public String toString() {
            return "Micronaut Instance<" + beanType.getName() + ">";
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T sneakyThrow(Throwable failure) throws E {
        throw (E) failure;
    }
}
