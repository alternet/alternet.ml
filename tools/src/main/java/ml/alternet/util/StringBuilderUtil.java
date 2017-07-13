package ml.alternet.util;

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
     * @param delimiter The delimiter.
     * @param prefix The prefix.
     * @param suffix The suffix.
     * @param buf The buffer.
     * @return A collector to a buffer.
     */
    public static Collector<String, StringBuilder, StringBuilder> collectorOf(
            CharSequence delimiter,
            CharSequence prefix,
            CharSequence suffix,
            StringBuilder buf)
    {
        Object separator = new Object() {
            boolean first = true;
            @Override
            public String toString() {
                if (first) {
                    first = false;
                    return "";
                } else return delimiter.toString();
            }
        };
        return Collector.of(
            () -> buf.append(prefix),
            (sb, s) -> sb.append(separator).append(s),
            StringBuilder::append,
            sb -> sb.append(suffix)
        );
    }

}
