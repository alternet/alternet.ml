package ml.alternet.security.web.server;

import ml.alternet.security.PasswordManager;

/**
 * Extract the passwords of an HTML form,
 * and replace their characters with '*'.
 *
 * @author Philippe Poulard
 */
public abstract class FormReader {

    int maxLength; // maxFormContentSize e.g. 10000
    int maxKeys; // maxFormKeys e.g. 100

    // the current key
    String key = null;
    // the current value before escaping its value
    char[] value = null;
    /** Indicates that the raw data in the input buffer has to be replaced with '*' */
    protected boolean replace = false;

    PasswordManager pm;

    /**
     * Create an HTML form reader, encoded in UTF-8.
     *
     * @param formLimit The size limits of the form to read.
     * @param pm Used to create safe passwords.
     */
    public FormReader(FormLimit formLimit, PasswordManager pm) {
        this.maxLength = formLimit.getMaxFormContentSize();
        this.maxKeys = formLimit.getMaxFormKeys();
        this.pm = pm;
    }

    /**
     * Reset this form reader.
     */
    public void reset() {
        key = null;
        value = null;
        replace = false;
    }

    /**
     * This method is called when the ByteBuffer has to
     * be read for further parsing ; it is used to fill
     * the "buf" byte array.
     *
     * @param size The max amount of data that can be read.
     * @param buf The target buffer to fill.
     * @param offset The offset index to read.
     * @param length The length to read.
     *
     * @return The amount of data effectively read.
     */
    public int get(int size, byte[] buf, int offset, int length) {
        // checkBounds : fail fast
        if ((offset | length | (offset + length) | (buf.length - (offset + length))) < 0) {
            throw new IndexOutOfBoundsException();
        }

        // here, we intercept the process by decoding the bytes
        // to char, and when a field name is recognized as a password
        // (according to the configuration), then the password
        // is captured ; during the process, we ensure that
        // no String object is ever created, and that the data
        // in the data source have been replaced with '*'
        // The captured password is stored in a temp char array
        // and encrypted ; then the temp char array is cleared.

        Utf8StringBuilder buffer = new Utf8StringBuilder();
        int mapSize = 0;
        int totalLength = 0;
        int end = offset + size;
        int b;
        for (int i = offset; i < end; ) {
            b = readItem(buf, i++); // i++ is not in the for loop otherwise we would miss a place
            try {
                switch ((char) b) {
                case '&':
                    value = buffer.toChars();
                    if (key != null) {
                        capture(key, value);
                        mapSize++;
                        endValue();
                        buffer.clear();
                    } else if (value != null && value.length > 0) {
                        capture(new String(value), new char[0]);
                        mapSize++;
                    }
                    buffer.reset();
                    key = null;
                    value = null;
                    if (maxKeys > 0 && mapSize > maxKeys) {
                        throw new IllegalStateException("Form too many keys");
                    }
                    break;
                case '=':
                    if (key != null) {
                        buffer.append((byte) b);
                        break;
                    }
                    key = buffer.toReplacedString();
                    startValue();
                    buffer.reset();
                    break;
                case '+':
                    buffer.append((byte) ' ');
                    break;
                case '%':
                    int code0 = readItem(buf, i++);
                    boolean decoded = false;
                    if ('u' == code0) {
                        code0 = readItem(buf, i++);
                        // XXX: we have to read the next byte, otherwise code0 is always 'u'
                        if (code0 >= 0) {
                            int code1 = readItem(buf, i++);
                            if (code1 >= 0) {
                                int code2 = readItem(buf, i++);
                                if (code2 >= 0) {
                                    int code3 = readItem(buf, i++);
                                    if (code3 >= 0) {
                                        buffer.append(Character.toChars
                                                ((convertHexDigit(code0) << 12)
                                                + (convertHexDigit(code1) << 8)
                                                + (convertHexDigit(code2) << 4)
                                                + convertHexDigit(code3)));
                                        decoded = true;
                                    }
                                }
                            }
                        }
                    } else if (code0 >= 0) {
                        int code1 = readItem(buf, i++);
                        if (code1 >= 0) {
                            buffer.append((byte) ((convertHexDigit(code0) << 4)
                                    + convertHexDigit(code1)));
                            decoded = true;
                        }
                    }
                    if (!decoded) {
                        buffer.append(Utf8Appendable.REPLACEMENT);
                    }
                    break;
                default:
                    buffer.append((byte) b);
                    break;
                }
            } catch (Utf8StringBuilder.NotUtf8Exception e) {
                log(e);
            } catch (NumberFormatException e) {
                buffer.append(Utf8Appendable.REPLACEMENT_UTF8, 0, 3);
                log(e);
            }
            if (maxLength >= 0 && (++totalLength > maxLength)) {
                throw new IllegalStateException("Form too large");
            }
        }
        if (key != null) {
            value = buffer.toChars();
            capture(key, value);
            mapSize++;
            endValue();
            buffer.clear();
            buffer.reset();
        } else if (buffer.length() > 0) {
            capture(buffer.toReplacedString(), new char[0]);
            mapSize++;
        }
        return size;
    }

    /**
     * Read the next byte ; if the <code>replace</code>
     * flag is set, the input source AND the buffer have
     * to be set to '*', but the byte read has to kept
     * unchanged.
     *
     * @param buf The target buffer to fill.
     * @param i The index of the target buffer.
     *
     * @return The byte read ; do not set it to '*',
     *      it's a byte from the password.
     */
    public abstract int readItem(byte[] buf, int i);

    /**
     * Hold the passwords that are extracted ; the
     * capture context also indicates which fields
     * in the form has to be captured, and hold a
     * reference to the incoming data source.
     *
     * @return The current capture context.
     */
    public abstract CaptureContext<?> getCurrentCaptureContext();

    /**
     * Log an exception.
     *
     * @param exception The exception to log.
     */
    public abstract void log(Exception exception);

    void startValue() {
        CaptureContext<?> cc = getCurrentCaptureContext();
        if (key != null && cc != null && cc.fields.contains(key)) {
            // the current key is part of the known fields,
            // we do have a password to capture
            replace = true;
        }
    }

    void endValue() {
        replace = false; // end reached
    }

    void capture(String name, char[] value) {
        CaptureContext<?> cc = getCurrentCaptureContext();
        if (cc != null && cc.fields.contains(name)) {
            cc.add(name, pm.newPassword(value));
        }
    }

    /**
     * Convert an HEX digit to a byte.
     *
     * @param c An ASCII encoded character 0-9 a-f A-F
     * @return The byte value of the character 0-16.
     */
    public static int convertHexDigit( int c ) {
        int d = ((c & 0x1f) + ((c >> 6) * 0x19) - 0x10);
        if (d < 0 || d > 15) {
            throw new NumberFormatException("!hex " + c);
        }
        return d;
    }

}
