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
package io.micronaut.interceptor.errorprone;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Forbids reflection in the interception.
 *
 * <p>An interceptor method is invoked through the executable method Micronaut generated for it, a chain is read
 * from annotation metadata, and a binding is compared as a string. Only the accessors of
 * {@code jakarta.interceptor.InvocationContext} reach for the reflection of the platform, because the types the
 * specification has them return leave no choice, and each of those says so.</p>
 *
 * <p>This is an ErrorProne check rather than a Checkstyle rule because the rule is about types rather than
 * spelling. {@code getAnnotation} names two different things: {@code java.lang.Class} declares one, which asks the
 * platform, and {@code io.micronaut.core.annotation.AnnotationMetadata} declares one, which reads what Micronaut
 * compiled and is the whole point of the module. A regular expression sees one name; this sees the receiver, so
 * the reflective call is reported and the compiled read is not.</p>
 *
 * <p>Where the specification leaves no way around it, suppress with a reason:</p>
 *
 * <pre>{@code
 * // getMethod returns a java.lang.reflect.Method, which only the platform can produce
 * @SuppressWarnings("NoReflection")
 * public Method getMethod() {
 *     return context.getExecutableMethod().getTargetMethod();
 * }
 * }</pre>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@AutoService(BugChecker.class)
@BugPattern(
    name = "NoReflection",
    summary = "The interception does not reflect: this asks the platform for something Micronaut compiled.",
    explanation = """
        An interceptor method is invoked through the executable method Micronaut generated for it, an interceptor \
        chain is read from annotation metadata, and a binding is compared as a string. Reaching for the reflection \
        of the platform inflates the members of a class into the reflection data the virtual machine keeps for it, \
        or defines a proxy class that lives as long as its class loader, and needs reachability metadata in a \
        native image.

        Where the specification leaves no way around it - getMethod returns a java.lang.reflect.Method, \
        getConstructor a java.lang.reflect.Constructor, getInterceptorBindings the annotation instances themselves \
        - suppress this with @SuppressWarnings("NoReflection") and say above it why the platform had to be asked.""",
    severity = BugPattern.SeverityLevel.ERROR)
public final class NoReflection extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final long serialVersionUID = 1L;

    /** The members of a class, whose lookup inflates every declared member of it into the reflection data. */
    private static final Matcher<ExpressionTree> CLASS_MEMBERS =
        MethodMatchers.instanceMethod().onExactClass("java.lang.Class").namedAnyOf(
            "getMethod", "getMethods", "getDeclaredMethod", "getDeclaredMethods",
            "getConstructor", "getConstructors", "getDeclaredConstructor", "getDeclaredConstructors",
            "getField", "getFields", "getDeclaredField", "getDeclaredFields",
            "getRecordComponents", "getPermittedSubclasses", "getNestMembers");

    /**
     * The annotations of an annotated element, read from the element rather than from the metadata Micronaut
     * compiled. The receiver is what tells this from {@code AnnotationMetadata}, which declares the same names.
     */
    private static final Matcher<ExpressionTree> ANNOTATIONS_OF_AN_ELEMENT =
        MethodMatchers.instanceMethod().onDescendantOf("java.lang.reflect.AnnotatedElement").namedAnyOf(
            "getAnnotation", "getAnnotations", "getDeclaredAnnotation", "getDeclaredAnnotations",
            "getAnnotationsByType", "getDeclaredAnnotationsByType", "isAnnotationPresent");

    /** The accessibility flag, and instantiation that bypasses the bean context. */
    private static final Matcher<ExpressionTree> REFLECTIVE_ACCESS = Matchers.anyOf(
        MethodMatchers.instanceMethod().onDescendantOf("java.lang.reflect.AccessibleObject")
            .namedAnyOf("setAccessible", "trySetAccessible", "canAccess"),
        MethodMatchers.instanceMethod().onDescendantOf("java.lang.reflect.Constructor").named("newInstance"),
        MethodMatchers.instanceMethod().onDescendantOf("java.lang.reflect.Method").named("invoke"),
        MethodMatchers.instanceMethod().onExactClass("java.lang.Class").named("newInstance"),
        MethodMatchers.staticMethod().onClass("java.lang.Class").named("forName"));

    /** A proxy class, which is defined for the life of the class loader it is defined in. */
    private static final Matcher<ExpressionTree> PROXY = Matchers.anyOf(
        MethodMatchers.staticMethod().onClass("java.lang.reflect.Proxy")
            .namedAnyOf("newProxyInstance", "getProxyClass"),
        MethodMatchers.staticMethod().anyClass().namedAnyOf("lookup", "privateLookupIn"));

    /**
     * Building an annotation instance from the metadata, which is the proxy above reached by another name: the
     * metadata is compiled, but turning a value of it back into an annotation defines a class.
     */
    private static final Matcher<ExpressionTree> ANNOTATION_SYNTHESIS =
        MethodMatchers.instanceMethod().onDescendantOf("io.micronaut.core.annotation.AnnotationMetadata")
            .namedAnyOf("synthesize", "synthesizeDeclared", "synthesizeAll", "synthesizeAnnotationsByType",
                "synthesizeDeclaredAnnotationsByType");

    /**
     * The {@code java.lang.reflect.Method} of an executable method, which Micronaut looks up reflectively. The
     * executable method itself describes the declaring type, the name, the arguments and the annotations without
     * reflecting, and is what the interception invokes through.
     */
    private static final Matcher<ExpressionTree> TARGET_MEMBER = Matchers.anyOf(
        MethodMatchers.instanceMethod().onDescendantOf("io.micronaut.inject.MethodReference").named("getTargetMethod"),
        MethodMatchers.instanceMethod().onDescendantOf("io.micronaut.inject.ExecutableMethod").named("getTargetMethod"));

    private static final Matcher<ExpressionTree> ANY = Matchers.anyOf(
        CLASS_MEMBERS, ANNOTATIONS_OF_AN_ELEMENT, REFLECTIVE_ACCESS, PROXY, ANNOTATION_SYNTHESIS, TARGET_MEMBER);

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!ANY.matches(tree, state)) {
            return Description.NO_MATCH;
        }
        return describeMatch(tree);
    }
}
