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

import io.micronaut.aop.Interceptor;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.internal.metadata.BindingValue;
import io.micronaut.interceptor.internal.metadata.JakartaInterception;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorSupport;
import io.micronaut.interceptor.metadata.InterceptionKind;
import io.micronaut.interceptor.processor.BindingConflicts;
import io.micronaut.interceptor.processor.InterceptorBindingValues;
import io.micronaut.interceptor.processor.InterceptorClassModel;
import io.micronaut.interceptor.processor.InterceptorClassScanner;
import io.micronaut.interceptor.processor.JakartaInterceptors;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Writes the interception of an intercepted class, of its constructor, of its business methods and of the beans
 * its factory methods produce, for {@link JakartaInterceptorVisitor}.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class BeanInterceptionWriter {

    private BeanInterceptionWriter() {
    }

    /**
     * Declares the interception of a class and of its methods.
     */
    static void intercept(ClassElement element,
                          InterceptorClassModel model,
                          boolean declaredAsABean,
                          VisitorContext context) {
        boolean reflective = BindingReflectionHints.reflectiveAccessors(context);
        List<String> classInterceptors = namedInterceptors(element, context);
        MethodElement constructor = element.getPrimaryConstructor().orElse(null);
        List<String> constructorInterceptors = constructor == null ? List.of() : namedInterceptors(constructor, context);
        // an @AroundInvoke method interposes on the business methods of the class that declares it, but only when
        // that class is a bean of its own: a plain class declaring nothing else is an interceptor class, named by
        // @Interceptors somewhere, and intercepting it would be intercepting the interceptor
        boolean classDeclares = !classInterceptors.isEmpty()
            || !InterceptorClassScanner.bindingsOf(element).isEmpty()
            || (model.intercepts() && declaredAsABean);
        // a constructor may be bound on its own, which intercepts the construction of the bean and nothing else
        boolean constructorDeclares = constructor != null
            && (!constructorInterceptors.isEmpty() || !InterceptorClassScanner.bindingsOf(constructor).isEmpty());
        // a member of a factory that declares another bean is not a business method of the factory: what it declares
        // belongs to the bean it produces, and is worked out with that bean as the owner
        boolean factory = isFactory(element);
        List<Producer> producers = producersOf(element);
        List<MethodElement> methods = InterceptorClassScanner.methodsOf(element).stream()
            .filter(method -> !factory || !declaresAnotherBean(method))
            .toList();
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
        for (Producer producer : producers) {
            rejectConflictingBindings(producer.element(), "factory member", context);
        }
        if (!InterceptorClassScanner.bindingsOf(element).isEmpty()) {
            rejectFinalMethods(element, methods);
        }
        for (Producer producer : producers) {
            interceptProduced(producer, context);
        }
        if (!classDeclares && !constructorDeclares
            && methods.stream().noneMatch(BeanInterceptionWriter::declaresInterception)) {
            return;
        }
        interceptEachTarget(element, false);
        List<BindingValue> classBindings = InterceptorBindingValues.bindingsOf(element, null, context);
        if (classDeclares) {
            element.annotate(JakartaInterception.class, builder -> {
                interceptorMembers(builder, classInterceptors);
                selfMember(builder, model);
                bindingsMember(builder, classBindings);
            });
        }
        // the constructor carries the interception of its own: that is where Micronaut decides whether the
        // construction of a bean is intercepted, and it also lets a constructor declare a binding, or name its own
        // interceptor classes, after the ones the class names
        if (constructor != null && (classDeclares || constructorDeclares)) {
            List<String> whenConstructed = new ArrayList<>();
            if (!constructor.hasDeclaredAnnotation(JakartaInterceptors.EXCLUDE_CLASS_INTERCEPTORS)) {
                whenConstructed.addAll(classInterceptors);
            }
            whenConstructed.addAll(constructorInterceptors);
            List<BindingValue> constructorBindings = InterceptorBindingValues.bindingsOf(constructor, element, context);
            constructor.annotate(JakartaInterception.class, builder -> {
                interceptorMembers(builder, whenConstructed);
                bindingsMember(builder, constructorBindings);
            });
            // the specification hands an @AroundConstruct interceptor method a java.lang.reflect.Constructor, which
            // Micronaut Core looks up on the class by the argument types of the bean constructor, with or without
            // the reflection module. Reflection is permitted for it here so that the lookup answers inside a native
            // image as it does on a virtual machine. The methods need nothing of the kind: their executable methods
            // look them up by constants, which the image registers on its own
            constructor.annotate(ReflectiveAccess.class);
        }
        for (MethodElement method : methods) {
            interceptMethod(model, method, classInterceptors, classBindings, classDeclares, context);
        }
        if (reflective) {
            BindingReflectionHints.permitBindingSynthesis(element, constructor, methods, producers, context);
        }
    }

    /**
     * Tells whether a class is a factory, which is the one kind of class whose members declare beans of other types.
     *
     * <p>An abstract class is left out, as Micronaut leaves it out: no bean is produced from it.</p>
     */
    static boolean isFactory(ClassElement element) {
        return !element.isAbstract() && element.hasStereotype(Factory.class);
    }

    /**
     * The methods of a factory that declare beans of other types.
     *
     * <p>Both a static method and an instance method may produce a bean, which is why the whole of the method list
     * is read here rather than the instance methods the business methods are taken from.</p>
     */
    private static List<Producer> producersOf(ClassElement element) {
        if (!isFactory(element)) {
            return List.of();
        }
        List<Producer> producers = new ArrayList<>();
        for (MethodElement method : element.getEnclosedElements(ElementQuery.ALL_METHODS)) {
            if (declaresAnotherBean(method)) {
                producers.add(new Producer(method, method.getGenericReturnType()));
            }
        }
        return List.copyOf(producers);
    }

    /**
     * Tells whether a method of a factory declares a bean of the type it returns, which Micronaut reads from a bean
     * annotation or a scope the method declares.
     */
    private static boolean declaresAnotherBean(MethodElement method) {
        AnnotationMetadata own = InterceptorClassScanner.ownMetadataOf(method);
        return own.hasDeclaredStereotype(Bean.class.getName())
            || own.hasDeclaredStereotype(AnnotationUtil.SCOPE);
    }

    /**
     * Declares, on a member of a factory that produces a bean, the interception of the bean it produces.
     *
     * <p>Micronaut carries the metadata of such a member over to the bean it declares, so the interception written
     * here is the interception of the produced bean and is worked out with the produced type as the owner: the
     * bindings and the interceptor classes of the produced type, with the ones the member declares over them. What
     * the factory itself is bound by, and the interceptor methods the factory declares on itself, belong to the
     * factory and are deliberately left out - an interceptor method of the factory invoked on the produced object
     * would be invoked on the wrong receiver.</p>
     *
     * <p>The whole of the effective interception is written even where the produced type declares part of it,
     * because Micronaut merges the metadata of the member over the metadata of the type member by member: a member
     * the producer declares replaces the one of the type rather than adding to it.</p>
     */
    private static void interceptProduced(Producer producer, VisitorContext context) {
        MethodElement member = producer.element();
        ClassElement producedType = producer.producedType();
        InterceptorClassModel producedModel = InterceptorClassScanner.scan(producedType);
        if (InterceptorClassScanner.isInterceptorClass(producedType, producedModel)) {
            // an interceptor class is not intercepted, as it is not where it is a bean of its own: its bindings
            // are the ones it binds to others, and a proxy of it would be a second bean the interceptor class is
            // looked up by, invoking its own interceptor methods on every call made to it
            if (declaresInterception(member)) {
                // what the member declares would intercept the interceptor class, and would otherwise be dropped
                // without a word
                throw new ProcessingException(member, "The factory member [" + member.getDeclaringType().getName()
                    + "." + member.getName() + "] produces the interceptor class [" + producedType.getName()
                    + "] and declares interceptor bindings or @Interceptors of its own. An interceptor class is not "
                    + "intercepted, so they would bind nothing. Remove them from the member; the bindings the "
                    + "interceptor class declares on itself are the ones it binds to other beans");
            }
            return;
        }
        if (!declaresInterception(member)) {
            // the produced type carries whatever it declares on itself, as any other bean does
            if (typeDeclaresInterception(producer.producedType())) {
                interceptEachTarget(member, true);
            }
            return;
        }
        List<String> interceptors = new ArrayList<>(namedInterceptors(producedType, context));
        interceptors.addAll(namedInterceptors(member, context));
        List<BindingValue> bindings = InterceptorBindingValues.bindingsOf(member, producedType, context);
        member.annotate(JakartaInterception.class, builder -> {
            interceptorMembers(builder, interceptors);
            // Micronaut reads the metadata of a produced bean together with the metadata of the factory that
            // produced it, and the member the producer declares is the one that wins. Every member is therefore
            // written out, the empty ones included, so that nothing the factory declares for itself - its own
            // bindings, or the interceptor method it declares on itself, which would be invoked on the produced
            // object rather than on the factory - reaches the bean it produces. The interceptor methods the
            // produced class declares on itself are another matter: they are the ones of the object intercepted,
            // which interpose on its business methods as they do where the class is a bean of its own
            builder.member(JakartaInterception.SELF, selfOf(producedModel));
            builder.member(JakartaInterception.BINDINGS, InterceptorBindingValues.written(bindings));
        });
        interceptEachTarget(member, true);
    }

    /**
     * Has a proxy with a separate target intercept each target with the advice of that target.
     *
     * <p>Such a proxy - of a bean declared {@code @Around(proxyTarget = true)}, of a bean of a scope that resolves
     * through a proxy, or of a bean a factory produces - otherwise resolves an advice of its own, while the
     * construction and the lifecycle of the target are intercepted with the advice of the target, and the one object
     * the specification sees would have two sets of interceptor instances. {@code lazyInterceptorsPerTarget} has
     * the proxy intercept each target with the advice of the target. It is written on the {@code @Around} in effect
     * together with what that declares already: an {@code @Around} declared on an element replaces the one its scope
     * contributes.</p>
     *
     * @param element     The intercepted class, or the member of a factory producing an intercepted bean
     * @param proxyTarget Whether the element is proxied with a separate target whatever its {@code @Around} says,
     *                    which a bean a factory produces always is
     */
    private static void interceptEachTarget(Element element, boolean proxyTarget) {
        AnnotationValue<Annotation> around = element.getAnnotation(AnnotationUtil.ANN_AROUND);
        if (!proxyTarget && (around == null || !around.isTrue(Interceptor.PROXY_TARGET.toString()))) {
            return;
        }
        element.annotate(AnnotationUtil.ANN_AROUND, builder -> {
            if (around != null) {
                builder.members(around.getValues());
            }
            builder.member(Interceptor.LAZY_INTERCEPTORS_PER_TARGET.toString(), true);
        });
    }

    /**
     * Whether a type declares an interception of its own that makes a bean of it advised: interceptor classes or
     * bindings on the class, or on one of its methods.
     */
    private static boolean typeDeclaresInterception(ClassElement type) {
        return InterceptorClassScanner.ownMetadataOf(type).hasDeclaredAnnotation(JakartaInterceptors.INTERCEPTORS)
            || !InterceptorClassScanner.bindingsOf(type).isEmpty()
            || InterceptorClassScanner.methodsOf(type).stream().anyMatch(BeanInterceptionWriter::declaresInterception);
    }

    /**
     * Reports a final method of a class that declares or inherits a class level binding, which section 3.3 f) of the
     * specification makes a definition error.
     *
     * <p>Micronaut refuses a final method it would have advised, but it advises only the methods a class declares and
     * does not advise a protected one, so a protected final method, or a final method inherited from a superclass,
     * would otherwise be passed over in silence. The specification names every non-static, non-private final method.
     * The final methods of {@code Object} are left out: every class has them, and they are not methods of the class
     * the specification has in mind.</p>
     *
     * <p>A method the compiler generated is left out as well, and with it the accessor of a Kotlin property. The
     * accessor is an ordinary method of the class and is intercepted as one, but the language model answers whether
     * it is final from the source alone, without the allowance it makes for a function of a class the all-open
     * compiler plugin opens; an accessor the plugin opened is therefore not told apart from one that is really final,
     * and reporting it would refuse a class that is perfectly proxyable. A property that is not open is left
     * uninterceptable rather than reported, as Micronaut leaves it.</p>
     */
    private static void rejectFinalMethods(ClassElement element, List<MethodElement> methods) {
        for (MethodElement method : methods) {
            if (method.isFinal()
                && !method.isPrivate()
                && !method.isStatic()
                && !method.isSynthetic()
                && !Object.class.getName().equals(method.getDeclaringType().getName())) {
                throw new ProcessingException(method, "The class [" + element.getName() + "] carries a class level "
                    + "interceptor binding and has the method [" + method.getName() + "], declared final by ["
                    + method.getDeclaringType().getName() + "]. A method of a class bound at class level must not be "
                    + "final unless it is private or static, since it could not be intercepted. Make the method "
                    + "non-final, or declare the binding on the methods to be intercepted rather than on the class");
            }
        }
    }

    /**
     * Reports a binding annotation that reaches a method or a constructor along two paths, carrying different
     * member values, which the specification makes a definition error there as it does on a class.
     */
    private static void rejectConflictingBindings(MemberElement member, String noun, VisitorContext context) {
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
                                        List<BindingValue> classBindings,
                                        boolean classDeclares,
                                        VisitorContext context) {
        if (model.isInterceptorMethod(method)) {
            // an interceptor method is not a business method, so it takes no part in the interception of the
            // class. A private method is never intercepted to begin with, and marking one would leave advice
            // metadata on a method Micronaut cannot override, which it reports as an error
            if (!method.isPrivate()) {
                method.annotate(JakartaInterception.class, builder -> builder.member(JakartaInterception.EXCLUDED, true));
            }
            return;
        }
        if (overridesObjectMethod(method)) {
            // the methods of Object are not business methods, and overriding one does not make it a method of
            // the class the way an ordinary declaration would. Class level advice reaches every method Micronaut
            // is able to override, so one of these is marked rather than passed over
            if (!method.isPrivate()) {
                method.annotate(JakartaInterception.class, builder -> builder.member(JakartaInterception.EXCLUDED, true));
            }
            return;
        }
        if (!isBusinessMethod(method)) {
            return;
        }
        if (!classDeclares && !declaresInterception(method)) {
            return;
        }
        List<String> methodInterceptors = namedInterceptors(method, context);
        boolean excludesClassInterceptors = method.hasDeclaredAnnotation(JakartaInterceptors.EXCLUDE_CLASS_INTERCEPTORS);
        List<String> interceptors = new ArrayList<>(classInterceptors.size() + methodInterceptors.size());
        if (!excludesClassInterceptors) {
            interceptors.addAll(classInterceptors);
        }
        interceptors.addAll(methodInterceptors);
        // a schedule is recorded through its repeatable container even when a method declares only one
        boolean timeout = method.hasDeclaredAnnotation(JakartaInterceptorSupport.SCHEDULED)
            || method.hasDeclaredAnnotation(JakartaInterceptors.SCHEDULES);
        List<BindingValue> methodBindings = InterceptorBindingValues.bindingsOf(method, method.getOwningType(), context);
        // a binding the method declares replaces the one of the class, so the method carries a declaration of its
        // own as soon as what it is bound by differs from what its class is bound by
        boolean replacesBindings = !sameBindings(classBindings, methodBindings);
        // Micronaut applies the advice a class declares to the public and package private methods of the class, and
        // leaves a protected one alone, although it is able to override it: the proxy is a subclass. The
        // specification intercepts every non-static, non-private business method, so a protected one carries the
        // interception it inherits from its class itself, which is what makes Micronaut advise it
        boolean advisedOnlyWhenDeclaredOnTheMethod = method.isProtected();
        if (timeout || replacesBindings || !classDeclares || excludesClassInterceptors
            || !methodInterceptors.isEmpty() || advisedOnlyWhenDeclaredOnTheMethod) {
            // the list of the method replaces the one it would otherwise inherit from the class
            method.annotate(JakartaInterception.class, builder -> {
                interceptorMembers(builder, interceptors);
                selfMember(builder, model);
                bindingsMember(builder, methodBindings);
                if (timeout) {
                    builder.member(JakartaInterception.TIMEOUT, true);
                }
            });
        }
    }

    /**
     * Whether two elements are bound by the same bindings, whatever order either of them declares them in.
     */
    private static boolean sameBindings(List<BindingValue> one,
                                        List<BindingValue> other) {
        return Set.copyOf(one).equals(Set.copyOf(other));
    }

    private static void bindingsMember(AnnotationValueBuilder<JakartaInterception> builder,
                                       List<BindingValue> bindings) {
        if (!bindings.isEmpty()) {
            builder.member(JakartaInterception.BINDINGS, InterceptorBindingValues.written(bindings));
        }
    }

    /**
     * Declares the interceptor classes of an element.
     *
     * <p>The member is written even when there is none: the metadata of a method is read together with the
     * metadata of its class, member by member, so only a member the method declares itself replaces the one the
     * class declares. An empty list is how a method that excludes the interceptor classes of its class says so.</p>
     */
    private static void interceptorMembers(AnnotationValueBuilder<JakartaInterception> builder, List<String> interceptors) {
        builder.member(JakartaInterception.INTERCEPTORS, interceptors.stream()
            .map(name -> new AnnotationClassValue<>(name))
            .toArray(AnnotationClassValue<?>[]::new));
    }

    private static void selfMember(AnnotationValueBuilder<JakartaInterception> builder, InterceptorClassModel model) {
        if (interceptsItself(model)) {
            builder.member(JakartaInterception.SELF, new AnnotationClassValue<>(model.interceptorClass().getName()));
        }
    }

    /**
     * The class whose interceptor methods interpose on the business methods of its own instances, written as the
     * member of a declaration that has to replace whatever another declaration says of it.
     */
    private static AnnotationClassValue<?> selfOf(InterceptorClassModel model) {
        return interceptsItself(model)
            ? new AnnotationClassValue<>(model.interceptorClass().getName())
            : new AnnotationClassValue<>(void.class);
    }

    private static boolean interceptsItself(InterceptorClassModel model) {
        return model.methods().containsKey(InterceptionKind.AROUND_INVOKE)
            || model.methods().containsKey(InterceptionKind.AROUND_TIMEOUT);
    }

    /**
     * The interceptor classes an element names with {@code @Interceptors}.
     *
     * <p>A class named there that declares no interceptor method at all - one whose only interceptor method is
     * overridden by a method that is not one, say - has nothing to interpose with, and is left out. The runtime
     * requires every class it is handed to have had its interceptor methods recorded, and it cannot tell a class
     * that has none from one compiled without this processor, whose methods it cannot invoke; here the class itself
     * is visible, wherever it was compiled, and answers which of the two it is.</p>
     */
    private static List<String> namedInterceptors(Element element, VisitorContext context) {
        AnnotationValue<?> interceptors = element.getAnnotationMetadata()
            .getDeclaredAnnotation(JakartaInterceptors.INTERCEPTORS);
        if (interceptors == null) {
            return List.of();
        }
        // a set keeps the declaration order while ignoring a class named twice, which the specification invokes once
        Set<String> names = new LinkedHashSet<>();
        for (AnnotationClassValue<?> value : interceptors.annotationClassValues(AnnotationMetadata.VALUE_MEMBER)) {
            ClassElement named = context.getClassElement(value.getName()).orElse(null);
            if (named == null || InterceptorClassScanner.declaresAnInterceptorMethod(named)) {
                names.add(value.getName());
            }
        }
        return List.copyOf(names);
    }

    private static boolean declaresInterception(MemberElement member) {
        return InterceptorClassScanner.ownMetadataOf(member).hasDeclaredAnnotation(JakartaInterceptors.INTERCEPTORS)
            || !InterceptorClassScanner.bindingsOf(member).isEmpty();
    }

    /**
     * Tells whether a method is one of the business methods of a class, which are the instance methods Micronaut is
     * able to intercept, less the lifecycle callbacks, which are intercepted as callbacks rather than as methods.
     */
    private static boolean isBusinessMethod(MethodElement method) {
        return !method.isPrivate()
            && !method.isFinal()
            && !Object.class.getName().equals(method.getDeclaringType().getName())
            && !overridesObjectMethod(method)
            && !method.hasDeclaredAnnotation(JakartaInterceptors.POST_CONSTRUCT)
            && !method.hasDeclaredAnnotation(JakartaInterceptors.PRE_DESTROY);
    }

    /**
     * Tells whether a method is one of the methods {@link Object} declares, which a class may override and which
     * are no more a business method of it than they are of {@code Object} itself.
     *
     * <p>The overridable ones are matched by their signature: the rest of what {@code Object} declares is final,
     * so nothing can be declared for it here. A method that only shares a name with one of them - a
     * {@code toString} that takes something, an {@code equals} of the class's own type - is an overload rather
     * than an override, and is a business method as any other method would be.</p>
     */
    private static boolean overridesObjectMethod(MethodElement method) {
        ParameterElement[] parameters = method.getParameters();
        return switch (method.getName()) {
            case "toString", "hashCode", "clone", "finalize" -> parameters.length == 0;
            case "equals" -> parameters.length == 1
                && Object.class.getName().equals(parameters[0].getType().getName());
            default -> false;
        };
    }

    /**
     * A method of a factory that declares another bean, together with the type of the bean it declares.
     *
     * <p>A field of a factory declares a bean as well, and is left out: an interceptor binding annotation, and
     * {@code jakarta.interceptor.Interceptors}, are declared on a type, a method or a constructor, so a field can
     * carry no Jakarta interception for its bean to be given.</p>
     *
     * @param element      The factory method
     * @param producedType The type of the bean it produces
     */
    record Producer(MethodElement element, ClassElement producedType) {
    }
}
