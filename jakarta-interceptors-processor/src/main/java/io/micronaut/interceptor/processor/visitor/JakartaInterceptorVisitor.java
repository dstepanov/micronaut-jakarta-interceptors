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

import io.micronaut.aop.Adapter;
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
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.annotation.InterceptionKind;
import io.micronaut.interceptor.annotation.JakartaInterception;
import io.micronaut.interceptor.annotation.JakartaInterceptorIndex;
import io.micronaut.interceptor.annotation.JakartaInterceptorMethods;
import io.micronaut.interceptor.annotation.JakartaVoidInterceptorIndex;
import io.micronaut.interceptor.processor.BindingConflicts;
import io.micronaut.interceptor.processor.InterceptorBindingValues;
import io.micronaut.interceptor.processor.InterceptorClassModel;
import io.micronaut.interceptor.processor.InterceptorClassScanner;
import io.micronaut.interceptor.processor.JakartaInterceptors;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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
        if (isInterceptorClass) {
            // an interceptor class is not itself intercepted: its bindings say what it intercepts. They are
            // completed before anything is written out, because what a binding is compared by depends on the
            // members excluded from it
            completeBindings(element, context);
        }
        if (model.intercepts()) {
            declareInterceptorMethods(model, declaredAsABean, isInterceptorClass);
        }
        if (isInterceptorClass) {
            if (!InterceptorClassScanner.bindingsOf(element).isEmpty()) {
                // only an interceptor class a binding annotation binds is looked for among the beans; one that is
                // named directly is found by the class the element names, and needs no index
                index(model);
            }
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
    private static void declareInterceptorMethods(InterceptorClassModel model,
                                                 boolean declaredAsABean,
                                                 boolean isInterceptorClass) {
        ClassElement interceptorClass = model.interceptorClass();
        if (!declaredAsABean) {
            // an interceptor class named directly by @Interceptors need not be a bean of its own; it is made one,
            // so that the advice can have an instance of it and so that it may have things injected into it. The
            // definition is a secondary one, so that a factory declaring the same interceptor is what wins rather
            // than the two of them being ambiguous
            interceptorClass.annotate(Prototype.class);
            interceptorClass.annotate(Secondary.class);
        }
        String[] bindings = isInterceptorClass ? bindingsOf(interceptorClass, null) : new String[0];
        interceptorClass.annotate(JakartaInterceptorMethods.class, builder -> {
            for (Map.Entry<InterceptionKind, List<MethodElement>> entry : model.methods().entrySet()) {
                builder.member(entry.getKey().member(), entry.getValue().stream()
                    .map(MethodElement::getName)
                    .toArray(String[]::new));
            }
            if (bindings.length > 0) {
                builder.member("bindings", bindings);
            }
        });
        for (Map.Entry<InterceptionKind, List<MethodElement>> entry : model.methods().entrySet()) {
            for (MethodElement method : entry.getValue()) {
                method.annotate(Executable.class);
                if (method.isPrivate()) {
                    // the specification allows a private interceptor method. Micronaut generates an executable
                    // method that reaches it reflectively once it is told that reflection is permitted
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
     * <p>An index holds the definitions of a type, and an interceptor class of the specification implements
     * nothing. One of its interceptor methods is adapted to the type the runtime asks for instead, which gives
     * the class a bean definition of that type; the definition carries the metadata of the interceptor class,
     * which is all the runtime reads. Nothing invokes the adapted method: what it produces is a definition.</p>
     */
    private static void index(InterceptorClassModel model) {
        MethodElement representative = null;
        for (List<MethodElement> methods : model.methods().values()) {
            for (MethodElement method : methods) {
                // a method that returns the result of what it interposes on is preferred, because a method
                // returning nothing cannot be adapted to one that returns something
                if (!method.getReturnType().isVoid()) {
                    representative = method;
                    break;
                }
                if (representative == null) {
                    representative = method;
                }
            }
            if (representative != null && !representative.getReturnType().isVoid()) {
                break;
            }
        }
        if (representative == null) {
            return;
        }
        Class<?> index = representative.getReturnType().isVoid()
            ? JakartaVoidInterceptorIndex.class : JakartaInterceptorIndex.class;
        representative.annotate(Adapter.class, builder -> builder.value(index));
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
        // a binding that disagrees with itself is a definition error wherever it is declared, so the members that
        // can be bound are checked as the class was, before anything is read from their bindings
        if (constructor != null) {
            rejectConflictingBindings(constructor, "constructor", context);
        }
        for (MethodElement method : methods) {
            if (!model.isInterceptorMethod(method) && isBusinessMethod(method)) {
                rejectConflictingBindings(method, "method", context);
            }
        }
        if (!classDeclares && !constructorDeclares
            && methods.stream().noneMatch(JakartaInterceptorVisitor::declaresInterception)) {
            return;
        }
        completeBindings(element, context);
        String[] classBindings = bindingsOf(element, null);
        if (classDeclares) {
            element.annotate(JakartaInterception.class, builder -> {
                interceptorMembers(builder, classInterceptors);
                selfMember(builder, model);
                bindingsMember(builder, classBindings);
                callbackMembers(builder, element, model);
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
            String[] constructorBindings = bindingsOf(constructor, element);
            constructor.annotate(JakartaInterception.class, builder -> {
                interceptorMembers(builder, whenConstructed);
                bindingsMember(builder, constructorBindings);
            });
        }
        for (MethodElement method : methods) {
            interceptMethod(model, method, classInterceptors, classBindings, classDeclares, context);
        }
    }

    /**
     * Reports a binding annotation that reaches a method or a constructor along two paths, carrying different
     * member values, which the specification makes a definition error there as it does on a class.
     */
    private static void rejectConflictingBindings(MethodElement member, String noun, VisitorContext context) {
        String conflict = BindingConflicts.declaredConflictOf(member, context);
        if (conflict != null) {
            throw new ProcessingException(member, "The " + noun + " [" + member.getDeclaringType().getName() + "."
                + member.getName() + "] is bound by [" + conflict + "] twice, with different member values. A "
                + "binding annotation declared on two of the annotations of a " + noun + " has to carry the same "
                + "values in both, or there is no one binding for the " + noun + " to be matched by");
        }
    }

    private static void interceptMethod(InterceptorClassModel model,
                                        MethodElement method,
                                        List<String> classInterceptors,
                                        String[] classBindings,
                                        boolean classDeclares,
                                        VisitorContext context) {
        if (model.isInterceptorMethod(method)) {
            // an interceptor method is not a business method, so it takes no part in the interception of the
            // class. A private method is never intercepted to begin with, and marking one would leave advice
            // metadata on a method Micronaut cannot override, which it reports as an error
            if (!method.isPrivate()) {
                method.annotate(JakartaInterception.class, builder -> builder.member("excluded", true));
            }
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
        String[] methodBindings = bindingsOf(method, null);
        // a binding the method declares replaces the one of the class, so the method carries a declaration of its
        // own as soon as what it is bound by differs from what its class is bound by
        boolean replacesBindings = !Arrays.equals(classBindings, methodBindings);
        if (timeout || replacesBindings || !classDeclares || excludesClassInterceptors || !methodInterceptors.isEmpty()) {
            // the list of the method replaces the one it would otherwise inherit from the class
            method.annotate(JakartaInterception.class, builder -> {
                interceptorMembers(builder, interceptors);
                selfMember(builder, model);
                bindingsMember(builder, methodBindings);
                if (timeout) {
                    builder.member("timeout", true);
                }
            });
        }
    }

    /**
     * Writes out what the bindings of an element are compared by.
     *
     * <p>Comparing an interceptor with an element it might intercept is comparing the members of their binding
     * annotations, the ones they default to filled in and the ones excluded from the binding left out. None of
     * that depends on the running application, so it is worked out here and the runtime compares strings.</p>
     *
     * <p>The metadata of a member is read together with the metadata of its class, and a binding the member
     * declares replaces the one of the class, so what is read here is already the set in effect on the element.
     * A binding declared on another annotation is one of the element's as well, which is why the bindings are
     * looked for by their stereotype rather than among the annotations the element declares itself.</p>
     *
     * <p>A constructor is the one element whose metadata does not carry the bindings of the class it belongs to,
     * so the class is read first and what the constructor declares is written over it. Reading the class as well
     * is harmless for a member that does carry them: the same binding read twice is the same binding.</p>
     *
     * @param element The element
     * @param owner   The class the element belongs to, or {@code null} when the element is the class
     * @return The bindings, as strings, in a stable order
     */
    private static String[] bindingsOf(Element element, @Nullable ClassElement owner) {
        Map<String, InterceptorBindingValues.Binding> bindings = new LinkedHashMap<>();
        if (owner != null) {
            for (InterceptorBindingValues.Binding binding : InterceptorBindingValues.of(owner.getAnnotationMetadata())) {
                bindings.put(binding.name(), binding);
            }
        }
        for (InterceptorBindingValues.Binding binding : InterceptorBindingValues.of(element.getAnnotationMetadata())) {
            bindings.put(binding.name(), binding);
        }
        return bindings.values()
            .stream()
            .map(InterceptorBindingValues.Binding::canonical)
            .sorted()
            .toArray(String[]::new);
    }

    private static void bindingsMember(AnnotationValueBuilder<JakartaInterception> builder, String[] bindings) {
        if (bindings.length > 0) {
            builder.member("bindings", bindings);
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

    /**
     * Records the lifecycle callbacks of the intercepted class itself.
     *
     * <p>The specification hands a {@code @PostConstruct} or {@code @PreDestroy} interceptor method the callback of
     * the class it is interposing on, and only {@code null} when the class has none. Micronaut intercepts the
     * lifecycle of a bean rather than one callback of it, so the callback is not something the interception itself
     * carries; it is read here, where the class is being looked at anyway, and the runtime is left with one lookup
     * to do when an interceptor asks for it.</p>
     */
    private static void callbackMembers(AnnotationValueBuilder<JakartaInterception> builder,
                                        ClassElement element,
                                        InterceptorClassModel model) {
        MethodElement postConstruct = callbackOf(element, model, JakartaInterceptors.POST_CONSTRUCT);
        if (postConstruct != null) {
            builder.member("postConstruct", postConstruct.getName());
        }
        MethodElement preDestroy = callbackOf(element, model, JakartaInterceptors.PRE_DESTROY);
        if (preDestroy != null) {
            builder.member("preDestroy", preDestroy.getName());
        }
    }

    /**
     * The name of the lifecycle callback of a class, the most specific one when the class and its superclasses
     * each declare one: the interception happens once around all of them, and the callback of the class itself is
     * the one the specification describes.
     */
    private static @Nullable MethodElement callbackOf(ClassElement element,
                                                      InterceptorClassModel model,
                                                      String annotation) {
        List<String> hierarchy = new ArrayList<>();
        for (ClassElement type = element; type != null; type = type.getSuperType().orElse(null)) {
            hierarchy.add(type.getName());
        }
        MethodElement callback = null;
        int mostSpecific = Integer.MAX_VALUE;
        for (MethodElement method : element.getEnclosedElements(ElementQuery.ALL_METHODS)) {
            // an interceptor method a class declares on itself interposes on other objects rather than being a
            // callback of this one
            if (!method.hasDeclaredAnnotation(annotation) || model.isInterceptorMethod(method)) {
                continue;
            }
            // the hierarchy is held with the class itself first, so the smaller index is the more specific one
            int declaredAt = hierarchy.indexOf(method.getDeclaringType().getName());
            if (declaredAt >= 0 && declaredAt < mostSpecific) {
                mostSpecific = declaredAt;
                callback = method;
            }
        }
        return callback;
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
