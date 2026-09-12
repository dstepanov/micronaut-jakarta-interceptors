package io.micronaut.interceptor.test.chaincache;

import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

/**
 * A bean that declares an interceptor method on itself, and declares no binding and names no interceptor class.
 *
 * <p>Its name looks arbitrary because it was chosen rather than written: the interception the processor declares on
 * it is {@code self=Abmkvgcz.class} beside an empty list of named interceptor classes, and the interception of
 * {@link MarkedService} is a binding beside the same empty list. Micronaut hashes an annotation by adding up one
 * number per member, and this name is the one that makes the number its {@code self} member contributes equal to
 * the number the {@code bindings} member of {@link MarkedService} contributes. The two interceptions then hash
 * alike, and {@code AnnotationValue.equals} compares them equal as well, because it compares the sizes of the two
 * member maps and then skips a member the other map does not have. Anything that remembers a chain under the
 * annotation itself hands one of these two beans the chain of the other; see {@link ChainCacheKeyTest}.</p>
 */
@Singleton
public class Abmkvgcz {

    public String work() {
        return "collides";
    }

    @AroundInvoke
    Object itsOwn(InvocationContext context) throws Exception {
        Calls.RECORDED.add("self");
        return context.proceed();
    }
}
