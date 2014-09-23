package ml.alternet.scan;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A number constraint is used by scanners to read numbers
 * under conditions, such as limiting the number of total
 * digits to read, or accepting negative numbers, forcing
 * a number to be an integer, etc.
 *
 * @author Philippe Poulard
 */
public interface NumberConstraint extends Constraint {

    /**
     * When there is no constraint, there is no stop condition
     * (that always return true).
     *
     * <p>A number under no constraint is parsed until a character
     * is unexpected in the input sequence.</p>
     */
    NumberConstraint NO_CONSTRAINT = new NumberConstraint() {
        public boolean stopCondition(StringBuffer buf, int sourceIndex,
                int dotIndex, int exponentIndex, Scanner scanner)
                throws IOException {
            return false;
        }
        public Class<? extends Number> getNumberType() {
            return null;
        }
    };

    /**
     * A constraint for parsing bytes.
     * The constraint is applied while parsing.
     *
     * @see Byte#MAX_VALUE
     * @see Byte#MIN_VALUE
     */
    NumberConstraint BYTE_CONSTRAINT = new NumberClassConstraint() {
        public boolean stopCondition(StringBuffer buf, int sourceIndex,
                int dotIndex, int exponentIndex, Scanner scanner)
                throws IOException {
            return checkInteger( scanner ) || checkDigits( buf, scanner, MIN_BYTE, MAX_BYTE );
        }
        public Class<? extends Number> getNumberType() {
            return Byte.class;
        }
    };

    /**
     * A constraint for parsing shorts.
     * The constraint is applied while parsing.
     *
     * @see Short#MAX_VALUE
     * @see Short#MIN_VALUE
     */
    NumberConstraint SHORT_CONSTRAINT = new NumberClassConstraint() {
        public boolean stopCondition(StringBuffer buf, int sourceIndex,
                int dotIndex, int exponentIndex, Scanner scanner)
                throws IOException {
            return checkInteger( scanner ) || checkDigits( buf, scanner, MIN_SHORT, MAX_SHORT );
        }
        public Class<? extends Number> getNumberType() {
            return Short.class;
        }
    };

    /**
     * A constraint for parsing integers.
     * The constraint is applied while parsing.
     *
     * @see Integer#MAX_VALUE
     * @see Integer#MIN_VALUE
     */
    NumberConstraint INT_CONSTRAINT = new NumberClassConstraint() {
        public boolean stopCondition(StringBuffer buf, int sourceIndex,
                int dotIndex, int exponentIndex, Scanner scanner)
                throws IOException {
            return checkInteger( scanner ) || checkDigits( buf, scanner, MIN_INTEGER, MAX_INTEGER );
        }
        public Class<? extends Number> getNumberType() {
            return Integer.class;
        }
    };

    /**
     * A constraint for parsing integers.
     * The constraint is applied while parsing.
     *
     * @see BigInteger
     */
    NumberConstraint INTEGER_CONSTRAINT = new NumberClassConstraint() {
        public boolean stopCondition(StringBuffer buf, int sourceIndex,
                int dotIndex, int exponentIndex, Scanner scanner)
                throws IOException {
            return checkInteger( scanner );
        }
        public Class<? extends Number> getNumberType() {
            return BigInteger.class;
        }
    };

    /**
     * A constraint for parsing longs.
     * The constraint is applied while parsing.
     *
     * @see Long#MAX_VALUE
     * @see Long#MIN_VALUE
     */
    NumberConstraint LONG_CONSTRAINT = new NumberClassConstraint() {
        public boolean stopCondition(StringBuffer buf, int sourceIndex,
                int dotIndex, int exponentIndex, Scanner scanner)
                throws IOException {
            return checkInteger( scanner ) || checkDigits( buf, scanner, MIN_LONG, MAX_LONG );
        }
        public Class<? extends Number> getNumberType() {
            return Long.class;
        }
    };

    /**
     * A constraint for parsing decimal.
     * A decimal doesn't accept an exponent.
     * The constraint is applied while parsing.
     *
     * @see BigDecimal
     */
    NumberConstraint DECIMAL_CONSTRAINT = new NumberClassConstraint() {
        public boolean stopCondition(StringBuffer buf, int sourceIndex,
                int dotIndex, int exponentIndex, Scanner scanner)
                throws IOException {
            return checkExponent( scanner );
        }
        public Class<? extends Number> getNumberType() {
            return BigDecimal.class;
        }
    };

    /**
     * A constraint for parsing a double.
     * The constraint is <b>NOT</b> applied while parsing.
     *
     * @see Double
     */
    NumberConstraint DOUBLE_CONSTRAINT = new NumberConstraint() {
        public Class<? extends Number> getNumberType() {
            return Double.class;
        }
        public boolean stopCondition(StringBuffer buf, int sourceIndex,
                int dotIndex, int exponentIndex, Scanner scanner)
                throws IOException {
            return false;
        }
    };

    /**
     * A constraint for parsing a float.
     * The constraint is <b>NOT</b> applied while parsing.
     *
     * @see Float
     */
    NumberConstraint FLOAT_CONSTRAINT = new NumberConstraint() {
        public Class<? extends Number> getNumberType() {
            return Float.class;
        }
        public boolean stopCondition(StringBuffer buf, int sourceIndex,
                int dotIndex, int exponentIndex, Scanner scanner)
                throws IOException {
            return false;
        }
    };

    String MIN_BYTE = "" + Byte.MIN_VALUE;
    String MAX_BYTE = "" + Byte.MAX_VALUE;
    String MIN_SHORT = "" + Short.MIN_VALUE;
    String MAX_SHORT = "" + Short.MAX_VALUE;
    String MIN_INTEGER = "" + Integer.MIN_VALUE;
    String MAX_INTEGER = "" + Integer.MAX_VALUE;
    String MIN_LONG = "" + Long.MIN_VALUE;
    String MAX_LONG = "" + Long.MAX_VALUE;

    /**
     * Base class for checking constraints on number types.
     *
     * @author Philippe Poulard
     */
    abstract class NumberClassConstraint implements NumberConstraint {
        public boolean checkInteger( Scanner scanner ) throws IOException {
            return scanner.hasNextChar( ".Ee", false );
//            return scanner.lookAhead() == '.';
        }
        public boolean checkExponent( Scanner scanner ) throws IOException {
            return scanner.hasNextChar( "Ee", false );
        }
        /**
         * Check the digits of a buffer.
         * 
         * @param buf The buffer
         * @param scanner The scanner
         * @param min For negative values only. Inclusive.
         * @param max For positive values only. Inclusive.
         * @return <tt>true</tt> if the number is in the expected range, <tt>false</tt> otherwise.
         * @throws IOException When an I/O error occurs.
         */
        public boolean checkDigits(StringBuffer buf, Scanner scanner, String min, String max) throws IOException {
            int bl = buf.length();
            boolean neg = bl > 0 && buf.charAt( 0 ) == '-';
            if ( neg && min == null || ! neg && max == null ) {
                return false; // nothing to check
            }
            String number = "" + scanner.lookAhead();
            // skip sign and leading zeroes
            for (int i = neg?1:0; i < bl; i++ ) {
                if ( buf.charAt( i ) != '0' ) {
                    number = buf.substring( i ) + scanner.lookAhead();
                    break;
                }
            }
            int l = number.length();
            if ( neg ) {
                // negative values, use min
                max = min.substring( 1 );
            }
            if ( l > max.length() ) {
                // too much digits
                return true;
            } else if ( l < max.length() ) {
                // not too much digits
                return false;
            } else {
                // same digits, tests them one by one until one is different
                for ( int i = 0 ; i < max.length() ; i++ ) {
                    char nc = number.charAt( i );
                    char mc = max.charAt( i );
                    if ( nc > mc ) {
                        return true;
                    } else if ( nc < mc ) {
                        return false;
                    } // else continue
                }
                // all digits are eligible
                return false; // because inclusive
            }
        }
    };

    /**
     * Evaluate the stop condition from the given parameters.
     *
     * <p>This condition is evaluated by the scanner to check
     * if it has to append characters to the current buffer.</p>
     *
     * @param buf The buffer that receive the input characters
     * 		so far. It can be used by this method to evaluate
     * 		the stop condition.
     * 		For example if negative numbers are not allowed, the
     * 		first character of the buffer can't be "-".
     * @param sourceIndex The number of characters read so far ;
     * 		might be useful in certain stop conditions.
     * @param dotIndex The index where the dot character was
     * 		encountered, or -1 if not found so far.
     * @param exponentIndex The index where the exponent character
     * 		was encountered, or -1 if not found so far.
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
     */
    boolean stopCondition(StringBuffer buf, int sourceIndex, int dotIndex, int exponentIndex, Scanner scanner) throws IOException;

    /**
     * Enforce a number to be of a specific type.
     *
     * @return The type of the number, or <code>null</code>
     * 		if the type have to be the more suitable.
     */
    Class<? extends Number> getNumberType();

    /**
     * A constraint on numbers : after parsing the number, it will
     * be of the type given.
     *
     * @author Philippe Poulard
     */
    class Type implements NumberConstraint {

        /** The number class. */
        private Class<? extends Number> clazz;

        /**
         *
         * @param numberClass Used to enforce the number to be an
         * 		instance of the class given. The class must implement
         * 		{@link Number} and belong either to <tt>java.lang</tt>
         * 		or to <tt>java.math</tt>.
         */
        public Type( Class<? extends Number> numberClass ) {
            this.clazz = numberClass;
        }

        /**
         * Return the type of the number.
         */
        public Class<? extends Number> getNumberType() {
            return this.clazz;
        }

        /**
         * No stop condition here : the string supplied must be
         * of the type expected.
         *
         * @return <code>false</code>
         */
        public boolean stopCondition(StringBuffer buf, int sourceIndex,
                int dotIndex, int exponentIndex, Scanner scanner)
                throws IOException {
            return false;
        }
    }

}
