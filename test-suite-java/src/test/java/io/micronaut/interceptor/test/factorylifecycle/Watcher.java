package io.micronaut.interceptor.test.factorylifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Interceptor
@Watched
@Priority(Interceptor.Priority.APPLICATION)
public class Watcher {

    public static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    @AroundConstruct
    Object construct(InvocationContext context) throws Exception {
        Object constructed = context.proceed();
        EVENTS.add("around construct " + named(context.getTarget()));
        return constructed;
    }

    @PostConstruct
    void postConstruct(InvocationContext context) throws Exception {
        EVENTS.add("post construct " + named(context.getTarget()));
        context.proceed();
    }

    @PreDestroy
    void preDestroy(InvocationContext context) throws Exception {
        EVENTS.add("pre destroy " + named(context.getTarget()));
        context.proceed();
    }

    private static String named(Object target) {
        if (target instanceof Assembly) {
            return "Assembly";
        }
        if (target instanceof GearAssembly) {
            return "GearAssembly";
        }
        if (target instanceof Gear) {
            return "Gear";
        }
        return target instanceof Part ? "Part" : String.valueOf(target);
    }
}
