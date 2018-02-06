package ml.alternet.util;

import java.util.function.Supplier;

public enum Zn0 implements Supplier<Integer> {
    x {
        @Override
        public Integer get() {
            return 10;
        }
    },
    b,
    y {
            @Override
            public Integer get() {
                return 30;
            }
    };
    Zn0() {}
    A42 e;
    Zn0(A42 e) {
        this.e = e;
    }
    @Override
    public Integer get() {
        return this.e.get();
    };

    static {
        EnumUtil.extend(A42.class);
    }

}
