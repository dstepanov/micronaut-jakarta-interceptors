package io.micronaut.interceptor.test.binding;

import jakarta.inject.Singleton;

/** The beans a binding is carried to by another annotation. */
final class CarriedBindingServices {

    private CarriedBindingServices() {
    }

    /** The plain carrier on the class. */
    @Singleton
    @Surveilled
    public static class SurveilledClass {

        public String work() {
            return "work";
        }
    }

    /** The plain carrier on a method, which is all the interception the class declares. */
    @Singleton
    public static class SurveilledMethod {

        @Surveilled
        public String work() {
            return "work";
        }

        public String rest() {
            return "rest";
        }
    }

    /** The plain carrier on a method, beside a method that declares the binding directly. */
    @Singleton
    public static class SurveilledBesideWatched {

        @Surveilled
        public String work() {
            return "work";
        }

        @Watched
        public String direct() {
            return "direct";
        }
    }

    /** A binding carrying the binding on a method, which is all the interception the class declares. */
    @Singleton
    public static class GuardedMethod {

        @Guarded
        public String work() {
            return "work";
        }
    }
}
