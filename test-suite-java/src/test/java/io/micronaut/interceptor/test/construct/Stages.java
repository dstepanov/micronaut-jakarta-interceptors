package io.micronaut.interceptor.test.construct;

import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** What happened, in order, as a {@link StagedService} was created. Also what its interceptors are injected with. */
@Singleton
public class Stages {

    public static final List<String> RECORDED = Collections.synchronizedList(new ArrayList<>());
}
