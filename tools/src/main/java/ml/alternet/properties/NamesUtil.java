package ml.alternet.properties;

import java.util.function.IntFunction;
import java.util.stream.IntStream;

import ml.alternet.util.JavaUtil;
import ml.alternet.util.Util;

/**
 * Works on Java names.
 *
 * @author Philippe Poulard
 */
@Util
class NamesUtil {

    static String changeFirstChar(String s, IntFunction<Integer> charChange) {
        int[] cp = IntStream.concat(
            IntStream.of(charChange.apply(s.codePointAt(0))),
            s.codePoints().skip(1)
        ).toArray();
        return new String(cp, 0, cp.length);
    }

    static String asClassName(String s) {
        return changeFirstChar(
            JavaUtil.asJavaName(s),
            Character::toUpperCase
        );
    }

    static String asPropName(String s) {
        s = changeFirstChar(
            JavaUtil.asJavaName(s),
            Character::toLowerCase
        );
        if (JavaUtil.isKeyword(s)) {
            return s + '_';
        } else {
            return s;
        }
    }

}
