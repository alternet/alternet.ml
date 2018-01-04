package ml.alternet.misc;

import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterator.SORTED;
import static ml.alternet.misc.CharRange.range;
import static ml.alternet.util.StringBuilderUtil.collectorOf;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ml.alternet.misc.CharRange.BoundRange;
import ml.alternet.misc.CharRange.Reversible;
import ml.alternet.misc.CharRange.UnboundRange;

/**
 * Internal implementation of kind of ranges.
 *
 * @author Philippe Poulard
 */
class CharRange$ {

    /**
     * Define a range made of a single character, or made of
     * all characters but one.
     *
     * @author Philippe Poulard
     */
    static class Char extends BoundRange {

        int car;
        boolean equal;

        /**
         * Defines a range with a single character.
         *
         * @param equal <code>true</code> to indicate inclusion,
         *      <code>false</code> to indicate exclusion.
         * @param car The actual character.
         *
         * @see Reversible#includes()
         */
        Char(boolean equal, int car) {
            this.car = car;
            this.equal = equal;
        }

        @Override
        public int start() {
            return this.car;
        }

        @Override
        public int end() {
            return this.car;
        }

        @Override
        public boolean includes() {
            return this.equal;
        }

        @Override
        public boolean contains(int codepoint) {
            return this.equal ^ this.car != codepoint;
        }

        @Override
        public Stream<BoundRange> asIntervals() {
            if (this.equal) {
                return Stream.of(this);
            } else {
                return Chars.reverse(IntStream.of(start()));
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
            if (this.equal) {
                if (range.contains(this.car)) {
                    return EMPTY;
                } else {
                    return this;
                }
            } else {
                return new Ranges(this).except(range);
            }
        }

        /**
         * Merges the given range with this range.
         *
         * @param car The character to include in this range.
         *
         * @return The new range of characters, or the same if
         *      the given character already belong to this range.
         */
        public CharRange union(Char car) {
            if ( ! (this.equal ^ car.equal) ) {
                if (car.car == this.car) {
                    return this; // same
                } else {
                    return new Chars(this.equal, this.car, car.car);
                }
            } else if (car.car == this.car) {
                return ANY; // c U !c
            } else {
                return new Ranges(this, car);
            }
        }

        @Override
        public CharRange union(CharRange range) {
            // delegate to Chars, Range, or Ranges
            return range.union(this);
        }

        private static final char[] HEXES = "0123456789abcdef".toCharArray();

        public static StringBuilder append(StringBuilder buf, int c) {
            if (Character.isISOControl(c)) {
                switch (c) {
                case '\b' : buf.append("\\b"); break;
                case '\n' : buf.append("\\n"); break;
                case '\t' : buf.append("\\t"); break;
                case '\f' : buf.append("\\f"); break;
                case '\r' : buf.append("\\r"); break;
                case '\'' : buf.append("\\'"); break;
                case '\\' : buf.append("\\\\"); break;
                default :
                    buf.append("\\u");
                    buf.append(HEXES[(c >> 12) & 15]);
                    buf.append(HEXES[(c >> 8) & 15]);
                    buf.append(HEXES[(c >> 4) & 15]);
                    buf.append(HEXES[(c) & 15]);
                }
            } else if (c > 0xffff || c == 0) {
                buf.append("\\u").append(Integer.toHexString(c));
            } else {
                buf.appendCodePoint(c);
            }
            return buf;
        }

        @Override
        public CharRange revert() {
            return new Char(! this.equal, this.car);
        }

    }

    /**
     * Define a range of characters made of the characters of a string,
     * either by inclusion or exclusion.
     *
     * @author Philippe Poulard
     */
    static class Chars extends UnboundRange implements CharRange, Reversible {

        String chars;
        boolean equal;

        /**
         * Defines a range with the characters given in a string.
         *
         * @param equal <code>true</code> to indicate inclusion,
         *      <code>false</code> to indicate exclusion.
         * @param chars The actual characters.
         *
         * @see Reversible#includes()
         */
        Chars(boolean equal, CharSequence chars) {
            this(equal, chars.codePoints());
        }

        /**
         * Defines a range with the codepoints given.
         *
         * @param equal <code>true</code> to indicate inclusion,
         *      <code>false</code> to indicate exclusion.
         * @param codepoints The actual codepoints.
         */
        Chars(boolean equal, int... codepoints) {
            this(equal, IntStream.of(codepoints));
        }

        /**
         * Defines a range with the codepoints given.
         *
         * @param equal <code>true</code> to indicate inclusion,
         *      <code>false</code> to indicate exclusion.
         * @param codepoints The actual codepoints.
         */
        Chars(boolean equal, IntStream codepoints) {
              int[] cp = codepoints.sorted().distinct().toArray();
              this.chars = new String(cp, 0, cp.length);
              this.equal = equal;
        }

        @Override
        public boolean includes() {
            return this.equal;
        }

        @Override
        public boolean contains(int codepoint) {
            return this.equal ^ this.chars.indexOf(codepoint) == -1;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            if (! this.equal) {
                buf.append("!");
            }
            return this.chars.codePoints()
                .boxed()
                .collect(collectorOf(
                    "( ", " | ", " )", buf,
                    cp -> Char.append(buf.append('\''), cp).append('\''))
                );
        }

        @Override
        public CharRange except(CharRange range) {
            return new Ranges(this).except(range);
        }

        public CharRange union(Char car) {
            if ( ! (this.equal ^ car.equal) ) {
                if (this.chars.indexOf(car.car) == -1) {
                    return new Chars(
                        this.equal,
                        IntStream.concat(this.chars.codePoints(), IntStream.of(car.car) )
                    );
                } else {
                    return this;
                }
            } else {
                return new Ranges(this, car);
            }
        }

        public CharRange union(Chars chars) {
            if ( ! (this.equal ^ chars.equal) ) {
                return new Chars(
                    this.equal,
                    IntStream.concat(this.chars.codePoints(), chars.chars.codePoints() )
                );
            } else {
                return new Ranges(this, chars);
            }
        }

        @Override
        public Stream<BoundRange> asIntervals() {
            if (this.equal) { // case of inclusion
                if (this.chars.length() == 0) {
                    return Stream.empty();
                } else {
                    int[] codepoints = this.chars.codePoints().sorted().distinct().toArray();
                    // group consecutive characters to a range, and serve individual ones
                    Spliterator<BoundRange> iter = new AbstractSpliterator<BoundRange>(
                        codepoints.length,
                        DISTINCT | IMMUTABLE | NONNULL | ORDERED | SIZED)
                    {
                        int i = 0;
                        boolean end = false;
                        int firstOfGroup = -1;
                        int lastOfGroup = -1;

                        @Override
                        public boolean tryAdvance(Consumer<? super BoundRange> action) {
                            if (end && firstOfGroup == - 1) {
                                return false;
                            } // else not yet the end OR firstOfGroup not yet consumed
                            while (i < codepoints.length) {
                                int cp = codepoints[i++];
                                if (firstOfGroup == -1) { // first set
                                    firstOfGroup = cp;
                                    lastOfGroup = cp;
                                } else if (cp == lastOfGroup + 1) {
                                    lastOfGroup++; // expand the range
                                    continue;
                                } else {
                                    i--; // cp not consumed in this tryAdvance => reset it
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
                            firstOfGroup = - 1; // reinit
                            lastOfGroup = -1;
                            return true;
                        }
                    };
                    return StreamSupport.stream(iter, false);
                }
            } else { // case of exclusion
                if (this.chars.length() == 0) {
                    return Stream.of(ANY);
                } else {
                    // range inversion
                    return reverse(this.chars.codePoints());
                }
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
        public static Stream<BoundRange> reverse(IntStream chars) {
            int[] lower = { Character.MIN_CODE_POINT };
            // chars are n points, ranges are n+1 intervals
            return Stream.<BoundRange> concat(
                chars.sorted()
                    .distinct()
                    // filter out consecutive chars
                    .filter(c -> c == lower[0] ? lower[0]++ < 0 /*false*/ : true)
                    .mapToObj( c -> {
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
                                    // known at the end
                        };
                    })
            ).filter(r -> ! r.isEmpty());
                    // when consecutive chars with MIN_CP or MAX_CP are found
        }

        @Override
        public CharRange revert() {
            return new Chars(! this.equal, this.chars);
        }

    }

    /**
     * Define an atomic range of characters.
     *
     * @author Philippe Poulard
     */
    static class Range extends BoundRange {

        private int start;
        private int end;

        /**
         * Create a range.
         *
         * @param start The start codepoint (included)
         * @param end The end codepoint (included)
         */
        Range(int start, int end) {
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
        public boolean contains(int codepoint) {
            return codepoint >= start() && codepoint <= end();
        }

        @Override
        public Stream<BoundRange> asIntervals() {
            return Stream.of(this);
        }

        @Override
        public CharRange except(CharRange range) {
            return new Ranges(this).except(range);
        }

        public CharRange union(Range range) {
            if (range.start() == start() && range.end() == end()) {
                return this;
            } else if (range.end() < start() || range.start() > end()) {
                return super.union(range);
            } else {
                // extend a range
                return new Range(Math.min(range.start(), start()), Math.max(range.end(), end()));
            }
        }

        public CharRange union(Char car) {
            if (contains(car.car)) {
                return this;
            } else {
                return super.union(car);
            }
        }

        public CharRange union(Chars chars) {
            if (chars.chars.codePoints().allMatch(c -> contains(c))) {
                return this;
            } else {
                return super.union(chars);
            }
        }

        @Override
        public CharRange revert() {
            if (isEmpty()) {
                return ANY;
            } else if (this.start == Character.MIN_CODE_POINT) {
                if (this.end == Character.MAX_CODE_POINT) {
                    return EMPTY;
                } else if (this.end + 1 == Character.MAX_CODE_POINT) {
                    return new Char(true, Character.MAX_CODE_POINT);
                } else {
                    return new Range(this.end + 1, Character.MAX_CODE_POINT);
                }
            } else {
                if (this.end == Character.MAX_CODE_POINT) {
                    if (this.start == Character.MIN_CODE_POINT + 1) {
                        return new Char(true, Character.MIN_CODE_POINT);
                    } else {
                        return new Range(Character.MIN_CODE_POINT, this.start - 1);
                    }
                } else {
                    return new Range(Character.MIN_CODE_POINT, this.start - 1)
                    .union(new Range(this.end + 1, Character.MAX_CODE_POINT));
                }
            }
        }

    }

    /**
     * Define non-overlapping ranges of characters.
     *
     * @author Philippe Poulard
     */
    static class Ranges extends UnboundRange {

        // set of successive ranges ordered by start char
        TreeSet<BoundRange> ranges = new TreeSet<>();

        /**
         * Create a non-overlapping ranges of characters.
         *
         * @param ranges The ranges that compose this set may overlap themselves.
         */
        Ranges(CharRange... ranges) {
            this(Stream.of(ranges));
        }

        /**
         * Create a non-overlapping ranges of characters.
         *
         * @param ranges The ranges that compose this set may overlap themselves.
         */
        Ranges(Stream<CharRange> ranges) {
            ranges.flatMap(c -> c.asIntervals())
                .filter(r -> ! r.isEmpty())
                .sorted()
                .distinct()
                .forEach(r -> {
                    BoundRange exist = this.ranges.floor(r);
                    if (exist == null || exist.end() < r.start() || exist.start() > r.end()) {
                        this.ranges.add(r);
                    } else { // extend a range
                        this.ranges.remove(exist);
                        this.ranges.add(new Range(Math.min(exist.start(), r.start()), Math.max(exist.end(), r.end())));
                    }
                });
        }

        @Override
        public boolean contains(int codepoint) {
            BoundRange exist = this.ranges.floor(new Char(true, codepoint));
            return exist != null && exist.contains(codepoint);
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return this.ranges.stream()
                .collect(collectorOf(
                    "(", " | ", ")", buf,
                    bcr -> bcr.toPrettyString(buf))
            );
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.ranges);
        }

        @Override
        public Stream<BoundRange> asIntervals() {
            return this.ranges.stream();
        }

        @Override
        public CharRange union(CharRange range) {
            if (range.asIntervals().allMatch(r -> {
                    // 'r' is in the boundaries of an existing range ?
                    BoundRange exist = this.ranges.floor(r);
                    return exist != null && r.end() <= exist.end();
            }))
            { // unchanged
                return this;
            } else {
                return new Ranges(this, range);
            }
        }

        @Override
        public CharRange except(CharRange range) {
            return except(new CharRange[] { range });
        }

        @Override
        public CharRange except(CharRange... ranges) {
            List<BoundRange> boundRanges = Arrays.asList(ranges)
                .stream()
                .flatMap(c -> c.asIntervals())
                .filter(r -> ! r.isEmpty())
                .sorted()
                .distinct()
                .collect(Collectors.toList());
            if (boundRanges.stream()
                .anyMatch(exclude -> {
                    // 'exclude' is overlapping an existing range ?
                    BoundRange include = this.ranges.floor(exclude);
                    // that include element is before exclude element
                    if (include == null) {
                        return false;
                    } else if (include.end() >= exclude.start()) {
                        return true;
                    } else { // check the next
                        include = this.ranges.higher(include);
                        // that include element is after exclude element
                        return include != null && include.start() <= exclude.end();
                    }
            }))
            { // at least one exclusion has an impact on the ranges
                // => create an iterator that merges 2 sorted cursors
                Spliterator<BoundRange> iter = new AbstractSpliterator<BoundRange>(
                        Long.MAX_VALUE, DISTINCT | IMMUTABLE | NONNULL | ORDERED | SORTED )
                {
                    Spliterator<BoundRange> cursorInclude = asIntervals().sorted().distinct().spliterator();
                    BoundRange include; // current to include
                    Spliterator<BoundRange> cursorExclude;
                    BoundRange exclude; // current to exclude
                    boolean end = initInclusion(() -> {
                        // will be call only if we have something to include
                        cursorExclude = boundRanges.spliterator();
                        cursorExclude.tryAdvance(br -> exclude = br);
                        return false; // end = false
                    });
                    // first read
                    boolean initInclusion(Supplier<Boolean> initExclusion) {
                        return cursorInclude.tryAdvance(br -> include = br) ? initExclusion.get() : true;
                    }

                    // accept currentInclude and read the next
                    boolean includeCurrent(Consumer<? super BoundRange> action) {
                        action.accept(include);
                        // read next
                        end = ! cursorInclude.tryAdvance(br -> include = br);
                        if (end) {
                            include = null;
                        }
                        return true;
                    }

                    @Override
                    public Comparator<? super BoundRange> getComparator() {
                        return null; // mean : sort in natural order
                    }

                    @Override
                    public boolean tryAdvance(Consumer<? super BoundRange> action) {
                        // read items to include while reading items to exclude

                        // below : [                ] => Unicode range
                        // we                  IIIIII => include range
                        // have                EEEEEE => exclude range

                        while (true) {
                            if (end) { // no more to include
                                return false;
                            } else if (exclude == null) { // no more to exclude
                                // accept all
                                return includeCurrent(action);
                            } else // both are present
                            if (include.end() < exclude.start()) {
                                // exclusion not yet reached
                                //     [     IIIIII       ]
                                //     [             EEE  ]
                                // => accept IIIIII
                                return includeCurrent(action);
                            } else {
                                if (exclude.end() < include.start()) {
                                    // inclusion not yet reached
                                    //        [         IIIIII    ]
                                    //        [  EEEE             ]
                                    // => ignore EEEE
                                    if (! cursorExclude.tryAdvance(br -> exclude = br)) {
                                        exclude = null;
                                    }
                                    // loop
                                } else // IIIIII and EEEEE are overlapping ; let's find how...
                                if (exclude.start() <= include.start() && exclude.end() >= include.end()) {
                                    //    [      IIIIII      ]
                                    //    [  EEEEEEEEEEEEE   ]
                                    // ignore => IIIIII
                                    // read next...
                                    end = ! cursorInclude.tryAdvance(br -> include = br);
                                    // ...and loop
                                } else if (exclude.end() <= include.end()) {
                                    if (exclude.start() > include.start()) {
                                        //       [   IIIIIII    ]
                                        //       [     EE       ]
                                        // accept => II
                                        // next =>       III
                                        action.accept(range(include.start(), exclude.start() - 1));
                                        include = range(exclude.end() + 1, include.end());
                                        if (include.isEmpty()) {
                                            end = ! cursorInclude.tryAdvance(br -> include = br);
                                        }
                                        return true;
                                    } else {
                                        //   [       IIIIII   ]
                                        //   [   EEEEEEEE     ]
                                        // cut =>        II
                                        include = new Range(exclude.end() + 1, include.end());
                                        if (include.isEmpty()) {
                                            end = ! cursorInclude.tryAdvance(br -> include = br);
                                        }
                                        // loop
                                    }
                                } else {
                                    //     [     IIIIII       ]
                                    //     [       EEEEEEEE   ]
                                    // accept => II
                                    action.accept(new Range(include.start(), exclude.start() - 1));
                                    // read next
                                    end = ! cursorInclude.tryAdvance(br -> include = br);
                                    return true;
                                }
                            }
                        }
                    }
                };
                Ranges newRanges = new Ranges();
                StreamSupport.<BoundRange> stream(iter , false)
                    .filter(elem -> ! elem.isEmpty())
                    .forEach(elem -> newRanges.ranges.add(elem));
                if (newRanges.ranges.size() == 0) {
                    return EMPTY;
                } else if (newRanges.ranges.size() == 1) {
                    BoundRange r = newRanges.ranges.first();
                    if (r.start() == Character.MIN_CODE_POINT && r.end() == Character.MAX_CODE_POINT) {
                        return ANY;
                    } else {
                        return r;
                    }
                } else {
                    return newRanges;
                }
            } else { // unchanged
                return this;
            }
        }

    }

}
