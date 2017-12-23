package ml.alternet.misc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ml.alternet.facet.Presentable;
import ml.alternet.misc.CharRange$.Char;
import ml.alternet.misc.CharRange$.Chars;
import ml.alternet.misc.CharRange$.Range;
import ml.alternet.misc.CharRange$.Ranges;

/**
 * Define, merge, and exclude ranges of Unicode characters.
 *
 * @author Philippe Poulard
 */
public interface CharRange extends Presentable {

    /**
     * Indicates whether this range contains the given character or not.
     *
     * @param codepoint The codepoint to check.
     *
     * @return <code>true</code> if the codepoint is found in this range,
     *      <code>false</code> otherwise.
     */
    boolean contains(int codepoint);

    /**
     * Return all the inclusive intervals that compose this range.
     *
     * @return All the intervals, in order.
     *
     * @see Reversible#includes()
     */
    Stream<BoundRange> asIntervals();

    /**
     * Exclude a given range to this range.
     *
     * @param range The characters to exclude from this range.
     *
     * @return The new range of characters.
     */
    CharRange except(CharRange range);

    /**
     * Exclude the given ranges to this range.
     *
     * @param ranges The ranges to exclude from this range.
     *
     * @return The new range of characters.
     */
    default CharRange except(CharRange... ranges) {
        return new Ranges(this).except(ranges);
    }

    /**
     * Merges the given range with this range.
     *
     * @param range The characters to include in this range.
     *
     * @return The new range of characters.
     */
    default CharRange union(CharRange range) {
        return new Ranges(this, range);
    }

    /**
     * Merges the given ranges with this range.
     *
     * @param ranges The ranges to include in this range.
     *
     * @return The new range of characters.
     */
    default CharRange union(CharRange... ranges) {
        return new Ranges(Stream.concat(Stream.of(this), Stream.of(ranges)));
    }

    /**
     * Allow to compare ranges canonically.
     *
     * For example, <code>!'b'</code> and
     * <code>['&#x5C;u0000'-'a'] | ['c'-'&#x5C;u10ffff']</code> are the same.
     */
    Comparator<CharRange> CHAR_RANGE_COMPARATOR = (r1, r2) -> {
        Spliterator<BoundRange> s1 = r1.asIntervals().sorted().distinct().spliterator();
        Spliterator<BoundRange> s2 = r2.asIntervals().sorted().distinct().spliterator();
        BoundRange[] br1 = { null };
        BoundRange[] br2 = { null };
        while (s1.tryAdvance(br -> br1[0] = br) && s2.tryAdvance(br -> br2[0] = br)) {
            int c = br1[0].compareTo(br2[0]);
            if (c != 0) {
                return c;
            }
        } // intervals are NEVER empty, br1 and br2 are NEVER null
        return br1[0].compareTo(br2[0]);
        // if one has advanced more, a difference will be reported
    };

    /**
     * The empty range.
     */
    BoundRange EMPTY = new Range(0, -1) {

        @Override
        public boolean contains(int codepoint) {
            return false; // shortcut
        }

        @Override
        public CharRange except(CharRange range) {
            return this;
        }

        @Override
        public CharRange union(CharRange range) {
            return range;
        }
    };

    /**
     * The range for any character.
     */
     BoundRange ANY = new Range(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT) {

        @Override
        public boolean contains(int codepoint) {
            return true; // shortcut
        }

        @Override
        public CharRange union(CharRange range) {
            return this;
        }

        @Override
        public CharRange except(CharRange range) {
            if (range instanceof Reversible) {
                return ((Reversible) range).revert();
            } else {
                return super.except(range);
            }
        }

        @Override
        public CharRange except(CharRange... ranges) {
            if (ranges.length == 0) {
                return this;
            }
            CharRange first = ranges[0];
            StringBuffer sb = new StringBuffer();
            if (Arrays.stream(ranges).allMatch(c -> {
                if (    (c instanceof Char || c instanceof Chars)
                     && ((Reversible) c).includes() == ((Reversible) first).includes())
                {
                    if (c instanceof Char) {
                        sb.append(Character.toChars(((Char) c).car));
                    } else {
                        sb.append(((Chars) c).chars);
                    }
                    return true;
                } else {
                    return false;
                }
            }))
            {
                // all are individual chars
                if (sb.length() == 1 || sb.length() == 2 && Character.isHighSurrogate(sb.charAt(0))) {
                    return new Char(! ((Reversible) first).includes(), sb.codePointAt(0));
                } else {
                    return new Chars(! ((Reversible) first).includes(), sb.toString());
                }
            } else {
                return super.except(ranges);
            }
        }
    };

    /**
     * Create a range made of a single character.
     *
     * @param codepoint The unicode character (code point)
     *
     * @return The range for this single character.
     */
    static BoundRange is(int codepoint) {
        return new Char(true, codepoint);
    }

    /**
     * Create a range made of all characters except a single character.
     *
     * @param codepoint The unicode character (code point) to exclude.
     *
     * @return The range for all characters except one.
     */
    static BoundRange isNot(int codepoint) {
        return new Char(false, codepoint);
    }

    /**
     * Create a range made of characters.
     *
     * @param characters The characters to include.
     *
     * @return The range for the given characters.
     */
    static CharRange isOneOf(CharSequence characters) {
        if (characters.length() == 0) {
            return EMPTY;
        } else if (! characters.codePoints().skip(1).findFirst().isPresent()) {
            // Unicode length == 1 ?
            return is(characters.codePoints().findFirst().getAsInt());
        } else {
            return new Chars(true, characters);
        }
    }

    /**
     * Create a range made of other characters than those given.
     *
     * @param characters The characters to exclude.
     *
     * @return The range for the other characters than those given.
     */
    static CharRange isNotOneOf(CharSequence characters) {
        if (characters.length() == 0) {
            return ANY;
        } else if (! characters.codePoints().skip(1).findFirst().isPresent()) {
            // Unicode length == 1 ?
            return isNot(characters.codePoints().findFirst().getAsInt());
        } else {
            return new Chars(false, characters);
        }
    }

    /**
     * Create a range made of the characters from the start position
     * to the end position (included).
     *
     * @param start The unicode character (code point) that starts this
     *      range.
     * @param end The unicode character (code point) that ends this
     *      range.
     *
     * @return The actual range.
     */
    static BoundRange range(int start, int end) {
        if (start > end) {
            return EMPTY;
        } else if (end == start) {
            return new Char(true, start);
        } else if (start == Character.MIN_CODE_POINT && end == Character.MAX_CODE_POINT) {
            return ANY;
        } else {
            return new Range(start, end);
        }
    }

    /**
     * Allow to specify a set of values in terms of inclusion or exclusion.
     *
     * @author Philippe Poulard
     */
    interface Reversible {

        /**
         * Indicates whether a set of values is specified by inclusion or exclusion
         * of the mentioned values.
         *
         * @return <code>true</code> to include values (by default),
         *      <code>false</code> to exclude values.
         */
        default boolean includes() {
            return true;
        }

        /**
         * Create a char range that is the complement of this char range.
         *
         * @return The revert char range.
         */
        CharRange revert();

    }

    /**
     * Base implementation of unbound ranges.
     *
     * @see BoundRange
     *
     * @author Philippe Poulard
     */
    abstract class UnboundRange implements CharRange, Comparable<UnboundRange> {

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CharRange) {
                return CHAR_RANGE_COMPARATOR.compare(this, (CharRange) obj) == 0;
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return toPrettyString().toString();
        }

        @Override
        public int compareTo(UnboundRange range) {
            return CHAR_RANGE_COMPARATOR.compare(this, range);
        }

    }

    /**
     * A single interval with defined start and end boundaries.
     *
     * @see UnboundRange
     *
     * @author Philippe Poulard
     */
    abstract class BoundRange implements CharRange, Reversible, Comparable<BoundRange> {

        /**
         * The lower boundary, included.
         *
         * @return The Unicode codepoint that starts this range.
         */
        public abstract int start();

        /**
         * The upper boundary, included.
         *
         * @return The Unicode codepoint that ends this range.
         */
        public abstract int end();

        /**
         * Return all the characters of this bound range.
         *
         * @return A stream of characters included in this range of characters.
         */
        public IntStream characters() {
            return IntStream.rangeClosed(start(), end());
        }

        /**
         * Indicates whether this range is empty or not.
         *
         * @return <code>true</code> if the start codepoint
         *      is greater than the end codepoint,
         *      <code>false</code> otherwise.
         */
        public boolean isEmpty() {
            return start() > end();
        }

        /**
         * Bound char ranges are compared by their start codepoint.
         *
         * @param range The range to compare.
         */
        @Override
        public int compareTo(BoundRange range) {
            if (isEmpty()) {
                if (range.isEmpty()) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (range.isEmpty()) {
                return 1;
            } else {
                return start() - range.start();
            }
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            if (end() == start()) {
                return Char.append(buf.append('\''), start()).append('\'');
            } else if (isEmpty()) {
                return buf.append("''");
            } else {
                return Char.append(Char.append(buf.append("['"), start())
                        .append("'-'"), end()).append("']");
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BoundRange) {
                BoundRange range = (BoundRange) obj;
                return isEmpty() ? range.isEmpty() : range.start() == start() && range.end() == end();
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(start(), end());
        }

        @Override
        public String toString() {
            return toPrettyString().toString();
        }

    }

}
