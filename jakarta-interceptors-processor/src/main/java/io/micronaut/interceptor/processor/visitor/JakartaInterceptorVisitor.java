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

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Executable;
import io.micronaut.context.annotation.NonBinding;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Secondary;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.annotation.InterceptionKind;
import io.micronaut.interceptor.annotation.JakartaInterception;
import io.micronaut.interceptor.annotation.JakartaInterceptorMethods;
import io.micronaut.interceptor.processor.BindingConflicts;
import io.micronaut.interceptor.processor.InterceptorClassModel;
import io.micronaut.interceptor.processor.InterceptorClassScanner;
import io.micronaut.interceptor.processor.JakartaInterceptors;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Declares, on the elements the Jakarta Interceptors specification intercepts, the interceptor bindings Micronaut
 * generates its proxies and resolves its interceptors from.
 *
 * <p>Nothing is generated: an intercepted element is annotated with the one interceptor binding of the module, and
 * with the interceptor classes it names, and an interceptor class is annotated with the names of its interceptor
 * methods and has those methods made executable. The advice then reads all of it at runtime.</p>
 *
 * <p>The order the specification prescribes is settled here, where the whole of an intercepted class is visible:
 * the interceptor classes a class names apply to each of its business methods, unless the method excludes them,
 * followed by the ones the method names itself. The interceptors bound by a binding annotation are resolved from
 * the bean context instead, since an interceptor class and the elements it intercepts need not be compiled
 * together, and the specification orders those by their priority.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class JakartaInterceptorVisitor implements TypeElementVisitor<Object, Object> {

    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of(
            JakartaInterceptors.INTERCEPTOR,
            JakartaInterceptors.INTERCEPTORS,
            JakartaInterceptors.INTERCEPTOR_BINDING,
            JakartaInterceptors.AROUND_INVOKE,
            JakartaInterceptors.AROUND_CONSTRUCT
        );
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (element.isAssignable(Annotation.class)) {
            // an annotation type is neither intercepted nor an interceptor class. A binding annotation names
            // itself among its own bindings, so leaving it out here is what keeps the interception of an element
            // from being declared on the annotation that binds it, and from there on everything that annotation
            // is declared on - the interceptor classes included
            return;
        }
        String conflict = BindingConflicts.conflictOf(element, context);
        if (conflict != null) {
            throw new ProcessingException(element, "The class [" + element.getName() + "] is bound by ["
                + conflict + "] twice, with different member values. A binding annotation declared on two of the "
                + "annotations of a class has to carry the same values in both, or there is no one binding for the "
                + "class to be matched by");
        }
        InterceptorClassModel model = InterceptorClassScanner.scan(element);
        // read before anything is declared on the class: an interceptor class is made a bean below, and whether it
        // was one to begin with is what tells a bean interposing on itself from a class written to intercept others
        boolean declaredAsABean = element.hasStereotype(Bean.class) || element.hasStereotype(AnnotationUtil.SCOPE);
        boolean isInterceptorClass = element.hasDeclaredAnnotation(JakartaInterceptors.INTERCEPTOR)
            || interposesOnAnotherObject(model);
        if (isInterceptorClass && !model.intercepts()) {
            throw new ProcessingException(element, "The interceptor class [" + element.getName() + "] declares no "
                + "interceptor method. An interceptor class declares at least one of @AroundInvoke, @AroundConstruct, "
                + "@PostConstruct or @PreDestroy, each accepting a single InvocationContext");
        }
        if (model.intercepts()) {
            declareInterceptorMethods(model, declaredAsABean);
        }
        if (isInterceptorClass) {
            // an interceptor class is not itself intercepted: its bindings say what it intercepts
            completeBindings(element, context);
            return;
        }
        intercept(element, model, declaredAsABean, context);
    }

    /**
     * Tells whether a class can only be an interceptor class.
     *
     * <p>Interposing on the construction of an object, or on its lifecycle callbacks, is something only an
     * interceptor class does; a class doing it is one, whether or not it says so with {@code @Interceptor}. What
     * is left is a class declaring nothing but {@code @AroundInvoke}, which may be either an interceptor class
     * named by {@code @Interceptors} or a class interposing on its own business methods, and which is read as the
     * latter only when the class is intercepted for some other reason as well.</p>
     */
    private static boolean interposesOnAnotherObject(InterceptorClassModel model) {
        return model.methods().keySet().stream()
            .anyMatch(kind -> kind != InterceptionKind.AROUND_INVOKE && kind != InterceptionKind.AROUND_TIMEOUT);
    }

    /**
     * Records the interceptor methods of a class and makes them executable, which is what lets the advice invoke
     * them without reflection. A lifecycle callback that interposes on another object is no longer a callback of
     * the class that declares it, so the annotation that would make Micronaut invoke it as one is taken off.
     */
    private static void declareInterceptorMethods(InterceptorClassModel model, boolean declaredAsABean) {
        ClassElement interceptorClass = model.interceptorClass();
        if (!declaredAsABean) {
            // an interceptor class named directly by @Interceptors need not be a bean of its own; it is made one,
            // so that the advice can have an instance of it and so that it may have things injected into it. The
            // definition is a secondary one, so that a factory declaring the same interceptor is what wins rather
            // than the two of them being ambiguous
            interceptorClass.annotate(Prototype.class);
            interceptorClass.annotate(Secondary.class);
        }
        interceptorClass.annotate(JakartaInterceptorMethods.class, builder -> {
            for (Map.Entry<InterceptionKind, List<MethodElement>> entry : model.methods().entrySet()) {
                builder.member(entry.getKey().member(), entry.getValue().stream()
                    .map(MethodElement::getName)
                    .toArray(String[]::new));
            }
        });
        for (Map.Entry<InterceptionKind, List<MethodElement>> entry : model.methods().entrySet()) {
            for (MethodElement method : entry.getValue()) {
                method.annotate(Executable.class);
                if (entry.getKey() == InterceptionKind.POST_CONSTRUCT) {
                    method.removeAnnotation(JakartaInterceptors.POST_CONSTRUCT);
                } else if (entry.getKey() == InterceptionKind.PRE_DESTROY) {
                    method.removeAnnotation(JakartaInterceptors.PRE_DESTROY);
                }
            }
        }
    }

    /**
     * Declares the interception of a class and of its methods.
     */
    private static void intercept(ClassElement element,
                                  InterceptorClassModel model,
                                  boolean declaredAsABean,
                                  VisitorContext context) {
        List<String> classInterceptors = namedInterceptors(element);
        MethodElement constructor = element.getPrimaryConstructor().orElse(null);
        List<String> constructorInterceptors = constructor == null ? List.of() : namedInterceptors(constructor);
        // an @AroundInvoke method interposes on the business methods of the class that declares it, but only when
        // that class is a bean of its own: a plain class declaring nothing else is an interceptor class, named by
        // @Interceptors somewhere, and intercepting it would be intercepting the interceptor
        boolean classDeclares = !classInterceptors.isEmpty()
            || !InterceptorClassScanner.bindingsOf(element).isEmpty()
            || (model.intercepts() && declaredAsABean);
        // a constructor may be bound on its own, which intercepts the construction of the bean and nothing else
        boolean constructorDeclares = constructor != null
            && (!constructorInterceptors.isEmpty() || !InterceptorClassScanner.bindingsOf(constructor).isEmpty());
        List<MethodElement> methods = element.getEnclosedElements(ElementQuery.ALL_METHODS.onlyInstance());
        if (!classDeclares && !constructorDeclares
            && methods.stream().noneMatch(JakartaInterceptorVisitor::declaresInterception)) {
            return;
        }
        completeBindings(element, context);
        if (classDeclares) {
            element.annotate(JakartaInterception.class, builder -> {
                interceptorMembers(builder, classInterceptors);
                selfMember(builder, model);
            });
        }
        // the constructor carries the interception of its own: that is where Micronaut decides whether the
        // construction of a bean is intercepted, and it also lets a constructor declare a binding, or name its own
        // interceptor classes, after the ones the class names
        if (constructor != null && (classDeclares || constructorDeclares)) {
            completeBindings(constructor, context);
            List<String> whenConstructed = new ArrayList<>();
            if (!constructor.hasDeclaredAnnotation(JakartaInterceptors.EXCLUDE_CLASS_INTERCEPTORS)) {
                whenConstructed.addAll(classInterceptors);
            }
            whenConstructed.addAll(constructorInterceptors);
            constructor.annotate(JakartaInterception.class,
                builder -> interceptorMembers(builder, whenConstructed));
        }
        for (MethodElement method : methods) {
            interceptMethod(model, method, classInterceptors, classDeclares, context);
        }
    }

    private static void interceptMethod(InterceptorClassModel model,
                                        MethodElement method,
                                        List<String> classInterceptors,
                                        boolean classDeclares,
                                        VisitorContext context) {
        if (model.isInterceptorMethod(method)) {
            // an interceptor method is not a business method, so it takes no part in the interception of the class
            method.annotate(JakartaInterception.class, builder -> builder.member("excluded", true));
            return;
        }
        if (!isBusinessMethod(method)) {
            return;
        }
        if (!classDeclares && !declaresInterception(method)) {
            return;
        }
        completeBindings(method, context);
        List<String> methodInterceptors = namedInterceptors(method);
        boolean excludesClassInterceptors = method.hasDeclaredAnnotation(JakartaInterceptors.EXCLUDE_CLASS_INTERCEPTORS);
        List<String> interceptors = new ArrayList<>(classInterceptors.size() + methodInterceptors.size());
        if (!excludesClassInterceptors) {
            interceptors.addAll(classInterceptors);
        }
        interceptors.addAll(methodInterceptors);
        // a schedule is recorded through its repeatable container even when a method declares only one
        boolean timeout = method.hasDeclaredAnnotation(JakartaInterceptors.SCHEDULED)
            || method.hasDeclaredAnnotation(JakartaInterceptors.SCHEDULES);
        if (timeout || !classDeclares || excludesClassInterceptors || !methodInterceptors.isEmpty()) {
            // the list of the method replaces the one it would otherwise inherit from the class
            method.annotate(JakartaInterception.class, builder -> {
                interceptorMembers(builder, interceptors);
                selfMember(builder, model);
                if (timeout) {
                    builder.member("timeout", true);
                }
            });
        }
    }

    /**
     * Records on every binding annotation of an element the complete list of the members excluded from the
     * binding.
     *
     * <p>Micronaut only records the excluded members that the element declares a value for, which is enough for a
     * qualifier but not here: an interceptor and the element it intercepts have to agree on which members are
     * compared, whether or not either of them declares a value for them.</p>
     */
    private static void completeBindings(Element element, VisitorContext context) {
        for (AnnotationValue<?> binding : InterceptorClassScanner.bindingsOf(element)) {
            List<String> excluded = excludedMembers(binding.getAnnotationName(), context);
            if (excluded.isEmpty()) {
                continue;
            }
            Map<CharSequence, Object> values = binding.getValues();
            element.annotate(binding.getAnnotationName(), builder -> builder
                .members(values)
                .member(AnnotationUtil.NON_BINDING_ATTRIBUTE, excluded.toArray(String[]::new)));
        }
    }

    private static List<String> excludedMembers(String annotationName, VisitorContext context) {
        ClassElement annotationType = context.getClassElement(annotationName).orElse(null);
        if (annotationType == null) {
            return List.of();
        }
        return annotationType.getEnclosedElements(ElementQuery.ALL_METHODS)
            .stream()
            .filter(member -> member.hasAnnotation(JakartaInterceptors.NONBINDING)
                || member.hasAnnotation(NonBinding.class))
            .map(MethodElement::getName)
            .toList();
    }

    /**
     * Declares the interceptor classes of an element.
     *
     * <p>The member is written even when there is none: the metadata of a method is read together with the
     * metadata of its class, member by member, so only a member the method declares itself replaces the one the
     * class declares. An empty list is how a method that excludes the interceptor classes of its class says so.</p>
     */
    private static void interceptorMembers(AnnotationValueBuilder<JakartaInterception> builder, List<String> interceptors) {
        builder.member("interceptors", interceptors.stream()
            .map(name -> new AnnotationClassValue<>(name))
            .toArray(AnnotationClassValue<?>[]::new));
    }

    private static void selfMember(AnnotationValueBuilder<JakartaInterception> builder, InterceptorClassModel model) {
        if (model.methods().containsKey(InterceptionKind.AROUND_INVOKE)
            || model.methods().containsKey(InterceptionKind.AROUND_TIMEOUT)) {
            builder.member("self", new AnnotationClassValue<>(model.interceptorClass().getName()));
        }
    }

    private static List<String> namedInterceptors(Element element) {
        AnnotationValue<?> interceptors = element.getAnnotationMetadata()
            .getDeclaredAnnotation(JakartaInterceptors.INTERCEPTORS);
        if (interceptors == null) {
            return List.of();
        }
        // a set keeps the declaration order while ignoring a class named twice, which the specification invokes once
        Set<String> names = new LinkedHashSet<>();
        for (AnnotationClassValue<?> value : interceptors.annotationClassValues(AnnotationMetadata.VALUE_MEMBER)) {
            names.add(value.getName());
        }
        return List.copyOf(names);
    }

    private static boolean declaresInterception(MethodElement method) {
        return method.hasDeclaredAnnotation(JakartaInterceptors.INTERCEPTORS)
            || !InterceptorClassScanner.bindingsOf(method).isEmpty();
    }

    /**
     * Tells whether a method is one of the business methods of a class, which are the instance methods Micronaut is
     * able to intercept, less the lifecycle callbacks, which are intercepted as callbacks rather than as methods.
     */
    private static boolean isBusinessMethod(MethodElement method) {
        return !method.isPrivate()
            && !method.isFinal()
            && !Object.class.getName().equals(method.getDeclaringType().getName())
            && !method.hasDeclaredAnnotation(JakartaInterceptors.POST_CONSTRUCT)
            && !method.hasDeclaredAnnotation(JakartaInterceptors.PRE_DESTROY);
    }
}
