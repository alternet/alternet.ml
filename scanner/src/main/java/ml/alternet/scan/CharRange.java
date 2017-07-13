package ml.alternet.scan;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ml.alternet.facet.Presentable;
import ml.alternet.util.StringBuilderUtil;

/**
 * Define, merge, and exclude ranges of Unicode characters.
 *
 * @author Philippe Poulard
 */
public interface CharRange extends Presentable {

    /**
     * Read the next Unicode character that belongs to this range of characters.
     *
     * @param scanner The input to read.
     *
     * @return <code>empty</code> if this range was not found in the input,
     *      the actual value otherwise.
     *
     * @throws IOException When an I/O error occurs.
     */
    Optional<String> nextValue(Scanner scanner) throws IOException;

    /**
     * Indicates whether this range contains the given character or not.
     *
     * @param codepoint The codepoint to check.
     *
     * @return <code>true</true> if the codepoint is found in this range,
     *      <code>false</code> otherwise.
     */
    boolean contains(int codepoint);

    /**
     * Exclude a given range to this range.
     *
     * @param range The characters to exclude from this range.
     *
     * @return The new range of characters.
     */
    CharRange except(CharRange range);

    /**
     * Merges the given range with this range.
     *
     * @param range The characters to include in this range.
     *
     * @return The new range of characters.
     */
    CharRange union(CharRange range);

    /**
     * Return all the intervals that compose this range from this set.
     *
     * @return All the intervals, in order.
     */
    Stream<BoundCharRange> asIntervals();

    /**
     * Create a range made of a single character.
     *
     * @param codepoint The unicode character (code point)
     *
     * @return The range for this single character.
     */
    static CharRange is(int codepoint) {
        return new Char(true, codepoint);
    }

    /**
     * Create a range made of all characters except a single character.
     *
     * @param codepoint The unicode character (code point) to exclude.
     *
     * @return The range for all characters except one.
     */
    static CharRange isNot(int codepoint) {
        return new Char(false, codepoint);
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
    static CharRange range(int start, int end) {
        return new Range(start, end);
    }

    /**
     * A single interval with defined start and end boundaries.
     *
     * @author Philippe Poulard
     */
    abstract class BoundCharRange implements CharRange {

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
         * Indicates whether this range is empty or not.
         *
         * @return <code>true</code> if the start codepoint
         *      is greater than the end codepoint,
         *      <code>false</code> otherwise.
         */
        public boolean isEmpty() {
            return start() > end();
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            if (end() == start()) {
                return buf.append('\'').append(start()).append('\'');
            } else if (isEmpty()){
                return buf.append("''");
            } else {
                return buf.append("['").append(start()).append("'-'").append(end()).append("']");
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof BoundCharRange) {
                BoundCharRange range = (BoundCharRange) obj;
                return range.start() == start() && range.end() == end();
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

        /**
         * Indicates whether the characters specified here are
         * include, exclude, or whether it is unrelated.
         *
         * @return The kind of range.
         */
        public Kind getKind() {
            return Kind.UNRELATED;
        };

    }

    /**
     * Indicates whether a range is specified by inclusion or exclusion
     * of the mentioned characters, or whether it is made of several
     * ranges (unrelated).
     *
     * @author Philippe Poulard
     */
    enum Kind {
        /** The mentioned characters are included in the range. */
        INCLUSION,
        /** The mentioned characters are excluded in the range. */
        EXCLUSION,
        /** Case of several ranges. */
        UNRELATED;
    }

    /**
     * Define a range made of a single character, or made of
     * all characters but one.
     *
     * @author Philippe Poulard
     */
    class Char extends BoundCharRange {

        int car;
        boolean equal;

        /**
         * Defines a range with a single character.
         *
         * @param equal <code>true</code> to indicate inclusion,
         *      <code>false</code> to indicate exclusion.
         * @param car The actual character.
         */
        public Char(boolean equal, int car) {
            this.car = car;
            this.equal = equal;
        }

        @Override
        public int start() {
            return car;
        }

        @Override
        public int end() {
            return car;
        }

        @Override
        public Kind getKind() {
            return this.equal ? Kind.INCLUSION : Kind.EXCLUSION;
        }

        @Override
        public Optional<String> nextValue(Scanner scanner) throws IOException {
            if (! (this.equal ^ scanner.hasNextChar((char) start(), false))) {
                // match
                char c = scanner.nextChar();
                return Optional.of(String.valueOf(c));
            }
            return Optional.empty();
        }

        @Override
        public Stream<BoundCharRange> asIntervals() {
            if (this.equal) {
                return Stream.of(this);
            } else {
                return reverse(IntStream.of(start()));
            }
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            if (! this.equal) {
                buf.append("!");
            }
            return super.toPrettyString(buf);
        }

        @Override
        public CharRange except(CharRange range) {
            if (range.contains(this.car)) {
                return new Range(0, -1); // empty
            } else {
                return this;
            }
        }

        public CharRange union(Char range) {
            if ( ! (this.equal ^ range.equal) ) {
                return new Chars(this.equal, this.car, range.car);
            } else {
                return new Ranges(this, range);
            }
        }

        @Override
        public CharRange union(CharRange range) {
            return range.union(this);
        }

        @Override
        public boolean contains(int codepoint) {
            return this.equal ^ this.car != codepoint;
        }

    }

    /**
     * Create a range of characters made of the characters of a string,
     * either by inclusion or exclusion.
     *
     * @author Philippe Poulard
     */
    class Chars implements CharRange {

        String chars;
        boolean equal;

        /**
         * Defines a range with the characters given in a string.
         *
         * @param equal <code>true</code> to indicate inclusion,
         *      <code>false</code> to indicate exclusion.
         * @param cars The actual characters.
         */
        public Chars(boolean equal, String cars) {
            int[] codepoints = cars.chars().sorted().distinct().toArray();
            this.chars = new String(codepoints, 0, codepoints.length);
            this.equal = equal;
        }

        /**
         * Defines a range with the codepoints given.
         *
         * @param equal <code>true</code> to indicate inclusion,
         *      <code>false</code> to indicate exclusion.
         * @param cars The actual codepoints.
         */
        public Chars(boolean equal, int... chars) {
            this(equal, new String(chars, 0, chars.length));
        }

        @Override
        public boolean contains(int codepoint) {
            return this.equal ^ this.chars.indexOf(codepoint) == -1;
        }

        @Override
        public Optional<String> nextValue(Scanner scanner) throws IOException {
            if (! (equal ^ scanner.hasNextChar(this.chars, false))) {
                // match
                char c = scanner.nextChar();
                return Optional.of(String.valueOf(c));
            }
            return Optional.empty();
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            if (! this.equal) {
                buf.append("!");
            }
            return this.chars.codePoints()
                .boxed()
                .collect( StringBuilderUtil.collectorOf(
                    "( ", " | ", " )", buf,
                    cp -> buf.appendCodePoint(cp))
                );
        }

        @Override
        public CharRange except(CharRange range) {
            return null;
        }

        public CharRange union(Char range) {
            if ( ! (this.equal ^ range.equal) ) {
                if (this.chars.indexOf(range.car) == -1) {
                    return new Chars(this.equal,
                        Stream.concat(
                            this.chars.chars().boxed(),
                            Stream.of(range.car) )
                        .mapToInt(i -> i)
                        .toArray()
                    );
                } else {
                    return this;
                }
            } else {
                return new Ranges(this, range);
            }
        }

        public CharRange union(Chars range) {
            if ( ! (this.equal ^ range.equal) ) {
                StringBuilder sb = new StringBuilder(this.chars);
                return new Chars(this.equal, range.chars.chars()
                    .filter(c -> this.chars.indexOf(c) == -1)
                    .boxed()
                    .collect(() -> sb,
                            StringBuilder::appendCodePoint,
                            StringBuilder::append)
                    .toString());
            } else {
                return new Ranges(this, range);
            }
        }

        @Override
        public CharRange union(CharRange range) {
            return new Ranges(this, range);
        }

        @Override
        public Stream<BoundCharRange> asIntervals() {
            if (this.equal) {
                if (this.chars.length() == 0) {
                    return Stream.empty();
                } else {
                    int[] codepoints = this.chars.chars().sorted().distinct().toArray();
                    // group consecutive characters to a range, and serve individual ones
                    Spliterator<BoundCharRange> iter = new AbstractSpliterator<BoundCharRange>(
                        codepoints.length,
                        Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL |
                        Spliterator.ORDERED | Spliterator.SIZED)
                    {
                        int i = 1;
                        boolean end = false;
                        int firstOfGroup = codepoints[0];
                        int lastOfGroup = firstOfGroup;

                        @Override
                        public boolean tryAdvance(Consumer<? super BoundCharRange> action) {
                            if (end) {
                                return false;
                            }
                            for ( ; i < codepoints.length ; i++) {
                                int cp = codepoints[i];
                                if (cp == lastOfGroup + 1) {
                                    lastOfGroup++;
                                    continue;
                                } else {
                                    break;
                                }
                            }
                            if (firstOfGroup == lastOfGroup) {
                                action.accept(new Char(equal, firstOfGroup));
                            } else {
                                action.accept(new Range(firstOfGroup, lastOfGroup));
                            }
                            if (i >= codepoints.length) {
                                end = true;
                            }
                            return true;
                        }
                    };
                    return StreamSupport.stream(iter, false);
                }
            } else {
                // range inversion
                return reverse(this.chars.chars());
            }
        }

    }

    /**
     * A character taken from a range of character.
     *
     * @author Philippe Poulard
     */
    class Range extends BoundCharRange {

        private int start;
        private int end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public int start() {
            return this.start;
        }

        @Override
        public int end() {
            return this.end;
        }

        @Override
        public Kind getKind() {
            return Kind.INCLUSION;
        }

        @Override
        public Optional<String> nextValue(Scanner scanner) throws IOException {
            char c = scanner.lookAhead();
            if (c >= start() && c <= end()) {
                // match
                c = scanner.nextChar(); // consume
                return Optional.of(String.valueOf(c));
            }
            return Optional.empty();
        }

        @Override
        public CharRange except(CharRange range) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public CharRange union(CharRange range) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Stream<BoundCharRange> asIntervals() {
            return Stream.of(this);
        }

        @Override
        public boolean contains(int codepoint) {
            return codepoint >= start() && codepoint <= end();
        }

    }

    class Ranges implements CharRange {

        TreeSet<BoundCharRange> ranges = new TreeSet<>((r1, r2) -> r2.start() - r1.end());

        public Ranges() { }

        public Ranges(CharRange... cr) {
            Arrays.asList(cr).stream()
                .flatMap(c -> c.asIntervals())
                .forEach(r -> add(r));
        }

        private void add(CharRange cr) { // NOTE : this is not cloned here
            // TODO : find intersection in ranges
        }

        @Override
        public Optional<String> nextValue(Scanner scanner) throws IOException {
            return null;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return this.ranges.stream()
                .collect(
                    StringBuilderUtil.collectorOf(
                        "(", " | ", ")", buf,
                        bcr -> bcr.toPrettyString(buf) )
            );
        }

        @Override
        public CharRange except(CharRange range) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public CharRange union(CharRange range) {
            Ranges r = new Ranges();
            r.ranges.addAll(this.ranges);
            add(range);
            return r;
        }

        @Override
        public Stream<BoundCharRange> asIntervals() {
            return this.ranges.stream();
        }

        @Override
        public boolean contains(int codepoint) {
            // TODO Auto-generated method stub
            return false;
        }

    }

    /**
     * Reverse a sequence of chars.
     *
     * @param chars The chars to reverse.
     *
     * @return The new stream contains all the chars that are
     *      not in the input stream.
     */
    static Stream<BoundCharRange> reverse(IntStream chars) {
        int[] lower = { Character.MIN_CODE_POINT };
        // range reversion
        return Stream.<BoundCharRange> concat(
            chars.sorted()
                .distinct()
                // filter out consecutive chars
                .filter(c -> c == lower[0] ? lower[0]++ < 0 : true)
                .boxed()
                .map(c -> {
                    // the char before because the upper bound must exclude it
                    Range r = new Range(lower[0], c - 1);
                    lower[0] = c + 1; // capture the last
                    return r;
                }),
            Stream.of(
                new Range( -1, Character.MAX_CODE_POINT) {
                    @Override
                    public int start() {
                        return lower[0]; // the very last character
                    };
                })
        ).filter(r -> ! r.isEmpty());
    }

}
