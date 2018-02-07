package ml.alternet.scan;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Stream;

import ml.alternet.facet.Rewindable;
import ml.alternet.facet.Trackable;
import ml.alternet.io.IOUtil;
import ml.alternet.misc.CharRange;
import ml.alternet.misc.Position;
import ml.alternet.misc.Thrower;
import ml.alternet.util.NumberUtil;

/**
 * A scanner can read Unicode characters from an input
 * stream under conditions.
 *
 * <p>Convenient methods are available for
 * testing for example whether the next content
 * is a number or not, for reading characters,
 * strings and numbers, pick a value
 * from a set of possible strings or Enum values,
 * and even pick the next character if it belongs
 * to a range of characters (possibly built with
 * union and exclusions of other ranges).</p>
 *
 * <p>Additional classes can help to read an
 * object under constraints, for example "get
 * the next number that has less than 5 digits",
 * or the next number that fit in a specific type.</p>
 *
 * <p>It is a progressive scanner in the sense that
 * the next content is inherently independent of
 * what was read before. To hold a context and go
 * back when successive items doesn't satisfy a
 * specific grammar, the user will have to set markers.</p>
 *
 * <p>The {@link #mark()}, {@link #consume()}, and
 * {@link #cancel()} methods can help to read some
 * characters and go back to the marked position.
 * Several successive positions can be marked without
 * canceling or consuming the previous ones ;
 * as marks are stacked, it falls to the user
 * to apply the appropriate number of cancel + consume
 * calls.</p>
 *
 * <p>A specific device is available for single Unicode
 * characters :
 * just use {@link #hasNextChar(int, boolean)},
 * {@link #hasNextChar(String, boolean)}, or get it simply
 * with {@link #lookAhead()} ; you don't need to set a mark
 * for testing the next character to read.</p>
 *
 * <p>It is markable-reentrant that is to say
 * that once marked, it can used itself for further
 * scanning with or without setting new marks.</p>
 *
 * @author Philippe Poulard
 */
public abstract class Scanner implements Trackable, Rewindable {

    /**
     * Scans a string.
     *
     * @param input The input.
     * @return The scanner
     */
    public static Scanner of(CharSequence input) {
        return Thrower.safeCall(() -> new StringScanner(input));
    }

    /**
     * Scans a reader.
     *
     * @param input The input.
     * @return The scanner
     *
     * @throws IOException When an I/O error occur.
     * @throws IllegalArgumentException When the marks aren't supported.
     */
    public static Scanner of(Reader input) throws IllegalArgumentException, IOException {
        return new ReaderScanner(input);
    }

    /**
     * Wraps this scanner in a trackable scanner.
     *
     * @return A trackable scanner.
     *
     * @throws IOException When an I/O error occur.
     */
    public TrackableScanner asTrackable() throws IOException {
        return new TrackableScanner(this);
    }

    @Override
    public Optional<Position> getPosition() {
        return Optional.empty();
    }

    // just wraps the mark // see TrackableScanner.Position
    static class Cursor {

        int mark;

        Cursor(int cursor) {
            this.mark = cursor;
        }

        @Override
        public String toString() {
            return "" + this.mark;
        }
    }

    static class State {

        State(Scanner scanner) {
            this.source = scanner;
        }

        Scanner source; // see TrackableScanner

        /** The cursors stacked after successive logical marks. */
        Stack<Cursor> cursors = new Stack<>();

        /** The current position from the unique physical mark. */
        protected int cursor;

        /** The next char to read, always available (except when the end is reached). */
        protected int next; // Unicode char
        /** Indicates that the end of the stream has been reached. */
        protected boolean end = false;
        /** The index of the source available after parsing items under constraint. */
        int sourceIndex = 0;

        @Override
        public String toString() {
            return (this.end ? "{END} " : '\'' + new String(Character.toChars(this.next)) + "' ")
                    + this.cursor + '/' + this.cursors;
        }
    }

    // the source is by default the scanner itself, but it can be delegate
    // (on demand) to a trackable scanner
    State state = new State(this);

    /**
     * Append the next string in the given buffer.
     *
     * <p>The string stops at the first separator specified
     * by the stop condition of the constraint or at the
     * end of the stream. Each character to append is
     * filtered by the constraint (for example to allow
     * escaping characters).</p>
     *
     * <p>The characters involved in the stop condition
     * can be preserved (they will be the next
     * characters to read) or not according to the
     * stop condition.</p>
     * <p>WARNING: It is the responsibility
     * of the {@link StringConstraint#stopCondition(int, int, Scanner)}
     * method to mark and reset the scanner if necessary.</p>
     *
     * <p>In any case the characters involved in the stop condition are not
     * appended to the buffer.</p>
     *
     * @param constraint The non-<code>null</code> constraint that the
     *         string to read has to satisfy.
     * @param buf The buffer to which the next string will be appended.
     *
     * @return The number of characters appended to the buffer.
     *
     * @throws IOException When an I/O error occur.
     */
    public int nextString( StringConstraint constraint, StringBuilder buf ) throws IOException {
        int targetLength = 0;
        this.state.sourceIndex = 0;
        while ( ! this.state.end && ! constraint.stopCondition( this.state.sourceIndex, targetLength, this ) ) {
            this.state.sourceIndex++;
            targetLength += constraint.append( this.state.sourceIndex, targetLength, this, buf );
            this.state.source.read();
        }
        return targetLength;
    }

    /**
     * Skip the next string (under constraint).
     *
     * <p>The string stops at the first separator specified
     * by the stop condition of the constraint or at the
     * end of the stream.</p>
     *
     * <p>The characters involved in the stop condition
     * can be preserved (they will be the next
     * characters to read) or not according to the
     * stop condition.</p>
     * <p>WARNING: It is the responsibility
     * of the {@link StringConstraint#stopCondition(int, int, Scanner)}
     * method to mark and reset the scanner if necessary.</p>
     *
     * @param constraint The constraints to apply on the string.
     *
     * @return The number of characters read, excluding those
     *         involved in the stop condition.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #nextString(StringConstraint, StringBuilder)
     */
    public int nextString( StringConstraint constraint ) throws IOException {
        this.state.sourceIndex = 0;
        while ( ! this.state.end && ! constraint.stopCondition( this.state.sourceIndex, 0, this ) ) {
            this.state.sourceIndex++;
            this.state.source.read();
        }
        return this.state.sourceIndex;
    }

    /**
     * Return the next Unicode character to read without advancing
     * the cursor.
     *
     * <p>It is a convenient method that avoids to use
     * a marker for reading a single char.</p>
     *
     * @return The next character if any, or EOF.
     *
     * @see #nextChar()
     */
    public int lookAhead() {
        return this.state.next;
    }

    /**
     * Read the next character.
     *
     * Similar to :
     * <pre>int car = scanner.lookAhead();
     *scanner.read();</pre>
     *
     * @return The next character if any, or EOF.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #lookAhead()
     * @see #read()
     */
    public int nextChar() throws IOException {
        if ( this.state.end ) {
            return IOUtil.EOF;
        } else {
            int c = this.state.next;
            this.state.source.read();
            return c;
        }
    }

    /**
     * Read the next number.
     *
     * @return The next number read, or <code>null</code> if none
     *         found. The type of the number will be the more
     *         suitable.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #nextNumber(NumberConstraint)
     */
    public Number nextNumber() throws IOException {
        return nextNumber( NumberConstraint.NO_CONSTRAINT );
    }

    /**
     * Read the next number.
     *
     * @param constraint The non-<code>null</code> constraint that the
     *         number to read has to satisfy.
     *
     * @return The next number read, or <code>null</code> if none
     *         found under the constraints.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see NumberUtil#parseNumber(String, boolean, Class)
     */
    public Number nextNumber( NumberConstraint constraint ) throws IOException {
        StringBuffer buf = new StringBuffer();
        this.state.source.mark();
        boolean isFloatingPoint = parseNumber( buf, constraint );
        // now, the buffer contains the expected number as a string
        // just return the right class
        if ( buf.length() > 0 ) {
            try {
                String number = buf.toString();
                Number n = NumberUtil.parseNumber(
                    number,
                    isFloatingPoint,
                    constraint.getNumberType()
                );
                this.state.source.consume();
                return n;
            } catch ( NumberFormatException nfe ) {
                // ooops !
                this.state.source.cancel();
                return null;
            }
        } else {
            this.state.source.cancel();
            // no number found
            return null;
        }
    }

    /**
     * Parse a number under constraint.
     *
     * @param buf The target buffer to fill with the number.
     *         '+' and leading zeroes are discarded.
     * @param constraint The non-<code>null</code> constraint that the
     *         number to read has to satisfy.
     *
     * @return <code>true</code> if the parsed number is a floating number,
     *         <code>false</code> otherwise.
     *
     * @throws IOException When the scanner fails to read the input.
     */
    private boolean parseNumber( StringBuffer buf, NumberConstraint constraint ) throws IOException {
// TODO :
//        Class clazz = constraint.getClass();
//        if ( clazz != null ) {
//            constraint = new CompositeNumberConstraint( constraint, clazz );
//        }
        this.state.sourceIndex = 0;
        int length = 0;
        int dotIndex = -1;
        int exponentIndex = -1;
        if ( constraint.stopCondition(buf, this.state.sourceIndex, dotIndex, exponentIndex, this) ) {
            return false;
        }
        char prev = (char) -1;
        boolean isFloatingPoint = false;
        char sign = (char) nextChar( "-+", true );
        switch ( sign ) {
        case '-':
            buf.append( sign );
            length++;
            this.state.sourceIndex++;
        case '+':
            prev = sign;
        }
        if ( constraint.stopCondition(buf, this.state.sourceIndex, dotIndex, exponentIndex, this) ) {
            return isFloatingPoint;
        }
        for ( int c = lookAhead() ; c >= '0' && c <= '9' ; c = lookAhead() ) {
            // skip leading zeroes
            // FIXME : wouldn't it be better to count leading zeroes and pass them to the constraint ?
            if ( prev == '0' && ( length == 1 || (length == 2 && sign == '-' ) ) ) {
                buf.setCharAt( length - 1, (char) c ); // c is a BMP codepoint
            } else {
                length++;
                buf.append( (char) c );
            }
            this.state.source.read();
            this.state.sourceIndex++;
            if ( constraint.stopCondition(buf, this.state.sourceIndex, dotIndex, exponentIndex, this) ) {
                return isFloatingPoint;
            }
            prev = (char) c;
        }
        if ( constraint.stopCondition(buf, this.state.sourceIndex, dotIndex, exponentIndex, this) ) {
            return isFloatingPoint;
        }
        if ( hasNextChar( '.', true ) ) {
            // FIXME : set the flag only when a non-zero digit is encountered ?
            isFloatingPoint = true;
            buf.append('.');
            this.state.sourceIndex++;
            dotIndex = this.state.sourceIndex;
            if ( constraint.stopCondition(buf, this.state.sourceIndex, dotIndex, exponentIndex, this) ) {
                return isFloatingPoint;
            }
            for ( int c = lookAhead() ; c >= '0' && c <= '9' ; c = lookAhead() ) {
                length++;
                buf.append( (char) c ); // c is a BMP codepoint
                this.state.source.read();
                this.state.sourceIndex++;
                if ( constraint.stopCondition(buf, this.state.sourceIndex, dotIndex, exponentIndex, this) ) {
                    return isFloatingPoint;
                }
            }
        }
        if ( constraint.stopCondition(buf, this.state.sourceIndex, dotIndex, exponentIndex, this) ) {
            return isFloatingPoint;
        }
        if ( hasNextChar( "eE", true ) ) {
            isFloatingPoint = true;
            buf.append('e');
            this.state.sourceIndex++;
            exponentIndex = this.state.sourceIndex;
            sign = (char) nextChar( "-+", true );
            if ( constraint.stopCondition(buf, this.state.sourceIndex, dotIndex, exponentIndex, this) ) {
                return isFloatingPoint;
            }
            if ( sign != IOUtil.EOF ) {
                buf.append( sign );
                this.state.sourceIndex++;
            }
            if ( constraint.stopCondition(buf, this.state.sourceIndex, dotIndex, exponentIndex, this) ) {
                return isFloatingPoint;
            }
            for ( int c = lookAhead() ; c >= '0' && c <= '9' ; c = lookAhead() ) {
                buf.append( (char) c ); // c is a BMP codepoint
                this.state.source.read();
                this.state.sourceIndex++;
                if ( constraint.stopCondition(buf, this.state.sourceIndex, dotIndex, exponentIndex, this) ) {
                    return isFloatingPoint;
                }
            }
        }
        return isFloatingPoint;
    }

    /**
     * Read the next enum value.
     *
     * If the same enum class have to be used several times,
     * prefer using {@link #nextEnumValue(EnumValues)}.
     *
     * @see EnumValues#from(Class)
     *
     * @param values The set of possible enum values.
     *
     * @return The next enum value wrapped in an optional.
     *
     * @param <T> The enum type.
     *
     * @throws IOException When an I/O error occur.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> Optional<T> nextEnumValue( Class<? extends Enum> values ) throws IOException {
        return (Optional<T>) EnumValues.from(values).nextValue(this);
    }

    /**
     * Read the next enum value.
     *
     * @param values The set of possible enum values.
     *
     * @return The next enum value wrapped in an optional.
     *
     * @param <T> The enum type.
     *
     * @throws IOException When an I/O error occur.
     */
    public <T> Optional<T>  nextEnumValue( EnumValues<T> values ) throws IOException {
        return values.nextValue(this);
    }

    /**
     * Read the next Unicode character that belongs to ranges of characters.
     *
     * @param range The range of possible characters.
     *
     * @return <code>empty</code> if the next character was not found in the input,
     *      the actual Unicode codepoint otherwise.
     *
     * @throws IOException When an I/O error occur.
     */
    public Optional<Integer> nextChar( CharRange range ) throws IOException {
        int c = lookAhead();
        if (range.contains(c)) {
            read(); // consume
            return Optional.of(c);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Read the next enum value.
     *
     * If the same enum values have to be used several times,
     * prefer using {@link #nextEnumString(EnumValues)}.
     *
     * @see EnumValues#from(String...)
     * @see EnumValues#from(Stream)
     *
     * @param values The set of possible string values.
     *
     * @return The next string value wrapped in an optional.
     *
     * @throws IOException When an I/O error occur.
     */
    public Optional<String> nextEnumString( String... values ) throws IOException {
        return EnumValues.from(values).nextValue(this);
    }

    /**
     * Read the next enum value.
     *
     * If the same enum values have to be used several times,
     * prefer using {@link #nextEnumString(EnumValues)}.
     *
     * @see EnumValues#from(String...)
     * @see EnumValues#from(Stream)
     *
     * @param values The stream of possible string values.
     *
     * @return The next string value wrapped in an optional.
     *
     * @throws IOException When an I/O error occur.
     */
    public Optional<String> nextEnumString( Stream<String> values ) throws IOException {
        return EnumValues.from(values).nextValue(this);
    }

    /**
     * Read the next enum value.
     *
     * @param values The set of possible string values.
     *
     * @return The next string value wrapped in an optional.
     *
     * @throws IOException When an I/O error occur.
     */
    public Optional<String> nextEnumString( EnumValues<String> values ) throws IOException {
        return values.nextValue(this);
    }

    /**
     * Indicates whether there remains characters to
     * read in the input.
     *
     * @return <code>true</code> if at least 1 character
     *         can be read, <code>false</code> if the end of
     *         the input stream was reached.
     *
     * @see #getPosition()
     */
    public boolean hasNext() {
        return ! this.state.end;
    }

    /**
     * Return the index in the source after parsing successfully
     * an item under constraint.
     * Indicates the number of characters involved for building
     * the item.
     *
     * @return The number of characters actually read in the
     *         source for parsing the last item.
     *
     * @see #nextNumber()
     * @see #nextNumber(NumberConstraint)
     * @see #nextString(StringConstraint)
     * @see #nextString(StringConstraint, StringBuilder)
     */
    public int getSourceIndex() {
        return this.state.sourceIndex;
    }

    /**
     * Indicates whether the next character in the input
     * is the given character. When found it can be consumed
     * in the source or not.
     *
     * @param c The character to test.
     * @param consume <code>true</code> if the character found have to be
     *         consumed, <code>false</code> otherwise.
     *
     * @return <code>true</code> if the next character in the input
     *        matches the one given, <code>false</code> otherwise.
     *
     * @throws IOException When an I/O error occur.
     */
    public boolean hasNextChar(int c, boolean consume) throws IOException {
        if ( this.state.next == c && ! this.state.end ) {
            if ( consume ) {
                this.state.source.read();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Indicates whether the next character in the input
     * is one of the given characters. When found it can be consumed
     * in the source or not.
     *
     * @param chars The string that contains the characters to test.
     * @param consume <code>true</code> if the character found have to be
     *         consumed, <code>false</code> otherwise.
     *
     * @return <code>true</code> if the next character in the input
     *        matches one of those given, <code>false</code> otherwise.
     *
     * @throws IOException When an I/O error occur.
     */
    public boolean hasNextChar(String chars, boolean consume) throws IOException {
        if ( ! this.state.end && chars.indexOf(this.state.next) >= 0) {
            if ( consume ) {
                this.state.source.read();
            }
            return true;
        }
        return false;
    }

    /**
     * Return the next character in the input that is one of the given
     * characters. When found it can be consumed in the source or not.
     *
     * @param chars The string that contains the characters to test.
     * @param consume <code>true</code> if the character found have to be
     *         consumed, <code>false</code> otherwise.
     *
     * @return the next character in the input if it matches one of those
     *         given, <code>(char) -1</code> otherwise.
     *
     * @throws IOException When an I/O error occur.
     */
    public int nextChar(String chars, boolean consume) throws IOException {
        if ( ! this.state.end && chars.indexOf(this.state.next) >= 0) {
            int c = this.state.next;
            if ( consume ) {
                this.state.source.read();
            }
            return c;
        }
        return IOUtil.EOF;
    }

    /**
     * Test whether or not the next string in the input is those given.
     * When found it can be consumed or not.
     *
     * @param string The string to test.
     * @param consume <code>true</code> if the string found have to be
     *         consumed, <code>false</code> otherwise.
     *
     * @return <code>true</code> if the string matches the input,
     *         <code>false</code> otherwise.
     *
     * @throws IOException When an I/O error occur.
     */
    public boolean hasNextString(CharSequence string, boolean consume) throws IOException {
        if ( string == null ) {
            return true;
        } else if ( this.state.end ) {
            return false;
        } else {
            int len = string.length();
            if ( len == 0 ) {
                return true;
            } else {
                int c = this.state.next;
                if ( c == codePointAt(string, 0) ) {
                    int start = Character.charCount(c);
                    if ( len > start ) {
                        this.state.source.mark();
                        for ( int i = start ; i < len ; i++ ) {
                            this.state.source.read();
                            if ( this.state.end ) {
                                this.state.source.cancel();
                                return false;
                            }
                            c = codePointAt(string, i);
                            if ( c != this.state.next ) {
                                this.state.source.cancel();
                                return false;
                            } else if (Character.isSupplementaryCodePoint(c)) {
                                i++;
                            }
                        }
                        if ( consume ) {
                            this.state.source.consume();
                            this.state.source.read();
                        } else {
                            this.state.source.cancel();
                        }
                    } else {
                        if ( consume ) {
                            this.state.source.read();
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * Read the next Unicode character.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #hasNextChar(int, boolean)
     * @see #nextChar()
     * @see #lookAhead()
     */
    public abstract void read() throws IOException;

    /**
     * Mark the present position in the stream.
     *
     * <p>This method can be called safely several times.</p>
     *
     * <p>For each mark set, there should be sooner or later
     * a consume or cancel.</p>
     *
     * <p>Some convenient methods are available for getting a single
     * character without using a mark :
     * {@link #lookAhead()}, {@link #nextChar()}, {@link #hasNextChar(int, boolean)}
     * and {@link #hasNextChar(String, boolean)}.</p>
     *
     * @see Reader#mark(int)
     * @see #cancel()
     * @see #consume()
     */
    @Override
    public abstract void mark();

    /**
     * Cancel the characters read since the last marked position
     * (the next read will start from the last marked position).
     *
     * @throws IllegalStateException When this method is called
     *                     whereas no position was marked so far.
     *
     * @see #mark()
     */
    @Override
    public abstract void cancel() throws IllegalStateException;

    /**
     * Consume the characters read so far.
     *
     * <p>This implies that the last marked position is removed
     * and the next read goes on from the current position.
     * If there wasn't other marker, it will be impossible
     * to go back. If there was at least another one marker,
     * it can be itself cancelled or consumed independently.</p>
     *
     * @throws IllegalStateException When this method is called
     *                     whereas no position was marked so far.
     *
     * @see #mark()
     */
    @Override
    public abstract void consume() throws IllegalStateException;

    /**
     *  Push a cursor in the stack.
     *
     *  @param cursor The cursor value to push.
     */
    protected void push( int cursor ) {
        this.state.cursors.push(new Cursor(cursor));
    }

    /**
     * Pop last cursor.
     *
     * @return The last cursor.
     */
    protected int pop() {
        if (this.state.cursors.isEmpty()) {
            throw new RuntimeException( "Unbalanced stack." );
        } else {
            return this.state.cursors.pop().mark;
        }
    }

    /**
     * Peek last cursor.
     *
     * @return The last cursor.
     */
    protected int peek() {
        return this.state.cursors.peek().mark;
    }

    /**
     * Return the remainder to read from the
     * current position.
     *
     * @return The remainder to read
     *         if the end was not reached.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #getRemainderString()
     */
    public abstract Optional<Reader> getRemainder() throws IOException;

    /**
     * Return the remainder to read from the
     * current position.
     *
     * @return The remainder to read
     *         if the end was not reached.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #getRemainder()
     */
    public abstract Optional<String> getRemainderString() throws IOException;

    static int codePointAt(CharSequence string, int index) {
        char c1 = string.charAt(index++);
        if (Character.isHighSurrogate(c1)) {
            char c2 = string.charAt(index);
            if (Character.isLowSurrogate(c2)) {
                return Character.toCodePoint(c1, c2);
            }
        }
        return c1;
    }

}
