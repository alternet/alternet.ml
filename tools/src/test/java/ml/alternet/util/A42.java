package ml.alternet.util;

import java.util.function.Supplier;

public enum A42 implements Supplier<Integer> {
    b, c, d;
    @Override
    public Integer get() {
        return 42;
    }
}