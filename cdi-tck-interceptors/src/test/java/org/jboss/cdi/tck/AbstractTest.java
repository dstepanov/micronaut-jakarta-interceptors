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

import io.micronaut.annotation.processing.test.JavaParser;
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

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Runs the unchanged CDI TCK tests against a Micronaut application context instead of Arquillian.
 *
 * <p>The TCK test bytecode extends a class of this name. Keeping that contract lets the tests and their assertions
 * come directly from the TCK artifact; only the container operations they use are adapted here.</p>
 */
public abstract class AbstractTest {

    private static final String INTERCEPTOR = "jakarta.interceptor.Interceptor";
    private static final String INTERCEPTOR_BINDING = "jakarta.interceptor.InterceptorBinding";
    private static final String ARQUILLIAN_DEPLOYMENT = "org.jboss.arquillian.container.test.api.Deployment";
    private static final String SHOULD_THROW_EXCEPTION =
        "org.jboss.arquillian.container.test.api.ShouldThrowException";
    private static final Map<String, String> EXPECTED_DEPLOYMENT_DIAGNOSTICS = Map.of(
        "org.jboss.cdi.tck.interceptors.tests.bindings.broken.InvalidTransitiveInterceptorBindingAnnotationsTest",
        "BazBinding",
        "org.jboss.cdi.tck.interceptors.tests.bindings.broken.InvalidStereotypeInterceptorBindingAnnotationsTest",
        "BazBinding"
    );

    private ApplicationContext context;
    private BeanManager beanManager;

    @BeforeClass(alwaysRun = true)
    public void startMicronaut() throws ReflectiveOperationException {
        if (verifyExpectedDeploymentFailure()) {
            return;
        }
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

    /**
     * Replaces Arquillian's deployment phase for tests whose entire assertion is that deployment fails.
     *
     * <p>The original test method is empty: Arquillian normally builds the archive returned by the method annotated
     * with Arquillian's {@code @Deployment} and satisfies {@code @ShouldThrowException} when the container rejects
     * it. This bridge reads that unchanged archive, obtains the corresponding source resources and hands them to
     * Micronaut's annotation processor instead.</p>
     */
    private boolean verifyExpectedDeploymentFailure() throws ReflectiveOperationException {
        for (Method deployment : getClass().getDeclaredMethods()) {
            Annotation expected = annotation(deployment, SHOULD_THROW_EXCEPTION);
            if (annotation(deployment, ARQUILLIAN_DEPLOYMENT) == null || expected == null) {
                continue;
            }
            Object archive;
            try {
                archive = deployment.invoke(null);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Cannot create TCK deployment archive", e.getCause());
            }
            List<JavaFileObject> sources = deploymentSources(archive);
            if (sources.isEmpty()) {
                throw new AssertionError("The TCK deployment archive contains no source classes");
            }
            String expectedDiagnostic = EXPECTED_DEPLOYMENT_DIAGNOSTICS.get(getClass().getName());
            if (expectedDiagnostic == null) {
                throw new AssertionError("No expected deployment diagnostic is recorded for " + getClass().getName());
            }
            try (JavaParser parser = new JavaParser()) {
                try {
                    parser.generate(sources.toArray(JavaFileObject[]::new));
                } catch (RuntimeException expectedFailure) {
                    if (!failureMentions(expectedFailure, expectedDiagnostic)) {
                        throw new AssertionError(
                            "Deployment failed without the expected diagnostic [" + expectedDiagnostic + "]",
                            expectedFailure
                        );
                    }
                    return true;
                }
            }
            Class<?> expectedType = (Class<?>) expected.annotationType().getMethod("value").invoke(expected);
            throw new AssertionError("Expected deployment to throw " + expectedType.getName());
        }
        return false;
    }

    private static boolean failureMentions(Throwable failure, String expected) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private static Annotation annotation(Method method, String annotationName) {
        return Arrays.stream(method.getAnnotations())
            .filter(annotation -> annotation.annotationType().getName().equals(annotationName))
            .findFirst()
            .orElse(null);
    }

    private static List<JavaFileObject> deploymentSources(Object archive) throws ReflectiveOperationException {
        Map<String, JavaFileObject> sources = new TreeMap<>();
        Class<?> archiveType = Class.forName("org.jboss.shrinkwrap.api.Archive");
        Class<?> archivePathType = Class.forName("org.jboss.shrinkwrap.api.ArchivePath");
        Map<?, ?> content = (Map<?, ?>) archiveType.getMethod("getContent").invoke(archive);
        Method getPath = archivePathType.getMethod("get");
        for (Object archivePath : content.keySet()) {
            String path = (String) getPath.invoke(archivePath);
            int classes = path.indexOf("/classes/");
            if (classes < 0 || !path.endsWith(".class")) {
                continue;
            }
            String className = path.substring(classes + "/classes/".length(), path.length() - ".class".length());
            int nested = className.indexOf('$');
            if (nested >= 0) {
                className = className.substring(0, nested);
            }
            String sourcePath = "/" + className + ".java";
            JavaFileObject source = sourceOf(sourcePath);
            if (source != null) {
                sources.putIfAbsent(sourcePath, source);
            }
        }
        return List.copyOf(sources.values());
    }

    private static JavaFileObject sourceOf(String path) {
        try (InputStream input = AbstractTest.class.getResourceAsStream(path)) {
            if (input == null) {
                // The archive also contains the TCK test class; only its deployment classes are source resources.
                return null;
            }
            String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return new SimpleJavaFileObject(URI.create("string://" + path), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return text;
                }
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
