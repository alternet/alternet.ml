package ml.alternet.misc;

/**
 * The Java basic scalar types.
 *
 * @author Philippe Poulard
 */
public enum ScalarType {

    /** The Boolean type. */
    Boolean {
        @SuppressWarnings("unchecked")
        @Override
        public Boolean get(java.lang.String value) {
            return java.lang.Boolean.parseBoolean(value);
        }
    },
    /** The Byte type. */
    Byte {
        @SuppressWarnings("unchecked")
        @Override
        public Byte get(java.lang.String value) {
            return java.lang.Byte.parseByte(value);
        }
    },
    /** The Character type. */
    Character {
        @SuppressWarnings("unchecked")
        @Override
        public String get(java.lang.String value) {
            return value;
        }
    },
    /** The Double type. */
    Double {
        @SuppressWarnings("unchecked")
        @Override
        public Double get(java.lang.String value) {
            return java.lang.Double.parseDouble(value);
        }
    },
    /** The Float type. */
    Float {
        @SuppressWarnings("unchecked")
        @Override
        public Float get(java.lang.String value) {
            return java.lang.Float.parseFloat(value);
        }
    },
    /** The Integer type. */
    Integer {
        @SuppressWarnings("unchecked")
        @Override
        public Integer get(java.lang.String value) {
            return java.lang.Integer.parseInt(value);
        }
    },
    /** The Long type. */
    Long {
        @SuppressWarnings("unchecked")
        @Override
        public Long get(java.lang.String value) {
            return java.lang.Long.parseLong(value);
        }
    },
    /** The Short type. */
    Short {
        @SuppressWarnings("unchecked")
        @Override
        public Short get(java.lang.String value) {
            return java.lang.Short.parseShort(value);
        }
    },
    /** The String type. */
    String {
        @SuppressWarnings("unchecked")
        @Override
        public String get(java.lang.String value) {
            return value;
        }
    };

    /**
     * "java.lang.".length()
     */
    public static final int JAVA_LANG_PACKAGE_LENGTH = "java.lang.".length();

    /**
     * "Cast" as the underlying type.
     *
     * @param value The value to transform.
     *
     * @return The value in the expected type.
     *
     * @param <T> The type of the expected type.
     *
     * @throws NumberFormatException For number types, if the string does not contain a parsable byte.
     */
    public abstract <T> T get(String value);

}
