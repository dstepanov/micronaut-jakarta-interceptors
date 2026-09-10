package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;

/**
 * One method declaring the same values as the interceptor in every member, and one for each member differing from
 * it in that member alone: the first shows every branch comparing equal contents as equal, and each of the others
 * shows its own branch comparing different contents as different.
 */
@Singleton
public class PrimitiveArraysService {

    @Primitives(bools = {true, false}, bytes = {1, 2}, shorts = {3, 4}, chars = {'a', 'b'},
        ints = {5, 6}, longs = {7L, 8L}, floats = {1.5f, 2.5f}, doubles = {3.5, 4.5})
    public void matching() {
    }

    @Primitives(bools = {true, true}, bytes = {1, 2}, shorts = {3, 4}, chars = {'a', 'b'},
        ints = {5, 6}, longs = {7L, 8L}, floats = {1.5f, 2.5f}, doubles = {3.5, 4.5})
    public void differentBools() {
    }

    @Primitives(bools = {true, false}, bytes = {1, 9}, shorts = {3, 4}, chars = {'a', 'b'},
        ints = {5, 6}, longs = {7L, 8L}, floats = {1.5f, 2.5f}, doubles = {3.5, 4.5})
    public void differentBytes() {
    }

    @Primitives(bools = {true, false}, bytes = {1, 2}, shorts = {3, 9}, chars = {'a', 'b'},
        ints = {5, 6}, longs = {7L, 8L}, floats = {1.5f, 2.5f}, doubles = {3.5, 4.5})
    public void differentShorts() {
    }

    @Primitives(bools = {true, false}, bytes = {1, 2}, shorts = {3, 4}, chars = {'a', 'z'},
        ints = {5, 6}, longs = {7L, 8L}, floats = {1.5f, 2.5f}, doubles = {3.5, 4.5})
    public void differentChars() {
    }

    @Primitives(bools = {true, false}, bytes = {1, 2}, shorts = {3, 4}, chars = {'a', 'b'},
        ints = {5, 9}, longs = {7L, 8L}, floats = {1.5f, 2.5f}, doubles = {3.5, 4.5})
    public void differentInts() {
    }

    @Primitives(bools = {true, false}, bytes = {1, 2}, shorts = {3, 4}, chars = {'a', 'b'},
        ints = {5, 6}, longs = {7L, 9L}, floats = {1.5f, 2.5f}, doubles = {3.5, 4.5})
    public void differentLongs() {
    }

    @Primitives(bools = {true, false}, bytes = {1, 2}, shorts = {3, 4}, chars = {'a', 'b'},
        ints = {5, 6}, longs = {7L, 8L}, floats = {1.5f, 9.5f}, doubles = {3.5, 4.5})
    public void differentFloats() {
    }

    @Primitives(bools = {true, false}, bytes = {1, 2}, shorts = {3, 4}, chars = {'a', 'b'},
        ints = {5, 6}, longs = {7L, 8L}, floats = {1.5f, 2.5f}, doubles = {3.5, 9.5})
    public void differentDoubles() {
    }
}
