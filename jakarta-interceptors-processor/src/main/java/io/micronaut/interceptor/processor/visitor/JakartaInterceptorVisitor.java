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
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.AnnotationElement;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementQuery;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.processor.BindingConflicts;
import io.micronaut.interceptor.processor.InterceptorClassModel;
import io.micronaut.interceptor.processor.InterceptorClassScanner;
import io.micronaut.interceptor.processor.JakartaInterceptors;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorSupport;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
 * <p>The class is visited in phases, in this order: the bindings it uses are validated, its interceptor methods
 * are scanned and the class classified, the interceptor methods are recorded ({@link InterceptorMethodWriter}),
 * the interception of the class and its members is written ({@link BeanInterceptionWriter}), and last the
 * reflection its bindings need ({@link BindingReflectionHints}). Each phase is a stateless helper.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class JakartaInterceptorVisitor implements TypeElementVisitor<Object, Object> {

    /**
     * The element types an interceptor binding is written on, which are the ones section 3.1.1 c) compares.
     */
    private static final Set<ElementType> BINDING_TARGETS =
        Set.of(ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR);

    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    // the work is done in visitClass, which queries the members it needs itself
    @Override
    public TypeElementQuery query() {
        return TypeElementQuery.onlyClass();
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
        rejectNarrowerBindings(element, context);
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
        boolean isInterceptorClass = InterceptorClassScanner.isInterceptorClass(element, model);
        if (isInterceptorClass && !model.intercepts()) {
            throw new ProcessingException(element, "The interceptor class [" + element.getName() + "] declares no "
                + "interceptor method. An interceptor class declares at least one of @AroundInvoke, @AroundConstruct, "
                + "@PostConstruct or @PreDestroy, each accepting a single InvocationContext");
        }
        if (model.intercepts()) {
            InterceptorMethodWriter.declareInterceptorMethods(model, declaredAsABean, isInterceptorClass, context);
        }
        if (isInterceptorClass) {
            if (element.hasDeclaredAnnotation(JakartaInterceptorSupport.INTERCEPTOR)
                && !InterceptorClassScanner.bindingsOf(element).isEmpty()) {
                // only an interceptor class a binding annotation binds is looked for among the beans; one that is
                // named directly is found by the class the element names, and needs no index. A class interposing
                // on another object without declaring @Interceptor is one only where it is named directly, and its
                // bindings bind nothing, so it is not indexed either
                InterceptorMethodWriter.index(model);
            }
            return;
        }
        BeanInterceptionWriter.intercept(element, model, declaredAsABean, context);
    }

    /**
     * Reports an interceptor binding type that declares another interceptor binding type which cannot be written
     * everywhere it can, which section 3.1.1 c) makes a definition error: a binding declared {@code @Target(TYPE)}
     * may not be applied to a binding declared {@code @Target({TYPE, METHOD})}.
     *
     * <p>The compiler does not report it. A binding declared {@code @Target(TYPE)} may annotate an annotation type,
     * so the declaration compiles, and a method the outer binding is written on would then carry an inherited binding
     * that could never have been written on it. Only the element types an interceptor binding is written on - a
     * type, a method and a constructor - are compared.</p>
     *
     * @param element The class using the binding types
     * @param context The visitor context
     */
    private static void rejectNarrowerBindings(ClassElement element, VisitorContext context) {
        // the annotation types are not visited themselves, so they are checked where they are used: on the class,
        // its constructor and its methods, an interceptor class included
        Set<String> checked = new HashSet<>();
        rejectNarrowerBindingsOf(element, element, checked, context);
        element.getPrimaryConstructor()
            .ifPresent(constructor -> rejectNarrowerBindingsOf(constructor, element, checked, context));
        for (MethodElement method : InterceptorClassScanner.methodsOf(element)) {
            rejectNarrowerBindingsOf(method, element, checked, context);
        }
    }

    private static void rejectNarrowerBindingsOf(Element user,
                                                 ClassElement owner,
                                                 Set<String> checked,
                                                 VisitorContext context) {
        for (AnnotationValue<?> binding : InterceptorClassScanner.bindingsOf(user)) {
            String name = binding.getAnnotationName();
            if (checked.add(name)
                && context.getClassElement(name).orElse(null) instanceof AnnotationElement bindingType) {
                rejectNarrowerBinding(bindingType, owner, context);
            }
        }
    }

    private static void rejectNarrowerBinding(AnnotationElement bindingType,
                                              ClassElement owner,
                                              VisitorContext context) {
        Set<ElementType> targets = new LinkedHashSet<>(bindingType.getTargets());
        targets.retainAll(BINDING_TARGETS);
        String name = bindingType.getName();
        for (String declaredName : bindingType.getDeclaredAnnotationNames()) {
            if (declaredName.equals(name)) {
                continue;
            }
            ClassElement declared = context.getClassElement(declaredName).orElse(null);
            if (!(declared instanceof AnnotationElement declaredAnnotation)
                || !declared.hasDeclaredAnnotation(JakartaInterceptorSupport.INTERCEPTOR_BINDING)) {
                continue;
            }
            Set<ElementType> declaredTargets = declaredAnnotation.getTargets();
            if (!declaredTargets.containsAll(targets)) {
                Set<ElementType> missing = new LinkedHashSet<>(targets);
                missing.removeAll(declaredTargets);
                throw new ProcessingException(owner, "The interceptor binding type [" + name + "], used by ["
                    + owner.getName() + "], declares the interceptor binding type [" + declaredName + "], which may "
                    + "not be written on " + missing + " although [" + name + "] may. An interceptor binding type "
                    + "can only be applied to an interceptor binding type whose targets are a subset of its own, so "
                    + "that every element the outer binding is written on can carry the inherited one. Widen the "
                    + "@Target of [" + declaredName + "], or narrow the @Target of [" + name + "]");
            }
        }
    }

    /**
     * Tells whether the application declares a class a bean of its own, as opposed to the module making it one.
     *
     * <p>What counts is a scope or an annotation that is a bean declaration, such as {@code @Singleton} or
     * {@code @Prototype}. An interceptor class that declares neither is made a bean by the module, as a prototype
     * and a secondary definition; see {@link InterceptorMethodWriter#declareInterceptorMethods}. A plain {@code @Bean} on a class declaring
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
        if (BeanInterceptionWriter.isFactory(element)) {
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
}
