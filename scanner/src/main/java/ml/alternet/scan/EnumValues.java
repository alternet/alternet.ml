package ml.alternet.scan;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ml.alternet.facet.Presentable;
import ml.alternet.misc.CharArray;
import ml.alternet.misc.Thrower;
import ml.alternet.util.StringBuilderUtil;

/**
 * Wraps an enum class or a list of string values to a hierarchy
 * of enum or string values, partitioned by characters (or common
 * sequence of characters).
 *
 * This allow to look for an enum or string value without testing
 * the same character several times (when it occurs).
 *
 * @author Philippe Poulard
 *
 * @param <T> The type of values : <code>String</code> or an enum class.
 *
 * @see EnumValues#nextValue(Scanner)
 * @see EnumValues#from(Class)
 * @see EnumValues#from(String...)
 * @see EnumValues#from(Stream)
 */
public class EnumValues<T> implements Readable<T>, Presentable { // is a set of EnumValues

    // the start index in the string value (val and values are complete strings, char may be partial)
    int start = 0;
    // a single terminal value
    T val;
    // a subset of values
    Set<EnumValues<T>> values;
    // the chars from index "start" common to all values
    CharArray chars;

    /**
     * Create a hierarchy of enum values by char.
     *
     * @param values The enum class.
     *
     * @return The enum values that holds all the hierarchy.
     */
    @SuppressWarnings({ "rawtypes" })
    public static EnumValues<? extends Enum> from(Class<? extends Enum> values) {
        Set<EnumValues<? extends Enum>> enumValues = Arrays.stream(values.getEnumConstants())
                .map(e -> new EnumValues<>(e))
                .collect(Collectors.toSet());
        // dispatch by chars, start at index 0
        @SuppressWarnings("unchecked")
        EnumValues<? extends Enum> result = new EnumValues<>((Set<EnumValues<?>>) (Object)
                                              enumValues); // odd cast to ensure to call the right constructor
        return result.dispatch(0);
    }

    /**
     * Create a hierarchy of enum values by char.
     *
     * @param values The list of string values.
     *
     * @return The enum values that holds all the hierarchy.
     */
    public static EnumValues<String> from(String... values) {
        return EnumValues.from(Arrays.stream(values));
    }

    /**
     * Create a hierarchy of enum values by char.
     *
     * @param values The stream of string values.
     *
     * @return The enum values that holds all the hierarchy.
     */
    public static EnumValues<String> from(Stream<String> values) {
        Set<EnumValues<String>> enumValues = values
                .map(e -> new EnumValues<>(e))
                .collect(Collectors.toSet());
        // dispatch by chars, start at index 0
        @SuppressWarnings("unchecked")
        EnumValues<String> result = new EnumValues<>((Set<EnumValues<?>>) (Object)
                                              enumValues); // odd cast to ensure to call the right constructor
        return result.dispatch(0);
    }

    // constructor for the root
    @SuppressWarnings("unchecked")
    EnumValues(Set<EnumValues<?>> enumValues) { // doesn't work with Set<EnumValues<T>>
        if (enumValues.size() == 1) {
            // single value => set the terminal value
            this.val = (T) enumValues.stream().findAny().get().val;
        } else {
            this.values = (Set<EnumValues<T>>) (Object) enumValues; // odd cast
        }
    }

    // constructor for terminal values
    EnumValues(T val) {
        // just wrap an enum value
        this.val = val;
    }

    // constructor for set of values to dispatch by char
    EnumValues(char c, Set<EnumValues<T>> enumValues, int start) {
        this.start = start; // start index of the character to take into account
        if (enumValues.size() == 1) {
            // single value => set the terminal value
            this.val = enumValues.stream().findAny().get().val;
        } else {
            this.values = enumValues;
            // all the values are starting by the same character
            this.chars = new CharArray(new char[1], 0, 0); // allocate a single char
            this.chars.append(c);
            // additional chars may be appended later
            // if it appears that the next chars are
            // also common to all values -> see push() below
        }
    }

    // push a char in the current char sequence
    void push(char c) {
        // additional characters are common to the set of values
        this.chars.append(c);
    }

    @Override
    public String toString() {
        return this.start == 0 ? "" : ("(" + this.start + ") ")
             + this.chars == null ? "" : ("\"" + this.chars + "\"=")
             + "\'" + (this.val == null ? "" : this.val) + "\'"
             + ("" + this.values == null ? "[]" : (this.values + ""));
    }

    // do we have the next chars from the input here ?
    boolean hasNextChars(Scanner scanner) {
        try {
            if (this.chars == null) {
                // we have the terminal value
                if (this.start == 0) {
                    return scanner.hasNextString(this.val.toString(), true);
                } else {
                    // when previous chars were already tested...
                    String nextChars = this.val.toString().substring(this.start);
                    // ...we check the rest of the sequence
                    return scanner.hasNextString(nextChars, true);
                }
            } else if (this.chars.length() == 1) {
                // we have the set of values with just a common char
                return scanner.lookAhead() == this.chars.charAt(0);
            } else {
                // we have the set of values with several common chars
                return scanner.hasNextString(this.chars, false);
                            // "false" because it is partial and consumed by the caller
            }
        } catch (IOException e) {
            return Thrower.doThrow(e);
        }
    }

    /**
     * Read the next enum value that belongs to this set of values.
     *
     * @param scanner The input to read.
     *
     * @return <code>empty</code> if not found in the input,
     *      the actual enum value otherwise.
     *
     * @throws IOException When an I/O error occurs.
     */
    @Override
    public Optional<T> nextValue(Scanner scanner) throws IOException {
        return nextValuePart(scanner, 0); // part is complete from 0
    }

    // look for the next enum value part in this (sub)tree at the given char pos
    Optional<T> nextValuePart(Scanner scanner, int charPos) throws IOException {
        Optional<T> result = Optional.empty();
        Optional<EnumValues<T>> value = this.values.stream()
            .filter(e -> e.hasNextChars(scanner))
            .findFirst(); // because we may find 1 or 0
        if (value.isPresent()) {
            EnumValues<T> enumValue = value.get();
            // try to find the longest match
            if (enumValue.values == null) {
                // we have it
                result = Optional.of(enumValue.val);
            } else {
                scanner.mark();
                // read as many chars as in the common chars (matched so far)...
                scanner.nextString(new StringConstraint.ReadLength(enumValue.chars.length()));
                // ...and go on parsing
                result = enumValue.nextValuePart(scanner, charPos + 1);
                if (result.isPresent()) {
                    scanner.consume();
                } else {
                    scanner.cancel();
                }
            }
            // if no longest match and a val is present use it
            if (! result.isPresent() && enumValue.val != null) {
                String strValue = enumValue.val.toString();
                if (strValue.length() == charPos // terminal value, all characters were consumed
                        || scanner.hasNextString(strValue.substring(charPos), true))
                {
                    result = Optional.of(enumValue.val);
                }
            }
        }
        return result;
    }

    // Dispatch enum value string by char at index.
    //
    // This method is called recursively at the
    // initialization phase.
    //
    // No longer used when using the scanner
    EnumValues<T> dispatch(int index) {
        if (this.values == null) {
            return this;
        } else if (this.values.size() == 1) {
            // a single enum for this set
            return this.values.stream().findAny().get();
        } else {
            Map<Boolean, Set<EnumValues<T>>> dispatchable;
            Map<Character, Set<EnumValues<T>>> enumByChar;
            boolean sameString;
            do {
                int i = index;
                dispatchable = this.values.stream().collect(
                    Collectors.partitioningBy(e -> e.val.toString().length() > i, Collectors.toSet())
                    // true -> can be dispatch
                    // false -> can't be dispatch (too small string) -> at most one item in this partition
                );
                enumByChar = dispatchable.get(true).stream()
                    .collect(
                        // map by char at index
                        Collectors.groupingBy(e -> e.val.toString().charAt(i),
                            TreeMap::new,
                            Collectors.toSet()
                    )
                );
                // if all values are starting by the same char
                // and everything was dispatched, we can push that char and loop
                sameString = enumByChar.size() == 1 && dispatchable.get(false).isEmpty();
                if (sameString) {
                    // forEach = forSingle
                    enumByChar.forEach((c, e) -> this.push(c));
                    index++;
                }
            } while (sameString); // exit => we have a clean separation by char / or not dispatchable
            int i = index;
            // try to dispatch recursively each set-by-char
            Set<EnumValues<T>> enumValues = enumByChar.entrySet().stream()
                //                            EnumValue<>(char,       Set<EnumValue>)
                .<EnumValues<T>> map(e -> new EnumValues<>(e.getKey(), e.getValue(), i))
                .map(e -> e.dispatch(i + 1)) // dispatch Set<EnumValue> for the next index
                .collect(Collectors.toSet());
            if (dispatchable.get(false).isEmpty()) {
                if (enumValues.size() == 1) {
                    // a single enum for this set
                    return enumValues.stream().findAny().get();
                }
            } else {
                // the non-dispatchable is the terminal value
                this.val = dispatchable.get(false).stream().findAny().get().val;
            }
            // it's a tree of enum by char
            this.values = enumValues; // replace by what was dispatched
            return this;
            // note that this.val and this.values can be set together
        }
    }

    @Override
    public StringBuilder toPrettyString(StringBuilder buf) {
        return values()
            .map(o -> "'" + o.toString() + "'")
            .sorted(Comparator.comparing(String::length).thenComparing(s -> s))
            .collect(StringBuilderUtil.collectorOf("( ", " | ", " )", buf));
    }

    private static int FLAGS =
              Spliterator.CONCURRENT | Spliterator.DISTINCT
            | Spliterator.IMMUTABLE | Spliterator.NONNULL
            | Spliterator.ORDERED;

    /**
     * Return all the values inside.
     *
     * @return The values, in alphabetical order.
     */
    public Stream<T> values() {
        // looks heavy, but it's lazy !
        return Stream.concat(
            // concat this.val if it exist...
            Optional.ofNullable(this.val)
                .map(Stream::of)
                .orElseGet(Stream::empty),
            // ...with this.values.forEach(EnumValues::values) if it exist
            StreamSupport.stream(
                new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, FLAGS) {
                    Spliterator<T> stream = Optional.ofNullable(EnumValues.this.values)
                        .map(values -> values.stream().flatMap(v -> v.values()))
                        .orElseGet(() -> Stream.<T>empty())
                        .spliterator();
                    @Override
                    public boolean tryAdvance(Consumer<? super T> action) {
                        return stream.tryAdvance(action);
                    }
                },
                false)
        );
    }

}
