package ml.alternet.security.auth;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.discover.LookupKey;
import ml.alternet.encode.BytesEncoder;
import ml.alternet.encode.BytesEncoding;
import ml.alternet.misc.Thrower;
import ml.alternet.misc.WtfException;
import ml.alternet.security.Password;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.auth.formats.CryptFormatter;

/**
 * Computes/checks a password to/with a crypt.
 *
 * <p>Some hasher require a user name, but the more often they won't.</p>
 *
 * <p>A hasher is usually available to the discovery service with
 * a variant make from the crypt format family name and the
 * hasher scheme.</p>
 *
 * <p>{@link CredentialsChecker} supply several means to find a hasher or
 * to check a password.</p>
 *
 * <p>A hasher is immutable, to reconfigure it, use its builder with {@link #getBuilder()}.</p>
 *
 * @see CryptFormat
 * @see Credentials
 *
 * @author Philippe Poulard
 */
public interface Hasher extends Credentials.Checker {

    /**
     * Return the current settings of this hasher.
     *
     * @return The current properties, including the default values.
     *
     * @see Builder
     */
    Configuration getConfiguration();

    /**
     * Return the builder used for creating this hasher.
     * Changes made to this builder doesn't affect the
     * configuration of this hasher. Since a hasher is
     * immutable, use this method to create a new hasher
     * based on the settings of this hasher.
     *
     * @return The builder used to create this hasher.
     */
    Builder getBuilder();

    /**
     * Compute a crypt from credentials.
     *
     * <p>By default, the user is ignored.</p>
     *
     * @param credentials Credentials, that must contain at least the password.
     *
     * @return A crypt.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    String encrypt(Credentials credentials) throws InvalidAlgorithmParameterException;

    /**
     * Compute a crypt from a password.
     *
     * <p>By default, the user is ignored.</p>
     *
     * @param user The user name (sometimes required by some hasher), or <tt>null</tt>
     * @param password The password.
     *
     * @return A crypt.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    default String encrypt(String user, char[] password) throws InvalidAlgorithmParameterException {
        return encrypt(Credentials.fromUserPassword(user, password));
    }

    /**
     * Compute a crypt from a password.
     *
     * @param password The password.
     *
     * @return A crypt.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    default String encrypt(char[] password) throws InvalidAlgorithmParameterException {
        return encrypt(Credentials.fromPassword(password));
    }

    /**
     * Compute a crypt from a password.
     *
     * <p>By default, the user is ignored.</p>
     *
     * @param user The user name (sometimes required by some hasher), or <tt>null</tt>
     *
     * @param password The password.
     *
     * @return A crypt.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    default String encrypt(String user, Password password) throws InvalidAlgorithmParameterException {
        return encrypt(Credentials.fromUserPassword(user, password));
    }

    /**
     * Compute a crypt from a password.
     *
     * @param password The password.
     *
     * @return A crypt.
     *
     * @throws InvalidAlgorithmParameterException When the hash algorithm fails.
     */
    default String encrypt(Password password) throws InvalidAlgorithmParameterException {
        return encrypt(Credentials.fromPassword(password));
    }

    /**
     * Find the hasher for a given crypt.
     *
     * @param crypt The crypt to resolve.
     * @param formats The list of candidate formats.
     * @return If found, the hasher.
     */
    static Optional<Hasher> resolve(String crypt, CryptFormat... formats) {
        return resolve(crypt, Arrays.asList(formats));
    }

    /**
     * Find the hasher for a given crypt.
     *
     * @param crypt The crypt to resolve.
     * @param formats The list of candidate formats.
     * @return If found, the hasher.
     */
    static Optional<Hasher> resolve(String crypt, List<CryptFormat> formats) {
        return formats.stream()
                .map(f -> f.resolve(crypt))
                .filter(o -> o.isPresent())
                .findFirst()
                .map(o -> o.get());
    }

    /**
     * Compare bytes array in length constant time in order to prevent "timing attack".
     *
     * @param a A non-null byte array.
     * @param b A non-null byte array.
     *
     * @return <code>true</code> if all the bytes are equals, <code>false</code> otherwise.
     */
    static boolean compare(byte[] a, byte[] b) {
        // do not use Arrays.equals(hash, pwdHash);
        int res = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            // do not use == , may be compiled / interpreted as a branch
            res |= a[i] ^ b[i];
        }
        return res == 0;
    }

    /**
     * Compare chars array in length constant time in order to prevent "timing attack".
     *
     * @param a A non-null byte array.
     * @param b A non-null byte array.
     *
     * @return <code>true</code> if all the bytes are equals, <code>false</code> otherwise.
     */
    static boolean compare(char[] a, char[] b) {
        // do not use Arrays.equals(hash, pwdHash);
        int res = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            // do not use == , may be compiled / interpreted as a branch
            res |= a[i] ^ b[i];
        }
        return res == 0;
    }

    /**
     * Hasher builder.
     *
     * According to its configuration, it will build
     * a concrete hasher.
     *
     * @author Philippe Poulard
     */
    public static interface Builder extends Configuration {

        /**
         * Get the default builder according to the configuration.
         *
         * @return A default builder.
         *
         * @see DiscoveryService#lookup(String)
         */
        static Builder builder() {
            try {
                Class<Builder> clazz = DiscoveryService.lookup(Builder.class.getCanonicalName());
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                return Thrower.doThrow(e);
            }
        }

        /**
         * The standard property name "scheme"
         */
        public static String SCHEME_PROPERTY_NAME = "scheme";

        /**
         * The standard property name "algorithm"
         */
        public static String ALGORITHM_PROPERTY_NAME = "algorithm";

        /**
         * The standard property name "variant"
         */
        public static String VARIANT_PROPERTY_NAME = "variant";

        /**
         * The standard property name "charset" ; default is "UTF-8"
         */
        public static String CHARSET_PROPERTY_NAME = "charset";

        /**
         * The standard property name "saltByteSize" ; the value must be an Integer
         */
        public static final String SALT_BYTE_SIZE_PROPERTY_NAME = "saltByteSize";

        /**
         * The standard property name "hashByteSize" ; the value must be an Integer
         */
        public static final String HASH_BYTE_SIZE_PROPERTY_NAME = "hashByteSize";

        /**
         * The standard property name "iterations" ; the value must be an Integer
         */
        public static final String ITERATIONS_PROPERTY_NAME = "iterations";

        /**
         * The standard property name "encoding" ; the value must be "base64", "hexa",
         * "auto" (base64 for encoding, and automatic detection for decoding) or
         * "none" (to get the UTF-8 bytes).
         */
        public static final String ENCODING_PROPERTY_NAME = "encoding";

        /**
         * The standard property name "logRounds" (for BCrypt only) ;
         * the log2 of the number of rounds of hashing to apply - the work
         * factor therefore increases as 2**log_rounds.
         */
        public static final String LOG_ROUNDS_PROPERTY_NAME = "logRounds";

        /**
         * Build a concrete configured hasher.
         *
         * @return A hasher.
         */
        Hasher build();

        /**
         * Configure this builder with some properties.
         *
         * @param properties The set of properties.
         * @return This builder.
         *
         * @throws InvalidAlgorithmParameterException When some parameters are invalid.
         */
        Builder configure(Properties properties) throws InvalidAlgorithmParameterException;

        /**
         * Return a snapshot of the configuration based on the
         * current builder.
         * Changes to this builder doesn't affect previous
         * configuration.
         *
         * @return A snapshot configuration.
         */
        Configuration getConfiguration();

        /**
         * Setter for the scheme ; the scheme is the name
         * under which is registered the underlying hasher.
         *
         * @param scheme The name of the scheme.
         * @return This builder.
         */
        Builder setScheme(String scheme);

        /**
         * Setter for the algorithm.
         *
         * @param algorithm The name of the digester algorithm.
         * @return This builder.
         */
        Builder setAlgorithm(String algorithm);

        /**
         * Setter for the variant.
         *
         * @param variant The name of the variant,
         *                  depend on the algorithm.
         * @return This builder.
         */
        Builder setVariant(String variant);

        /**
         * Setter for the charset.
         *
         * @param charset Value.
         * @return This builder.
         */
        Builder setCharset(Charset charset);

        /**
         * Setter for the charset.
         *
         * @param charset Value.
         * @return This builder.
         */
        Builder setCharset(String charset);

        /**
         * Setter for the salt byte size.
         *
         * @param saltByteSize Value.
         * @return This builder.
         */
        Builder setSaltByteSize(int saltByteSize);

        /**
         * Set the default salt byte size.
         *
         * @return This builder.
         *
         * @see HasherBuilder#DEFAULT_SALT_BYTE_SIZE
         */
        Builder setSaltByteSize();

        /**
         * Setter for the hash byte size.
         *
         * @param hashByteSize Value.
         * @return This builder.
         */
        Builder setHashByteSize(int hashByteSize);

        /**
         * Set the default hash byte size.
         *
         * @return This builder.
         *
         * @see HasherBuilder#DEFAULT_HASH_BYTE_SIZE
         */
        Builder setHashByteSize();

        /**
         * Setter for iterations count.
         *
         * @param iterations Number of iterations.
         * @return This builder.
         */
        Builder setIterations(int iterations);

        /**
         * Set the log rounds for BCrypt.
         *
         * @param logRounds For BCrypt algorithm only : the log2 of
         * the number of rounds of hashing to apply - the work factor
         * therefore increases as 2**log_rounds.
         * @return This builder.
         */
        Builder setLogRounds(int logRounds);

        /**
         * Set the default log rounds for BCrypt.
         * @return This builder.
         *
         * @see HasherBuilder#DEFAULT_GENSALT_LOG2_ROUNDS
         */
        Builder setLogRounds();

        /**
         * Set the default iterations count.
         *
         * @return This builder.
         *
         * @see HasherBuilder#DEFAULT_ITERATIONS
         */
        Builder setIterations();

        /**
         * Setter for encoding.
         *
         * @param encoding non-<code>null</code> encoding.
         * @return This builder.
         */
        Builder setEncoding(BytesEncoding encoding);

        /**
         * Setter for encoding.
         *
         * @param encoding encoding.
         * @return This builder.
         * @throws ClassNotFoundException When the encoding class doesn't exist.
         * @throws IllegalAccessException When the encoding class can't be accessed.
         * @throws InstantiationException When the encoding class can't be instanciated.
         */
        Builder setEncoding(String encoding) throws InstantiationException, IllegalAccessException, ClassNotFoundException;

        Builder setEncoder(String encoder);

        Builder setClass(Class<? extends Hasher> clazz);

        Builder setClass(String clazz) throws ClassNotFoundException;

        Builder setFormatter(CryptFormatter<? extends CryptParts> cryptFormatter);

        Builder setConfiguration(Configuration configuration);

        /**
         * Set the crypt that was used for resolving the hasher when appropriate.
         *
         * @see CryptFormat#resolve(String)
         *
         * @param crypt An existing crypt may hold parameters useful on the configuration.
         *
         * @return This builder.
         */
        Builder use(String crypt);

    }

    /**
     * Read-only configuration of a hasher.
     *
     * @author Philippe Poulard
     */
    public interface Configuration {

        /**
         * The builder that built this configuration,
         * can't be null when the configuration is used
         * in a hasher.
         *
         * @return The builder.
         */
        Class<? extends Builder> getBuilder();

        /**
         * Export all the configuration.
         *
         * @return The configuration as properties.
         */
        Properties asProperties();

        /**
         * Return the scheme of the underlying hasher.
         *
         * @return The scheme, e.g. "SHA"
         */
        String getScheme();

        /**
         * Return the algorithm of the underlying hasher.
         *
         * @return The algorithm, e.g. "SHA1"
         */
        String getAlgorithm();

        /**
         * Return the variant of the underlying hasher.
         *
         * @return The variant, for example "apr1" for the Apache
         *      variant (apr1) of the MD5 based BSD password algorithm 1
         */
        String getVariant();

        /**
         * The charset used to encode passwords.
         *
         * @return The charset
         */
        Charset getCharset();

        /**
         * The encoding for representing bytes.
         *
         * @return The encoding
         */
        BytesEncoding getEncoding();

        /**
         * The formatter used to breakdown a cypt in parts, or to format parts in a crypt.
         *
         * @return The formatter.
         */
        <T> CryptFormatter<? extends CryptParts> getFormatter();

        /**
         * Get the log rounds
         *
         * @return The log rounds
         */
        int getLogRounds();

        /**
         * Get the hash byte size
         *
         * @return The hash byte size
         */
        int getHashByteSize();

        /**
         * Get the salt byte size
         *
         * @return The salt byte size
         */
        int getSaltByteSize();

        /**
         * Get the number of iterations
         *
         * @return The number of iterations
         */
        int getIterations();

        /**
         * Get the crypt template
         *
         * @return The crypt template
         */
        String getCrypt();

        /**
         * A hasher can extend this configuration extension
         * when a crypt can be used for configuration, typically
         * by using it as a template in order to extract a parameter
         * such as the workfactor.
         *
         * @author Philippe Poulard
         */
        interface Extension {

            /**
             * If a new hasher is built, its builder MUST unset its crypt
             * (otherwise it would configure itself with that crypt and
             * loop here).
             *
             * @see Builder#use(String)
             *
             * @param crypt A crypt.
             * @return A hasher.
             */
            Hasher configureWithCrypt(String crypt);

        }

    }

    /**
     * Default hasher builder implementation.
     *
     * For using a custom builder, simply specify a
     * lookup key that won't be used by default.
     *
     * @author Philippe Poulard
     */
    @LookupKey(forClass = Builder.class, byDefault = true)
    public static class HasherBuilder implements Builder, Configuration {

        public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
        public static final int DEFAULT_SALT_BYTE_SIZE = 24;
        public static final int DEFAULT_HASH_BYTE_SIZE = 24;
        public static final int DEFAULT_ITERATIONS = 1000;
        public static final int DEFAULT_GENSALT_LOG2_ROUNDS = 10;

        Conf conf = new Conf();

        static class Conf implements Configuration, Cloneable {

            private Class<? extends Builder> builder;
            private Class<? extends Hasher> clazz;
            private String scheme;
            private String algorithm;
            private String variant;
            private Charset charset = DEFAULT_CHARSET;
            private BytesEncoding encoding;
            private int iterations = -1;
            private int saltByteSize = -1;
            private int hashByteSize = -1;
            private int logRounds = -1;
            private CryptFormatter<? extends CryptParts> formatter;
            private String crypt;

            @Override
            public Conf clone() throws CloneNotSupportedException {
                return (Conf) super.clone();
            }

            @Override
            public int getSaltByteSize() {
                return this.saltByteSize;
            }
            @Override
            public int getLogRounds() {
                return this.logRounds;
            }

            @Override
            public int getIterations() {
                return this.iterations;
            }

            @Override
            public int getHashByteSize() {
                return this.hashByteSize;
            }

            @Override
            public CryptFormatter<? extends CryptParts> getFormatter() {
                return this.formatter;
            }

            @Override
            public BytesEncoding getEncoding() {
                return this.encoding;
            }

            @Override
            public Charset getCharset() {
                return this.charset;
            }

            @Override
            public String getScheme() {
                return this.scheme;
            }

            @Override
            public String getAlgorithm() {
                return this.algorithm;
            }

            @Override
            public String getVariant() {
                return this.variant;
            }

            @Override
            public String getCrypt() {
                return this.crypt;
            }

            @Override
            public Properties asProperties() {
                Properties props = new Properties();
                if (this.saltByteSize != -1) {
                    props.put(SALT_BYTE_SIZE_PROPERTY_NAME, this.saltByteSize);
                }
                if (this.hashByteSize != -1) {
                    props.put(HASH_BYTE_SIZE_PROPERTY_NAME, this.hashByteSize);
                }
                if (this.iterations != -1) {
                    props.put(ITERATIONS_PROPERTY_NAME, this.iterations);
                }
                if (this.logRounds != -1) {
                    props.put(LOG_ROUNDS_PROPERTY_NAME, this.logRounds);
                }
                if (this.scheme != null) {
                    props.setProperty(SCHEME_PROPERTY_NAME, this.scheme);
                }
                if (this.algorithm != null) {
                    props.setProperty(ALGORITHM_PROPERTY_NAME, this.algorithm);
                }
                if (this.variant != null) {
                    props.setProperty(VARIANT_PROPERTY_NAME, this.variant);
                }
                props.put(ENCODING_PROPERTY_NAME, this.encoding);
                props.setProperty(CHARSET_PROPERTY_NAME, this.charset.name());
                props.setProperty(Builder.class.getCanonicalName(), builder.getName());
                props.setProperty(Hasher.class.getCanonicalName(), clazz.getName());
                props.setProperty(CryptFormatter.class.getCanonicalName(), formatter.getClass().getName());
                return props;
            }

            @Override
            public Class<? extends Builder> getBuilder() {
                return this.builder;
            }

            @Override
            public String toString() {
                return asProperties().toString();
            }

        };

        @Override
        public Configuration getConfiguration() {
            try {
                Conf config = this.conf.clone();
                config.builder = this.getClass();
                return config;
            } catch (CloneNotSupportedException e) {
                throw WtfException.throwException(e);
            }
        }

        @Override
        public Properties asProperties() {
            return this.conf.asProperties();
        }

        @Override
        public Builder setClass(Class<? extends Hasher> clazz) {
            this.conf.clazz = clazz;
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Builder setClass(String clazz) throws ClassNotFoundException {
            this.conf.clazz = (Class<? extends Hasher>) Class.forName(clazz);
            return this;
        }

        @Override
        public String getScheme() {
            return this.conf.scheme;
        }

        @Override
        public Builder setScheme(String scheme) {
            this.conf.scheme = scheme;
            return this;
        }

        @Override
        public String getAlgorithm() {
            return this.conf.algorithm;
        }

        @Override
        public Builder setAlgorithm(String algorithm) {
            this.conf.algorithm = algorithm;
            return this;
        }

        @Override
        public String getVariant() {
            return this.conf.variant;
        }

        @Override
        public Builder setVariant(String variant) {
            this.conf.variant = variant;
            return this;
        }

        @Override
        public Charset getCharset() {
            return this.conf.charset;
        }

        @Override
        public Builder setCharset(Charset charset) {
            this.conf.charset = charset;
            return this;
        }

        @Override
        public Builder setCharset(String charset) {
            this.conf.charset = Charset.forName(charset);
            return this;
        }

        @Override
        public int getSaltByteSize() {
            return this.conf.saltByteSize;
        }

        @Override
        public Builder setSaltByteSize(int saltByteSize) {
            this.conf.saltByteSize = saltByteSize;
            return this;
        }

        @Override
        public Builder setSaltByteSize() {
            this.conf.saltByteSize = DEFAULT_SALT_BYTE_SIZE;
            return this;
        }

        @Override
        public int getHashByteSize() {
            return this.conf.hashByteSize;
        }

        @Override
        public Builder setHashByteSize(int hashByteSize) {
            this.conf.hashByteSize = hashByteSize;
            return this;
        }

        @Override
        public Builder setHashByteSize() {
            this.conf.hashByteSize = DEFAULT_HASH_BYTE_SIZE;
            return this;
        }

        @Override
        public int getIterations() {
            return this.conf.iterations;
        }

        @Override
        public Builder setIterations(int iterations) {
            this.conf.iterations = iterations;
            return this;
        }

        @Override
        public Builder setIterations() {
            this.conf.iterations = DEFAULT_ITERATIONS;
            return this;
        }

        @Override
        public int getLogRounds() {
            return this.conf.logRounds;
        }

        /**
         * Set the log rounds for BCrypt.
         *
         * @param logRounds For BCrypt algorithm only : the log2 of
         * the number of rounds of hashing to apply - the work factor
         * therefore increases as 2**log_rounds.
         */
        @Override
        public Builder setLogRounds(int logRounds) {
            this.conf.logRounds = logRounds;
            return this;
        }

        @Override
        public Builder setLogRounds() {
            this.conf.logRounds = DEFAULT_GENSALT_LOG2_ROUNDS;
            return this;
        }

        @Override
        public BytesEncoding getEncoding() {
            return this.conf.encoding;
        }

        @Override
        public Builder setEncoding(BytesEncoding encoding) {
            this.conf.encoding = encoding;
            return this;
        }

        @Override
        public Builder setEncoding(String encoding) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            this.conf.encoding = (BytesEncoding) Class.forName(encoding).newInstance();
            return this;
        }

        @Override
        public Builder setEncoder(String encoder) {
            try {
                this.conf.encoding = BytesEncoder.valueOf(encoder);
            } catch (IllegalArgumentException e) {
                try {
                    this.conf.encoding = DiscoveryService.lookupSingleton(BytesEncoding.class.getName() + "/" + encoder);
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e1) {
                    throw e;
                }
            }
            return this;
        }

        @Override
        public Builder setFormatter(CryptFormatter<? extends CryptParts> formatter) {
            this.conf.formatter = formatter;
            return this;
        }

        @Override
        public CryptFormatter<? extends CryptParts> getFormatter() {
            return this.conf.formatter;
        }

        @Override
        public String getCrypt() {
            return this.conf.crypt;
        }

        @Override
        public Builder use(String crypt) {
            this.conf.crypt = crypt;
            return this;
        }

        @Override
        public Builder configure(Properties properties) throws InvalidAlgorithmParameterException {
            int p = 0;
            try {
                Integer sbs = (Integer) properties.get(SALT_BYTE_SIZE_PROPERTY_NAME);
                if (sbs != null) {
                    p++;
                    setSaltByteSize(sbs.intValue());
                }
            } catch (ClassCastException e) {
                throw new InvalidAlgorithmParameterException(SALT_BYTE_SIZE_PROPERTY_NAME
                        + " must be an integer", e);
            }
            try {
                Integer hbs = (Integer) properties.get(HASH_BYTE_SIZE_PROPERTY_NAME);
                if (hbs != null) {
                    p++;
                    setHashByteSize(hbs.intValue());
                }
            } catch (ClassCastException e) {
                throw new InvalidAlgorithmParameterException(HASH_BYTE_SIZE_PROPERTY_NAME
                        + " must be an integer", e);
            }
            try {
                Integer i = (Integer) properties.get(ITERATIONS_PROPERTY_NAME);
                if (i != null) {
                    p++;
                    if (i <= 0 ) {
                        throw new InvalidAlgorithmParameterException(ITERATIONS_PROPERTY_NAME
                                + " must be greater than 0.");
                    }
                    setIterations(i.intValue());
                }
            } catch (ClassCastException e) {
                throw new InvalidAlgorithmParameterException(ITERATIONS_PROPERTY_NAME
                        + " must be an integer", e);
            }
            try {
                Integer lr = (Integer) properties.get(LOG_ROUNDS_PROPERTY_NAME);
                if (lr != null) {
                    p++;
                    setLogRounds(lr.intValue());
                }
            } catch (ClassCastException e) {
                throw new InvalidAlgorithmParameterException(LOG_ROUNDS_PROPERTY_NAME
                        + " must be an integer", e);
            }
            String encoding = (String) properties.get(ENCODING_PROPERTY_NAME);
            if (encoding != null) {
                p++;
                this.conf.encoding = BytesEncoder.valueOf(encoding);
            }
            if (properties.size() > p) {
                String propName = properties.stringPropertyNames().stream()
                        .filter(s -> ! getPropertyNames().contains(s))
                        .collect(Collectors.joining(", "));
                throw new InvalidAlgorithmParameterException("Some properties are not supported by this hasher : "
                        + propName);
            }
            return this;
        }

        /**
         * Return the property names supported by this hasher.
         *
         * @return The default list contains all the standard
         *      properties defined in Hasher.
         *
         * @see #ALGORITHM_PROPERTY_NAME
         * @see #CHARSET_PROPERTY_NAME
         * @see #SALT_BYTE_SIZE_PROPERTY_NAME
         * @see #HASH_BYTE_SIZE_PROPERTY_NAME
         * @see #ITERATIONS_PROPERTY_NAME
         * @see #ENCODING_PROPERTY_NAME
         * @see #LOG_ROUNDS_PROPERTY_NAME
         */
        protected List<String> getPropertyNames() {
            return Arrays.asList(
                    ALGORITHM_PROPERTY_NAME,
                    CHARSET_PROPERTY_NAME,
                    SALT_BYTE_SIZE_PROPERTY_NAME,
                    HASH_BYTE_SIZE_PROPERTY_NAME,
                    ITERATIONS_PROPERTY_NAME,
                    ENCODING_PROPERTY_NAME,
                    LOG_ROUNDS_PROPERTY_NAME);
        }

        @Override
        public Hasher build() {
            try {
                Hasher hr = this.conf.clazz.getConstructor(Configuration.class)
                        .newInstance(getConfiguration());
                if (this.conf.getCrypt() != null && hr instanceof Configuration.Extension) {
                    hr = ((Configuration.Extension) hr).configureWithCrypt(this.conf.getCrypt());
                }
                return hr;
            } catch (Exception e) {
                return Thrower.doThrow(e);
            }
        }

        @Override
        public Class<? extends Builder> getBuilder() {
            return getClass();
        }

        /**
         * This method must be override by any subclass.
         */
        @Override
        public Builder setConfiguration(Configuration configuration) {
            if (configuration instanceof Conf) {
                try {
                    this.conf = ((Conf) configuration).clone();
                } catch (CloneNotSupportedException e) {
                    // wtf ? Conf is cloneable
                    WtfException.throwException(e);
                }
            } else {
                throw new IllegalArgumentException(configuration.getClass() + " can't be handled by this builder.");
            }
            return this;
        }

    }

}
