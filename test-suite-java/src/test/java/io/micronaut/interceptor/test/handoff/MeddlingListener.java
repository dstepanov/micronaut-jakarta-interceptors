package io.micronaut.interceptor.test.handoff;

import io.micronaut.context.BeanContext;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;

/**
 * Creates another bean as the target of a proxy is created, which happens between the target and the proxy. The
 * interceptor instances of the target have to survive it.
 */
@Singleton
public class MeddlingListener implements BeanCreatedEventListener<HandedTargetService> {

    private final BeanContext beanContext;
    private boolean meddled;

    public MeddlingListener(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    @Override
    public HandedTargetService onCreated(BeanCreatedEvent<HandedTargetService> event) {
        // the event is published for the target and again for the proxy; meddling once keeps the count of
        // interceptor instances the test asserts on exact
        if (!meddled) {
            meddled = true;
            beanContext.getBean(Meddled.class);
        }
        return event.getBean();
    }
}
