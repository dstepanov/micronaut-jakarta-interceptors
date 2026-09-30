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
import io.micronaut.context.annotation.Executable;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Secondary;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.core.annotation.Indexed;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.ReflectionConfig;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.annotation.InterceptionKind;
import io.micronaut.interceptor.annotation.JakartaInterception;
import io.micronaut.interceptor.annotation.JakartaInterceptorIndex;
import io.micronaut.interceptor.annotation.JakartaInterceptorMethods;
import io.micronaut.interceptor.processor.BindingConflicts;
import io.micronaut.interceptor.processor.InterceptorBindingValues;
import io.micronaut.interceptor.processor.InterceptorClassModel;
import io.micronaut.interceptor.processor.InterceptorClassScanner;
import io.micronaut.interceptor.processor.JakartaInterceptors;
import io.micronaut.interceptor.runtime.JakartaInterceptorSupport;

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

    /**
     * The annotations that make a class one this visitor has something to declare on.
     *
     * <p>They are also what brings a class to the attention of the bean processor, which only looks at a class that
     * carries one of the annotations a visitor supports. An interceptor class need not carry anything else: the
     * visitor makes it a bean, and that bean is only written out when the processor looks at the class. Every
     * interceptor method annotation is therefore listed, the lifecycle callbacks included, so that a class named by
     * {@code @Interceptors} whose only interceptor methods interpose on {@code @PostConstruct} or
     * {@code @PreDestroy} becomes a bean as well.</p>
     */
    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of(
            JakartaInterceptorSupport.INTERCEPTOR,
            JakartaInterceptors.INTERCEPTORS,
            JakartaInterceptorSupport.INTERCEPTOR_BINDING,
            JakartaInterceptors.AROUND_INVOKE,
            JakartaInterceptors.AROUND_TIMEOUT,
            JakartaInterceptors.AROUND_CONSTRUCT,
            JakartaInterceptors.POST_CONSTRUCT,
            JakartaInterceptors.PRE_DESTROY
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
        boolean declaredAsABean = declaresABean(element);
        boolean isInterceptorClass = element.hasDeclaredAnnotation(JakartaInterceptorSupport.INTERCEPTOR)
            || interposesOnAnotherObject(model);
        if (isInterceptorClass && !model.intercepts()) {
            throw new ProcessingException(element, "The interceptor class [" + element.getName() + "] declares no "
                + "interceptor method. An interceptor class declares at least one of @AroundInvoke, @AroundConstruct, "
                + "@PostConstruct or @PreDestroy, each accepting a single InvocationContext");
        }
        if (model.intercepts()) {
            declareInterceptorMethods(model, declaredAsABean, isInterceptorClass, context);
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
     * Tells whether the application declares a class a bean of its own, as opposed to the module making it one.
     *
     * <p>What counts is a scope or an annotation that is a bean declaration, such as {@code @Singleton} or
     * {@code @Prototype}. An interceptor class that declares neither is made a bean by the module, as a prototype
     * and a secondary definition; see {@link #declareInterceptorMethods}. A plain {@code @Bean} on a class declaring
     * {@code @Interceptor} is not counted: every such class is made a bean anyway, so {@code @Bean} declares nothing
     * {@code @Interceptor} does not, and counting it would keep the definition from being made the secondary one a
     * factory producing the same class takes the place of - the two would be ambiguous instead.</p>
     *
     * <p>A class the application does declare a bean is left as it declared it, which is how Micronaut treats any
     * bean: an {@code @Interceptor} class that is also {@code @Singleton} is a singleton and not a secondary
     * definition, and a factory producing the same class is as ambiguous with it as with any other bean the
     * application declares twice.</p>
     */
    private static boolean declaresABean(ClassElement element) {
        if (element.hasStereotype(AnnotationUtil.SCOPE)) {
            return true;
        }
        if (isFactory(element)) {
            // a factory is a bean of its own, and a singleton at that, although it declares its scope as the default
            // of the annotation rather than as a scope annotation of its own
            return true;
        }
        boolean interceptorClass = element.hasDeclaredAnnotation(JakartaInterceptorSupport.INTERCEPTOR);
        for (String name : element.getAnnotationNamesByStereotype(Bean.class.getName())) {
            if (!interceptorClass || !Bean.class.getName().equals(name)) {
                return true;
            }
        }
        return false;
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
        String[] bindings = isInterceptorClass ? bindingsOf(interceptorClass, null, context) : new String[0];
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
            if (bindings.length > 0) {
                builder.member("bindings", bindings);
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
    private static void index(InterceptorClassModel model) {
        model.interceptorClass().annotate(Indexed.class, builder -> builder.value(JakartaInterceptorIndex.class));
    }

    /**
     * Declares the interception of a class and of its methods.
     */
    private static void intercept(ClassElement element,
                                  InterceptorClassModel model,
                                  boolean declaredAsABean,
                                  VisitorContext context) {
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
        List<MethodElement> methods = methodsOf(element).stream()
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
            && methods.stream().noneMatch(JakartaInterceptorVisitor::declaresInterception)) {
            return;
        }
        interceptEachTarget(element, false);
        String[] classBindings = bindingsOf(element, null, context);
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
            String[] constructorBindings = bindingsOf(constructor, element, context);
            constructor.annotate(JakartaInterception.class, builder -> {
                interceptorMembers(builder, whenConstructed);
                bindingsMember(builder, constructorBindings);
                // the classes the class names stay associated with it even where the constructor excludes them,
                // and are created before its construction is interposed on; see JakartaInterception.associated
                if (!classInterceptors.isEmpty()) {
                    builder.member("associated", classInterceptors.stream()
                        .map(name -> new AnnotationClassValue<>(name))
                        .toArray(AnnotationClassValue<?>[]::new));
                }
            });
            // the specification hands an @AroundConstruct interceptor method a java.lang.reflect.Constructor, which
            // Micronaut looks up on the class by the argument types of the bean constructor. Reflection is permitted
            // for it here so that the lookup answers inside a native image as it does on a virtual machine. The
            // methods need nothing of the kind: their executable methods look them up by constants, which the image
            // registers on its own
            constructor.annotate(ReflectiveAccess.class);
        }
        for (MethodElement method : methods) {
            interceptMethod(model, method, classInterceptors, classBindings, classDeclares, context);
        }
        permitBindingSynthesis(element, constructor, methods, producers);
    }

    /**
     * Tells whether a class is a factory, which is the one kind of class whose members declare beans of other types.
     *
     * <p>An abstract class is left out, as Micronaut leaves it out: no bean is produced from it.</p>
     */
    private static boolean isFactory(ClassElement element) {
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
        if (!declaresInterception(member)) {
            // the produced type carries whatever it declares on itself, as any other bean does
            if (typeDeclaresInterception(producer.producedType())) {
                interceptEachTarget(member, true);
            }
            return;
        }
        ClassElement producedType = producer.producedType();
        List<String> interceptors = new ArrayList<>(namedInterceptors(producedType, context));
        interceptors.addAll(namedInterceptors(member, context));
        String[] bindings = bindingsOf(member, producedType, context);
        member.annotate(JakartaInterception.class, builder -> {
            interceptorMembers(builder, interceptors);
            // Micronaut reads the metadata of a produced bean together with the metadata of the factory that
            // produced it, and the member the producer declares is the one that wins. Every member is therefore
            // written out, the empty ones included, so that nothing the factory declares for itself - its own
            // bindings, or the interceptor method it declares on itself, which would be invoked on the produced
            // object rather than on the factory - reaches the bean it produces
            builder.member("self", new AnnotationClassValue<>(void.class));
            builder.member("bindings", bindings);
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
            || methodsOf(type).stream().anyMatch(JakartaInterceptorVisitor::declaresInterception);
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
     */
    private static void permitBindingSynthesis(ClassElement element,
                                               @Nullable MethodElement constructor,
                                               List<MethodElement> methods,
                                               List<Producer> producers) {
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
        for (Producer producer : producers) {
            for (AnnotationValue<?> binding : InterceptorClassScanner.bindingsOf(producer.element())) {
                bindings.add(binding.getAnnotationName());
            }
        }
        for (String binding : bindings) {
            element.annotate(ReflectionConfig.class, builder -> builder
                .member("type", new AnnotationClassValue<>(binding))
                .member("accessType", TypeHint.AccessType.DYNAMIC_PROXY));
        }
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
        if (overridesObjectMethod(method)) {
            // the methods of Object are not business methods, and overriding one does not make it a method of
            // the class the way an ordinary declaration would. Class level advice reaches every method Micronaut
            // is able to override, so one of these is marked rather than passed over
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
        String[] methodBindings = bindingsOf(method, method.getOwningType(), context);
        // a binding the method declares replaces the one of the class, so the method carries a declaration of its
        // own as soon as what it is bound by differs from what its class is bound by
        boolean replacesBindings = !Arrays.equals(classBindings, methodBindings);
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
     * <p>A binding a member declares replaces the one of the same type its class declares - the whole of it, so a
     * member the declaration leaves to its default takes the default rather than the value the class gives it. The
     * metadata Micronaut hands out for a method merges the two member by member instead, so the class and the
     * member are read apart: the bindings of the class first, and the ones the member's own metadata holds written
     * over them by type. A binding declared on another annotation is one of the element's as well, which is why
     * the bindings are looked for by their stereotype rather than among the annotations the element declares
     * itself.</p>
     *
     * <p>A repeatable binding is as many bindings as the element carries occurrences of it, so the bindings are kept
     * by type as a list rather than as one: what a member declares of a type replaces every occurrence of that type
     * its class declares, and the occurrences of a type the member says nothing about are all inherited.</p>
     *
     * <p>The members excluded from a binding are read off the annotation type rather than off the element, which
     * records only the ones it declares a value for: an interceptor and the element it intercepts have to agree on
     * which members are compared, whether or not either of them declares a value for them.</p>
     *
     * @param element The element
     * @param owner   The class the element belongs to, or {@code null} when the element is the class
     * @param context The visitor context, which resolves the binding annotation types
     * @return The bindings, as strings, in a stable order
     */
    private static String[] bindingsOf(Element element, @Nullable ClassElement owner, VisitorContext context) {
        InterceptorBindingValues.ExcludedMembers excluded = InterceptorBindingValues.excludedMembersOf(context);
        Map<String, List<InterceptorBindingValues.Binding>> bindings = new LinkedHashMap<>();
        if (owner != null) {
            groupByType(InterceptorBindingValues.of(owner.getAnnotationMetadata(), excluded), bindings);
        }
        AnnotationMetadata own = InterceptorClassScanner.ownMetadataOf(element);
        Map<String, List<InterceptorBindingValues.Binding>> declared = new LinkedHashMap<>();
        groupByType(InterceptorBindingValues.of(own, excluded), declared);
        bindings.putAll(declared);
        return bindings.values()
            .stream()
            .flatMap(List::stream)
            .map(InterceptorBindingValues.Binding::canonical)
            .sorted()
            .toArray(String[]::new);
    }

    private static void groupByType(Set<InterceptorBindingValues.Binding> read,
                                    Map<String, List<InterceptorBindingValues.Binding>> into) {
        for (InterceptorBindingValues.Binding binding : read) {
            into.computeIfAbsent(binding.name(), name -> new ArrayList<>()).add(binding);
        }
    }

    private static void bindingsMember(AnnotationValueBuilder<JakartaInterception> builder, String[] bindings) {
        if (bindings.length > 0) {
            builder.member("bindings", bindings);
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
            if (named == null || declaresAnInterceptorMethod(named)) {
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
     * Every instance method of a class the interception may reach, the accessors of its properties included.
     *
     * <p>A method query answers the methods a class declares as methods. Kotlin declares a property rather than a
     * pair of methods, and the accessors of that property are methods of the class on the virtual machine like any
     * other - they can be bound, and class level advice reaches them - but the language model answers them only as
     * the accessors of the property, so they are taken from the properties and added to the methods. A language that
     * answers an accessor as a method as well, which Java and Groovy do, answers the same method twice, and the
     * second answer is dropped.</p>
     */
    private static List<MethodElement> methodsOf(ClassElement element) {
        List<MethodElement> methods = new ArrayList<>(
            element.getEnclosedElements(ElementQuery.ALL_METHODS.onlyInstance()));
        for (PropertyElement property : element.getSyntheticBeanProperties()) {
            addAccessor(methods, property.getReadMethod().orElse(null));
            addAccessor(methods, property.getWriteMethod().orElse(null));
        }
        return List.copyOf(methods);
    }

    /**
     * Adds the accessor of a property to the methods of a class, unless the class answered it as a method already or
     * it is no instance method.
     */
    private static void addAccessor(List<MethodElement> methods, @Nullable MethodElement accessor) {
        if (accessor == null || accessor.isStatic()) {
            return;
        }
        for (MethodElement method : methods) {
            if (method.getName().equals(accessor.getName()) && sameParameters(method, accessor)) {
                return;
            }
        }
        methods.add(accessor);
    }

    private static boolean sameParameters(MethodElement one, MethodElement other) {
        ParameterElement[] ours = one.getParameters();
        ParameterElement[] theirs = other.getParameters();
        if (ours.length != theirs.length) {
            return false;
        }
        for (int i = 0; i < ours.length; i++) {
            if (!ours[i].getType().getName().equals(theirs[i].getType().getName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether a class named by {@code @Interceptors} has an interceptor method to interpose with.
     *
     * <p>Reading the class again does not answer it on its own. An interceptor method that interposes on a
     * lifecycle callback has the annotation that would make Micronaut invoke it as a callback of its own class
     * taken off it once it has been recorded, and the metadata of a method is the same metadata wherever it is read
     * from, so a class already visited in this compilation reads as declaring nothing. What it declared is recorded
     * on it as {@code @JakartaInterceptorMethods} before that happens, which is what is read here: the descriptor
     * of the class and of its superclasses, whether it was written while they were visited or while they were
     * compiled before.</p>
     *
     * <p>A recorded method counts only while the class still has it. An interceptor method overridden by a method
     * that is not one is no longer a method of the class - that is what the descriptor of its superclass records
     * and the class itself no longer has - and a class left with nothing to interpose with is the one this filter
     * is for.</p>
     *
     * @param named The class the element names
     * @return Whether it declares an interceptor method
     */
    private static boolean declaresAnInterceptorMethod(ClassElement named) {
        if (InterceptorClassScanner.scan(named).intercepts()) {
            return true;
        }
        for (ClassElement type = named; type != null; type = type.getSuperType().orElse(null)) {
            AnnotationValue<?> recorded = type.getAnnotationMetadata()
                .getDeclaredAnnotation(JakartaInterceptorMethods.class);
            if (recorded == null) {
                continue;
            }
            for (InterceptionKind kind : InterceptionKind.values()) {
                String[] recordedNames = recorded.stringValues(kind.member());
                AnnotationClassValue<?>[] declaringTypes = recorded.annotationClassValues(kind.declaringTypesMember());
                for (int i = 0; i < recordedNames.length && i < declaringTypes.length; i++) {
                    if (stillDeclares(named, declaringTypes[i].getName(), recordedNames[i])) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Tells whether a class still has the method one of its classes declared, which it does not when a subclass
     * overrides it: the methods of a class are the ones in effect on it, and an override takes the place of the
     * method it overrides.
     */
    private static boolean stillDeclares(ClassElement element, String declaringType, String name) {
        return element.getEnclosedElements(ElementQuery.ALL_METHODS)
            .stream()
            .anyMatch(method -> method.getName().equals(name)
                && method.getDeclaringType().getName().equals(declaringType));
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
    private record Producer(MethodElement element, ClassElement producedType) {
    }
}
