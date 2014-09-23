package ml.alternet.scan;

import java.io.IOException;

/**
 * A string constraint is used by scanners to read strings
 * under conditions, such as limiting the number of characters
 * to read, or stop reading when a whitespace is encountered.
 *
 * <p>An implementation is also responsible of the way the
 * characters have to be appended to the target string, for
 * example when some characters can be escaped in the source.</p>
 * 
 * <p>Notice that stop characters MUST NOT be consumed.</p>
 *
 * @author Philippe Poulard
 */
public interface StringConstraint extends Constraint {

    /**
     * Read only Java whitespaces.
     *
     * @see Character#isWhitespace(char)
     */
    StringConstraint WS_CONSTRAINT = new StringConstraint() {
        public int append(int sourceIndex, int targetLength, Scanner scanner,
                StringBuilder buf) throws IOException {
            char c = scanner.lookAhead();
            buf.append( c );
            return 1;
        }
        public boolean stopCondition(int sourceIndex, int targetLength,
                Scanner scanner) throws IOException {
            // stop as soon as the next char is not a whitespace
            return ! Character.isWhitespace( scanner.lookAhead() );
        }
    };

    /**
     * A string constraint that reads the characters until
     * a string is encountered. If the string is not found,
     * all the input is read and added to the buffer.
     *
     * @author Philippe Poulard
     */
    class Read_UntilString implements StringConstraint {
        String stopString;
        /**
         * Read until a given stop string; the stop
         * string is NOT consumed and NOT added to the buffer.
         *
         * @param stopString The stop string
         */
        public Read_UntilString(String stopString) {
            this.stopString = stopString;
        }
        public int append(int sourceIndex, int targetLength, Scanner scanner,
                StringBuilder buf) throws IOException {
            char c = scanner.lookAhead();
            buf.append( c );
            return 1;
        }
        public boolean stopCondition(int sourceIndex, int targetLength,
                Scanner scanner) throws IOException {
            // stop as soon as the new next string is the stop string
            return scanner.hasNextString(this.stopString, false);
        }
    }

    /**
     * A string constraint that reads the characters until
     * a char is encountered. If none of the chars are not found,
     * all the input is read and added to the buffer.
     *
     * @author Philippe Poulard
     */
    class ReadUntilChar implements StringConstraint {
        String stopChars;
        /**
         * Read until a given stop char; the stop
         * char is NOT consumed and NOT added to the buffer.
         *
         * @param stopChars A list of stop chars.
         */
        public ReadUntilChar(String stopChars) {
            this.stopChars = stopChars;
        }
        public int append(int sourceIndex, int targetLength, Scanner scanner,
                StringBuilder buf) throws IOException {
            char c = scanner.lookAhead();
            buf.append( c );
            return 1;
        }
        public boolean stopCondition(int sourceIndex, int targetLength,
                Scanner scanner) throws IOException {
            // stop as soon as the new next string is any of the stop chars
            return scanner.hasNextChar(this.stopChars, false);
        }
    }

    /**
     * A string constraint that reads the characters until
     * a char is encountered. If the char is not found,
     * all the input is read and added to the buffer.
     *
     * @author Philippe Poulard
     */
    class ReadUntilSingleChar implements StringConstraint {
        char stopChar;
        /**
         * Read until a given stop char; the stop
         * char is NOT consumed and NOT added to the buffer.
         *
         * @param stopChar A stop char.
         */
        public ReadUntilSingleChar(char stopChar) {
            this.stopChar = stopChar;
        }
        public int append(int sourceIndex, int targetLength, Scanner scanner,
                StringBuilder buf) throws IOException {
            char c = scanner.lookAhead();
            buf.append( c );
            return 1;
        }
        public boolean stopCondition(int sourceIndex, int targetLength,
                Scanner scanner) throws IOException {
            // stop as soon as the new next char is the stop char
            return scanner.hasNextChar(this.stopChar, false);
        }
    }

    /**
     * Evaluate the stop condition from the given parameters.
     *
     * <p>This condition is evaluated by the scanner to check
     * if it has to append characters to the current buffer.</p>
     *
     * @param sourceIndex The number of characters read so far ;
     * 		might be useful in certain stop conditions.
     * @param targetLength The number of characters put in the
     * 		target buffer so far ; might be useful in certain
     * 		stop conditions, for example to limit the length
     * 		of the string to return.
     * @param scanner The scanner that reads the input.
     * 		According to the parsing strategy, if the sequence
     * 		of characters involved in the stop condition (if any)
     * 		don't have to be consumed, the relevant methods of
     * 		the scanner should be involved.
     *
     * @return <code>true</code> to indicate that the current scan
     * 		must stop, <code>false</code> if more characters have
     * 		to be read.
     *
     * @throws IOException When the scanner cause an error.
     *
     * @see #append(int, int, Scanner, StringBuilder)
     */
    boolean stopCondition(int sourceIndex, int targetLength, Scanner scanner) throws IOException;

    /**
     * Append the current character to the buffer.
     *
     * <p>This is a kind of post-process for characters that have to be accepted
     * in the target string. An escape mechanism can be applied here
     * (note that this method could be called by {@link #stopCondition(int, int, Scanner)}
     * as well). Several characters can be produced as well.</p>
     *
     * @param sourceIndex The position of the index read so far.
     * 		Start at 0.
     * @param targetLength The number of characters put in the
     * 		target buffer so far.
     * @param scanner The scanner that reads the input. The characters
     * 		to read can be scanned as well (if necessary, marks can be
     * 		used safely).
     * @param buf The buffer where the accepted characters will be appended.
     *
     * @return The number of characters actually appended to the buffer.
     *
     * @throws IOException When the scanner cause an error.
     *
     * @see Scanner#nextString(StringConstraint, StringBuilder)
     */
    int append(int sourceIndex, int targetLength, Scanner scanner, StringBuilder buf) throws IOException;

}
