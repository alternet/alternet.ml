package ml.alternet.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ml.alternet.misc.OmgException;

/**
 * Numbers utilities.
 *
 * @author Philippe Poulard
 */
@Util
public final class NumberUtil {

    private NumberUtil() {
    }

    /**
     * Min Float, as a double. Exact value is 1.4E-45 and not
     * 1.401298464324817E-45.
     */
    private static final double MIN_FLOAT = Double.valueOf("" + Float.MIN_VALUE).doubleValue();

    /**
     * Parse a number from the given string.
     *
     * @param number
     *            The number to parse ; all characters must be valid characters
     *            for the result number.
     *
     * @return A number, which class is the most suitable.
     *
     * @throws NumberFormatException
     *             When the string is not a number.
     */
    public static Number parseNumber(String number) {
        return parseNumber(number, true);
    }

    /**
     * Parse a number from the given string.
     *
     * @param number
     *            The number to parse ; all characters must be valid characters
     *            for the result number.
     * @param isFloatingPoint
     *            <code>false</code> if the string doesn't represent a decimal
     *            number (with a dot or an exponent) ; <code>true</code>
     *            otherwise. If we don't know, <code>true</code> is acceptable,
     *            and if it appears that the number was an integer, getting the
     *            result will be a little more longer, but not so much. If
     *            <code>false</code> and the number do have fraction digits,
     *            they will be ignored.
     *
     * @return A number with the most suitable Number type.
     *
     * @throws NumberFormatException
     *             When the string is not a number.
     */
    public static Number parseNumber(String number, boolean isFloatingPoint) {
        Number n;
        if (isFloatingPoint) {
            n = Double.valueOf(number);
            double d = n.doubleValue();
            if (d == Double.POSITIVE_INFINITY || d == Double.NEGATIVE_INFINITY) {
                BigDecimal bd = new BigDecimal(number);
                BigDecimal rounded = bd.setScale(0, BigDecimal.ROUND_FLOOR);
                if (bd.compareTo(rounded) == 0) {
                    // it is an integer
                    return bd.toBigInteger();
                } else { // it is not an integer
                    return bd;
                }
            } else if (d > Long.MAX_VALUE || d < Long.MIN_VALUE || d != n.longValue()) {
                // DON'T SIMPLIFY TO (d <= 0.0d) ? - d : d;
                // otherwise 0.0 will give -0.0 (although we don't mind here ;
                // sure ?)
                double absD = d <= 0.0d ? 0.0d - d : d;
                if (absD >= MIN_FLOAT) {
                    float f = n.floatValue();
                    if (f != Float.POSITIVE_INFINITY && f != Float.NEGATIVE_INFINITY) {
                        return new Float(f);
                    }
                }
                return n;
            }
        } // else : either a non floating point,
          // or an integer expressed like a floating point e.g. 1.0
        try {
            n = Long.valueOf(number);
            long l = n.longValue();
            if (l >= 0) {
                if (l <= Byte.MAX_VALUE) {
                    return new Byte(n.byteValue());
                } else {
                    if (l <= Short.MAX_VALUE) {
                        return new Short(n.shortValue());
                    } else {
                        if (l <= Integer.MAX_VALUE) {
                            return new Integer(n.intValue());
                        }
                    }
                }
                return n;
            } else {
                if (l >= Byte.MIN_VALUE) {
                    return new Byte(n.byteValue());
                } else {
                    if (l >= Short.MIN_VALUE) {
                        return new Short(n.shortValue());
                    } else {
                        if (l >= Integer.MIN_VALUE) {
                            return new Integer(n.intValue());
                        }
                    }
                }
                return n;
            }
        } catch (NumberFormatException nfe) { // too big !
            try {
                return new BigInteger(number);
            } catch (NumberFormatException unexpectedDotOrExp) {
                // damned, they decide to annoy me
                for (int i = number.length() ; --i >= 0 ; ) {
                    char c = number.charAt(i);
                    if (c == 'e' || c == 'E') {
                        return new BigDecimal(number).toBigInteger();
                    }
                }
                int dot = number.indexOf('.');
                if (dot == -1) { // avoid recursive call
                    throw unexpectedDotOrExp; // nothing more to do here
                } else {
                    return parseNumber(number.substring(0, dot), false);
                }
            }
        }
    }

    /**
     * Parse a number from the given string.
     *
     * @param number
     *            The number to parse ; all characters must be valid characters
     *            for the result number.
     * @param isFloatingPoint
     *            <code>false</code> if the string doesn't represent a decimal
     *            number (with a dot or an exponent) ; <code>true</code>
     *            otherwise. If we don't know, <code>true</code> is acceptable,
     *            and if it appears that the number was an integer, getting the
     *            result will be a little more longer, but not so much. If
     *            <code>false</code> and the number do have fraction digits,
     *            they will be ignored. IN ANY CASE, if the numberClass is not
     *            <code>null</code> this value is useless.
     * @param numberClass
     *            Used to enforce the number to be an instance of the class
     *            given. The class must implement {@link Number} and belong
     *            either to <tt>java.lang</tt> or <tt>java.math</tt> ; or can be
     *            AtomicInteger or AtomicLong ; other custom types should be
     *            built by hand from one of the default types. If
     *            <code>null</code>, the most suitable class will be returned.
     *
     * @return A number.
     *
     * @throws NumberFormatException
     *             When the string is not a number.
     */
    public static Number parseNumber(String number, boolean isFloatingPoint, Class<? extends Number> numberClass) {
        if (numberClass == null) {
            return parseNumber(number, isFloatingPoint);
        } else {
            if (Integer.class.equals(numberClass) || int.class.equals(numberClass)) {
                return Integer.valueOf(number);
            } else if (Byte.class.equals(numberClass) || byte.class.equals(numberClass)) {
                return Byte.valueOf(number);
            } else if (Double.class.equals(numberClass) || double.class.equals(numberClass)) {
                return Double.valueOf(number);
            } else if (Float.class.equals(numberClass) || float.class.equals(numberClass)) {
                return Float.valueOf(number);
            } else if (Long.class.equals(numberClass) || long.class.equals(numberClass)) {
                return Long.valueOf(number);
            } else if (BigInteger.class.equals(numberClass)) {
                return new BigInteger(number);
            } else if (BigDecimal.class.equals(numberClass)) {
                return new BigDecimal(number);
            } else if (Short.class.equals(numberClass) || short.class.equals(numberClass)) {
                return Short.valueOf(number);
            } else if (AtomicInteger.class.equals(numberClass)) {
                return new AtomicInteger(Integer.valueOf(number));
            } else if (AtomicLong.class.equals(numberClass)) {
                return new AtomicLong(Long.valueOf(number));
            } else {
                return parseNumber(number, isFloatingPoint);
            }
        }
    }

    /**
     * Cast a number to a given number type.
     *
     * @param number The actual number.
     * @param numberClass The target class.
     * @param <T> The number type.
     *
     * @return A number of the expected type.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Number> T as(Number number, Class<T> numberClass) {
        if (numberClass.isInstance(number)) {
            return (T) number;
        } else {
            if (Integer.class.equals(numberClass) || int.class.equals(numberClass)) {
                return (T) (Integer) number.intValue();
            } else if (Byte.class.equals(numberClass) || byte.class.equals(numberClass)) {
                return (T) (Byte) number.byteValue();
            } else if (Double.class.equals(numberClass) || double.class.equals(numberClass)) {
                return (T) (Double) number.doubleValue();
            } else if (Float.class.equals(numberClass) || float.class.equals(numberClass)) {
                return (T) (Float) number.floatValue();
            } else if (Long.class.equals(numberClass) || long.class.equals(numberClass)) {
                return (T) (Long) number.longValue();
            } else if (BigInteger.class.equals(numberClass)) {
                if (number instanceof BigDecimal) {
                    return (T) ((BigDecimal) number).toBigInteger();
                } else {
                    return (T) BigInteger.valueOf(number.longValue());
                }
            } else if (BigDecimal.class.equals(numberClass)) {
                if (number instanceof BigInteger) {
                    return (T) new BigDecimal((BigInteger) number);
                } else {
                    return (T) BigDecimal.valueOf(number.doubleValue());
                }
            } else if (Short.class.equals(numberClass) || short.class.equals(numberClass)) {
                return (T) (Short) number.shortValue();
            } else if (AtomicInteger.class.equals(numberClass)) {
                return (T) new AtomicInteger(number.intValue());
            } else if (AtomicLong.class.equals(numberClass)) {
                return (T) new AtomicLong(number.longValue());
            } else {
                throw new OmgException();
            }
        }
    }

}
