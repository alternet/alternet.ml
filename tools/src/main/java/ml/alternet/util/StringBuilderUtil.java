package ml.alternet.util;

import java.util.function.Consumer;
import java.util.stream.Collector;

/**
 * String builder utilities.
 *
 * @author Philippe Poulard
 */
@Util
public class StringBuilderUtil {

    /**
     * Return a collector that collects items inside an existing buffer.
     *
     * @param prefix The prefix.
     * @param delimiter The delimiter.
     * @param suffix The suffix.
     * @param buffer The buffer.
     * @param adder A function that adds its T argument to the buffer.
     *
     * @return A collector to a buffer.
     *
     * @param <T> The type of the items to collect.
     */
    public static <T> Collector<T, StringBuilder, StringBuilder> collectorOf(
            CharSequence prefix,
            CharSequence delimiter,
            CharSequence suffix,
            StringBuilder buffer,
            Consumer<T> adder)
    {
        Object separator = new Object() {
            boolean first = true;
            @Override
            public String toString() {
                if (first) {
                    first = false;
                    return "";
                } else {
                    return delimiter.toString();
                }
            }
        };
        return Collector.of(
            () -> buffer.append(prefix),
            (sbuf, item) -> {
                sbuf.append(separator);
                adder.accept(item);
            },
            StringBuilder::append,
            sb -> sb.append(suffix)
        );
    }

    /**
     * Return a collector that collects items inside an existing buffer.
     *
     * @param prefix The prefix.
     * @param delimiter The delimiter.
     * @param suffix The suffix.
     * @param buffer The buffer.
     *
     * @return A collector to a buffer.
     */
    public static Collector<String, StringBuilder, StringBuilder> collectorOf(
            CharSequence prefix,
            CharSequence delimiter,
            CharSequence suffix,
            StringBuilder buffer)
    {
        return collectorOf(prefix, delimiter, suffix, buffer, s -> buffer.append(s));
    }

}
