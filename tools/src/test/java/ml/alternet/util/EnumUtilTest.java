package ml.alternet.util;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

@Test
public class EnumUtilTest {

    public static enum A {
        b, c, d;
    }

    public static enum Z {
        x, b, y;
        static {
            EnumUtil.extend(A.class); // expect z = { b, c, d, x, y }
        }
    }

    public void enum_ShouldBe_extendWithNewValues() {
        Z[] values = Z.values();

        Assertions.assertThat(values).hasSize(5);

        Assertions.assertThat(Arrays.stream(values).map(v -> v.name()).collect(Collectors.toList()))
            .containsExactly("x", "b", "y", "c", "d");

        Assertions.assertThat(Arrays.stream(values).map(v -> v.ordinal()).collect(Collectors.toList()))
            .containsExactly(0, 1, 2, 3, 4);

        Z c = Z.valueOf("c");
        Assertions.assertThat(c.name()).isEqualTo("c");
    }

    public void enumWithInterface_ShouldBe_extendWithNewValues() {
        // expect z = { b, c, d, x, y }
        Zn0[] values = Zn0.values();

        Assertions.assertThat(values).hasSize(5);

        Assertions.assertThat(Arrays.stream(values).map(v -> v.name()).collect(Collectors.toList()))
            .containsExactly("x", "b", "y", "c", "d");

        Assertions.assertThat(Arrays.stream(values).map(v -> v.ordinal()).collect(Collectors.toList()))
            .containsExactly(0, 1, 2, 3, 4);

        Zn0 c = Zn0.valueOf("c");
        Assertions.assertThat(c.name()).isEqualTo("c");
    }

    public void enum_ShouldBe_extendTwiceWithNewValues() {
        DayOfWeek[] values = DayOfWeek.values();
        Assertions.assertThat(values).hasSize(7);

        Assertions.assertThat(Arrays.stream(values).map(v -> v.name()).collect(Collectors.toList()))
            .containsExactly("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

        Assertions.assertThat(Arrays.stream(values).map(v -> v.ordinal()).collect(Collectors.toList()))
            .containsExactly(0, 1, 2, 3, 4, 5, 6);

        DayOfWeek day = DayOfWeek.valueOf("WED");
        Assertions.assertThat(day.name()).isEqualTo("WED");

        Assertions.assertThat(
            Arrays.stream(values).filter(v -> {
                try {
                    Weekday.valueOf(v.name());
                    return false;
                } catch (IllegalArgumentException e) {
                    return true;
                }
            }).map(DayOfWeek::isWeekendDay)
        ).allMatch(b -> b);
        Assertions.assertThat(
            Arrays.stream(values).filter(v -> {
                try {
                    Weekday.valueOf(v.name());
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }).map(DayOfWeek::isWeekendDay)
        ).allMatch(b -> ! b);
    }

    public static enum Weekday {
        MON, TUE, WED, THU, FRI;
    }

    public static enum WeekendDay {
        SAT, SUN;
    }

    public static enum DayOfWeek {
        MON; // enum expect at least one value

        private boolean isWeekendDay = false;

        public boolean isWeekendDay() {
            return this.isWeekendDay;
        }

        private DayOfWeek() { }

        private DayOfWeek(WeekendDay weekendDay) {
            this.isWeekendDay = true;
        }

        static {
            EnumUtil.extend(Weekday.class);
            EnumUtil.extend(WeekendDay.class);
        }
    }

}
