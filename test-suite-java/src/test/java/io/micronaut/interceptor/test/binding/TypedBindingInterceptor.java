package io.micronaut.interceptor.test.binding;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.Set;
import java.util.TreeSet;

/** Reads the bindings of the element through the typed accessors the specification added in 2.2. */
@Interceptor
@TypedRead
public class TypedBindingInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Cached cached = context.getInterceptorBinding(Cached.class);
        Calls.RECORDED.add("cached=" + (cached == null ? "none" : cached.region()));

        Set<String> labels = new TreeSet<>();
        for (Labelled labelled : context.getInterceptorBindings(Labelled.class)) {
            labels.add(labelled.value());
        }
        Calls.RECORDED.add("labelled=" + labels);

        // an annotation the element does not carry as a binding
        Calls.RECORDED.add("tagged=" + context.getInterceptorBinding(Tagged.class));
        Calls.RECORDED.add("taggedAll=" + context.getInterceptorBindings(Tagged.class));
        Set<String> all = new TreeSet<>();
        context.getInterceptorBindings().forEach(a -> all.add(a instanceof Labelled l
            ? "Labelled(" + l.value() + ")" : a.annotationType().getSimpleName()));
        Calls.RECORDED.add("full=" + all);
        return context.proceed();
    }
}
