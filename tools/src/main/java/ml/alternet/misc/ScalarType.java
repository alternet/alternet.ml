package ml.alternet.misc;

/**
 * The Java basic scalar types.
 *
 * @author Philippe Poulard
 */
public enum ScalarType {

    Boolean {
        @SuppressWarnings("unchecked")
        @Override
        public Boolean get(java.lang.String value) {
            return (Boolean) java.lang.Boolean.parseBoolean(value);
        }
    },
    Byte {
        @SuppressWarnings("unchecked")
        @Override
        public Byte get(java.lang.String value) {
            return (Byte) java.lang.Byte.parseByte(value);
        }
    },
    Character {
        @SuppressWarnings("unchecked")
        @Override
        public String get(java.lang.String value) {
            return value;
        }
    },
    Double {
        @SuppressWarnings("unchecked")
        @Override
        public Double get(java.lang.String value) {
            return (Double) java.lang.Double.parseDouble(value);
        }
    },
    Float {
        @SuppressWarnings("unchecked")
        @Override
        public Float get(java.lang.String value) {
            return (Float) java.lang.Float.parseFloat(value);
        }
    },
    Integer {
        @SuppressWarnings("unchecked")
        @Override
        public Integer get(java.lang.String value) {
            return (Integer) java.lang.Integer.parseInt(value);
        }
    },
    Long {
        @SuppressWarnings("unchecked")
        @Override
        public Long get(java.lang.String value) {
            return (Long) java.lang.Long.parseLong(value);
        }
    },
    Short {
        @SuppressWarnings("unchecked")
        @Override
        public Short get(java.lang.String value) {
            return (Short) java.lang.Short.parseShort(value);
        }
    },
    String {
        @SuppressWarnings("unchecked")
        @Override
        public String get(java.lang.String value) {
            return value;
        }
    };

    public static final int JAVA_LANG_PACKAGE_LENGTH = "java.lang.".length();

    public abstract <T> T get(String value);

}
