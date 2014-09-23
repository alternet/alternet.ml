package ml.alternet.scan;

import java.io.IOException;
import java.io.Reader;

import ml.alternet.util.IOUtil;
import ml.alternet.util.NumberUtil;

/**
 * A scanner can read characters from an input
 * stream under conditions.
 *
 * <p>Convenient methods are available for
 * testing for example whether the next content
 * is a number or not, for reading characters,
 * strings and numbers.</p>
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
 * <p>The {@link #mark(int)}, {@link #consume()}, and
 * {@link #cancel()} methods can help to read some
 * characters and go back to the marked position.
 * Several successive positions can be marked without
 * canceling or consuming the previous ones ;
 * as marks are stacked, it falls to the user
 * to apply the appropriate number of cancel + consume
 * calls.</p>
 *
 * <p>A specific device is available for single characters :
 * just use {@link #hasNextChar(char, boolean)},
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
public abstract class Scanner {

    /** The cursors stacked after successive logical marks. */
    private int[] cursors = new int[ 16 ];
    /** The position in the stack. */
    protected int pos = 0;
    /** The current position from the unique physical mark. */
    protected int cursor;

    /** The next char to read, always available (except when the end is reached). */
    protected char next;
    /** Indicates that the end of the stream has been reached. */
    protected boolean end = false;
    /** The index of the source available after parsing items under constraint. */
    int sourceIndex = 0;

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
     * 		string to read has to satisfy.
     * @param buf The buffer to which the next string will be appended.
     *
     * @return The number of characters appended to the buffer.
     *
     * @throws IOException When an I/O error occur.
     */
    public int nextString( StringConstraint constraint, StringBuilder buf ) throws IOException {
        int targetLength = 0;
        sourceIndex = 0;
        while ( ! this.end && ! constraint.stopCondition( sourceIndex, targetLength, this ) ) {
            sourceIndex++;
            targetLength += constraint.append( sourceIndex, targetLength, this, buf );
            read();
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
     * 		involved in the stop condition.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #nextString(StringConstraint, StringBuilder)
     */
    public int nextString( StringConstraint constraint ) throws IOException {
        sourceIndex = 0;
        while ( ! this.end && ! constraint.stopCondition( sourceIndex, 0, this ) ) {
            sourceIndex++;
            read();
        }
        return sourceIndex;
    }

    /**
     * Return the next character to read without advancing the cursor.
     *
     * <p>It is a convenient method that avoids to use
     * a marker for reading a single char.</p>
     *
     * @return The next character if any, or EOF.
     *
     * @see #nextChar()
     */
    public char lookAhead() {
        return this.next;
    }

    /**
     * Read the next character.
     *
     * @return The next character if any, or EOF.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #lookAhead()
     */
    public char nextChar() throws IOException {
        if ( this.end ) {
            return IOUtil.EOF;
        } else {
            char c = this.next;
            read();
            return c;
        }
    }

    /**
     * Read the next number.
     *
     * @return The next number read, or <code>null</code> if none
     * 		found. The type of the number will be the more
     * 		suitable.
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
     * 		number to read has to satisfy.
     *
     * @return The next number read, or <code>null</code> if none
     * 		found under the constraints.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see NumberUtil#parseNumber(String, boolean, Class)
     */
    public Number nextNumber( NumberConstraint constraint ) throws IOException {
        StringBuffer buf = new StringBuffer();
        mark( 2048 );
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
                consume();
                return n;
            } catch ( NumberFormatException nfe ) {
                // ooops !
                cancel();
                return null;
            }
        } else {
            cancel();
            // no number found
            return null;
        }
    }

    /**
     * Parse a number under constraint.
     *
     * @param buf The target buffer to fill with the number.
     * 		'+' and leading zeroes are discarded.
     * @param constraint The non-<code>null</code> constraint that the
     * 		number to read has to satisfy.
     *
     * @return <code>true</code> if the parsed number is a floating number,
     * 		<code>false</code> otherwise.
     *
     * @throws IOException When the scanner fails to read the input.
     */
    private boolean parseNumber( StringBuffer buf, NumberConstraint constraint ) throws IOException {
// TODO :
//        Class clazz = constraint.getClass();
//        if ( clazz != null ) {
//            constraint = new CompositeNumberConstraint( constraint, clazz );
//        }
        sourceIndex = 0;
        int length = 0;
        int dotIndex = -1;
        int exponentIndex = -1;
        if ( constraint.stopCondition(buf, sourceIndex, dotIndex, exponentIndex, this) ) {
            return false;
        }
        char prev = (char) -1;
        boolean isFloatingPoint = false;
        char sign = nextChar( "-+", true );
        switch ( sign ) {
        case '-':
            buf.append( sign );
            length++;
            sourceIndex++;
        case '+':
            prev = sign;
        }
        if ( constraint.stopCondition(buf, sourceIndex, dotIndex, exponentIndex, this) ) {
            return isFloatingPoint;
        }
        for ( char c = lookAhead() ; c >= '0' && c <= '9' ; c = lookAhead() ) {
            // skip leading zeroes
            // FIXME : wouldn't it be better to count leading zeroes and pass them to the constraint ?
            if ( prev == '0' && ( length == 1 || (length == 2 && sign == '-' ) ) ) {
                buf.setCharAt( length - 1, c );
            } else {
                length++;
                buf.append( c );
            }
            read();
            sourceIndex++;
            if ( constraint.stopCondition(buf, sourceIndex, dotIndex, exponentIndex, this) ) {
                return isFloatingPoint;
            }
            prev = c;
        }
        if ( constraint.stopCondition(buf, sourceIndex, dotIndex, exponentIndex, this) ) {
            return isFloatingPoint;
        }
        if ( hasNextChar( '.', true ) ) {
            // FIXME : set the flag only when a non-zero digit is encountered ?
            isFloatingPoint = true;
            buf.append('.');
            sourceIndex++;
            dotIndex = sourceIndex;
            if ( constraint.stopCondition(buf, sourceIndex, dotIndex, exponentIndex, this) ) {
                return isFloatingPoint;
            }
            for ( char c= lookAhead() ; c >= '0' && c <= '9' ; c = lookAhead() ) {
                length++;
                buf.append( c );
                read();
                sourceIndex++;
                if ( constraint.stopCondition(buf, sourceIndex, dotIndex, exponentIndex, this) ) {
                    return isFloatingPoint;
                }
            }
        }
        if ( constraint.stopCondition(buf, sourceIndex, dotIndex, exponentIndex, this) ) {
            return isFloatingPoint;
        }
        if ( hasNextChar( "eE", true ) ) {
            isFloatingPoint = true;
            buf.append('e');
            sourceIndex++;
            exponentIndex = sourceIndex;
            sign = nextChar( "-+", true );
            if ( constraint.stopCondition(buf, sourceIndex, dotIndex, exponentIndex, this) ) {
                return isFloatingPoint;
            }
            if ( sign != IOUtil.EOF ) {
                buf.append( sign );
                sourceIndex++;
            }
            if ( constraint.stopCondition(buf, sourceIndex, dotIndex, exponentIndex, this) ) {
                return isFloatingPoint;
            }
            for ( char c= lookAhead() ; c >= '0' && c <= '9' ; c = lookAhead() ) {
                buf.append( c );
                read();
                sourceIndex++;
                if ( constraint.stopCondition(buf, sourceIndex, dotIndex, exponentIndex, this) ) {
                    return isFloatingPoint;
                }
            }
        }
        return isFloatingPoint;
    }

    /**
     * Read the next object.
     *
     * @param constraint The non-<code>null</code> constraint that the
     * 		object to read has to satisfy.
     *
     * @return The next object read wrapped in a user data which length
     * 		is the number of characters read, excluding those
     * 		involved in the stop condition. Once unwrapped, the
     * 		target object can be <code>null</code>.
     *
     * @throws IOException When an I/O error occur.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public UserData nextObject( DataConstraint constraint ) throws IOException {
        sourceIndex = 0;
        UserData userData = new UserData();
        while ( ! this.end && ! constraint.stopCondition( sourceIndex, userData, this ) ) {
            sourceIndex++;
            read();
        }
        userData.setLength( sourceIndex );
        return userData;
    }

    /**
     * Indicates whether there remains characters to
     * read in the input.
     *
     * @return <code>true</code> if at least 1 character
     * 		can be read, <code>false</code> if the end of
     * 		the input stream was reached.
     *
     * @see #getPosition()
     */
    public boolean hasNext() {
        return ! this.end;
    }

    /**
     * Return the current position from the unique physical mark.
     *
     * @return The number of characters actually read from the mark.
     *
     * @see #hasNext()
     */
    public int getPosition() {
        return this.cursor - 1;
    }

    /**
     * Return the index in the source after parsing successfully
     * an item under constraint.
     * Indicates the number of characters involved for building
     * the item.
     *
     * @return The number of characters actually read in the
     * 		source for parsing the last item.
     *
     * @see #nextNumber()
     * @see #nextNumber(NumberConstraint)
     * @see #nextObject(DataConstraint)
     * @see #nextString(StringConstraint)
     * @see #nextString(StringConstraint, StringBuilder)
     */
    public int getSourceIndex() {
        return this.sourceIndex;
    }

    /**
     * Indicates whether the next character in the input
     * is the given character. When found it can be consumed
     * in the source or not.
     *
     * @param c The character to test.
     * @param consume <code>true</code> if the character found have to be
     * 		consumed, <code>false</code> otherwise.
     *
     * @return <code>true</code> if the next character in the input
     *		matches the one given, <code>false</code> otherwise.
     *
     * @throws IOException When an I/O error occur.
     */
    public boolean hasNextChar(char c, boolean consume) throws IOException {
        if ( this.next == c && ! this.end ) {
            if ( consume ) {
                read();
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
     * 		consumed, <code>false</code> otherwise.
     *
     * @return <code>true</code> if the next character in the input
     *		matches one of those given, <code>false</code> otherwise.
     *
     * @throws IOException When an I/O error occur.
     */
    public boolean hasNextChar(String chars, boolean consume) throws IOException {
        if ( ! this.end ) {
            if (chars.indexOf(this.next) >= 0) {
                if ( consume ) {
                    read();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Return the next character in the input that is one of the given
     * characters. When found it can be consumed in the source or not.
     *
     * @param chars The string that contains the characters to test.
     * @param consume <code>true</code> if the character found have to be
     * 		consumed, <code>false</code> otherwise.
     *
     * @return the next character in the input if it matches one of those
     * 		given, <code>(char) -1</code> otherwise.
     *
     * @throws IOException When an I/O error occur.
     */
    public char nextChar(String chars, boolean consume) throws IOException {
        if ( ! this.end ) {
            if (chars.indexOf(this.next) >= 0) {
                char c = this.next;
                if ( consume ) {
                    read();
                }
                return c;
            }
        }
        return IOUtil.EOF;
    }

    /**
     * Test whether or not the next string in the input is those given.
     * When found it can be consumed or not.
     *
     * @param string The string to test.
     * @param consume <code>true</code> if the string found have to be
     * 		consumed, <code>false</code> otherwise.
     *
     * @return <code>true</code> if the string matches the input,
     * 		<code>false</code> otherwise.
     *
     * @throws IOException When an I/O error occur.
     */
    public boolean hasNextString(String string, boolean consume) throws IOException {
        if ( string == null ) {
            return true;
        } else if ( this.end ) {
            return false;
        }else {
            int len = string.length();
            if ( len == 0 ) {
                return true;
            } else {
                char c = this.next;
                if ( c == string.charAt(0) ) {
                    if ( len > 1 ) {
                        mark( len + 1 );
                        for ( int i = 1 ; i < len ; i++ ) {
                            read();
                            if ( this.end || string.charAt( i ) != this.next ) {
                                cancel();
                                return false;
                            }
                        }
                        if ( consume ) {
                            consume();
                            read();
                        } else {
                            cancel();
                        }
                    } else {
                        if ( consume ) {
                            read();
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
     * Read the next character.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #hasNextChar(char, boolean)
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
     * {@link #lookAhead()}, {@link #nextChar()}, {@link #hasNextChar(char, boolean)}
     * and {@link #hasNextChar(String, boolean)}.</p>
     *
     * @param limit The maximum number of characters
     * 		that can be read before loosing the mark.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see Reader#mark(int)
     * @see #cancel()
     * @see #consume()
     */
    public abstract void mark( int limit ) throws IOException;

    /**
     * Cancel the characters read since the last marked position
     * (the next read will start from the last marked position).
     *
     * @throws IOException When an I/O error occur.
     * @throws IllegalStateException When this method is called
     * 					whereas no position was marked so far.
     *
     * @see #mark(int)
     */
    public abstract void cancel() throws IOException;

    /**
     * Consume the characters read so far.
     *
     * <p>This implies that the last marked position is removed
     * and the next read goes on from the current position.
     * If there wasn't other marker, it will be impossible
     * to go back. If there was at least another one marker,
     * it can be itself cancelled or consumed independently.</p>
     *
     * @throws IOException When an I/O error occur.
     * @throws IllegalStateException When this method is called
     * 					whereas no position was marked so far.
     *
     * @see #mark(int)
     */
    public abstract void consume() throws IOException;

    /**
     *  Push a cursor in the stack.
     *
     *  @param cursor The cursor value to push.
     */
    final protected void push( int cursor ) {
        if ( this.pos >= this.cursors.length ) {
            int[] newstack = new int[ this.cursors.length << 1 ];
            System.arraycopy( this.cursors, 0, newstack, 0, this.cursors.length );
            this.cursors = newstack;
        }
        this.cursors[ this.pos++ ] = cursor;
    }

    /**
     * Pop last cursor.
     *
     * @return The last cursor.
     */
    final protected int pop() {
        if ( --this.pos < 0 ) {
            throw new RuntimeException( "Unbalanced stack." );
        } else {
            return this.cursors[ this.pos ];
        }
    }

    /**
     * Peek last cursor.
     *
     * @return The last cursor.
     */
    final protected int peek() {
        return this.cursors[ this.pos - 1 ];
    }

    /**
     * Return the remainder to read from the
     * current position.
     *
     * @return The remainder to read, or <code>null</code>
     * 		if the end was reached.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #getRemainderString()
     */
    public abstract Reader getRemainder() throws IOException;

    /**
     * Return the remainder to read from the
     * current position.
     *
     * @return The remainder to read, or <code>null</code>
     * 		if the end was reached.
     *
     * @throws IOException When an I/O error occur.
     *
     * @see #getRemainder()
     */
    public abstract String getRemainderString() throws IOException;

}
