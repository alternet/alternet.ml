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
    interface Builder extends Configuration {

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
         * The standard property names
         *
         * @author Philippe Poulard
         */
        enum Field {

            /**
             * The standard property name "scheme"
             */
            scheme,

            /**
             * The standard property name "algorithm"
             */
            algorithm,

            /**
             * The standard property name "variant"
             */
            variant,

            /**
             * The standard property name "charset" ; default is "UTF-8"
             */
            charset,

            /**
             * The standard property name "saltByteSize" ; the value must be an Integer
             */
            saltByteSize,

            /**
             * The standard property name "hashByteSize" ; the value must be an Integer
             */
            hashByteSize,

            /**
             * The standard property name "iterations" ; the value must be an Integer
             */
            iterations,

            /**
             * The standard property name "encoding" ; the value must be "base64", "hexa",
             * "auto" (base64 for encoding, and automatic detection for decoding) or
             * "none" (to get the UTF-8 bytes).
             */
            encoding,

            /**
             * The standard property name "logRounds" (for BCrypt only) ;
             * the log2 of the number of rounds of hashing to apply - the work
             * factor therefore increases as 2**log_rounds.
             */
            logRounds

        }

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
         * Setter for the hash byte size.
         *
         * @param hashByteSize Value.
         * @return This builder.
         */
        Builder setHashByteSize(int hashByteSize);

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
         * Setter for encoding.
         *
         * @param encoding non-<code>null</code> encoding.
         * @return This builder.
         */
        Builder setEncoding(BytesEncoding encoding);

        /**
         * Setter for encoding.
         *
         * @param encoding encoding class name.
         * @return This builder.
         */
        Builder setEncoding(String encoding);

        /**
         * Setter for encoding, by encoder name.
         *
         * The name is looked up in {@link BytesEncoder}, and if not found
         * with the lookup mechanism for the {@link BytesEncoding} class
         * and the given encoder name as the variant.
         *
         * @param encoder The name of the encoder.
         * @return This builder.
         *
         * @see DiscoveryService
         */
        Builder setEncoder(String encoder);

        /**
         * Set the hasher class
         *
         * @param clazz The hasher class
         * @return This builder.
         */
        Builder setClass(Class<? extends Hasher> clazz);

        /**
         * Set the hasher class
         *
         * @param clazz The hasher class name
         * @return This builder.
         */
        Builder setClass(String clazz);

        /**
         * Set the crypt formatter
         *
         * @param cryptFormatter The crypt formatter
         * @return This builder.
         */
        Builder setFormatter(CryptFormatter<? extends CryptParts> cryptFormatter);

        /**
         * Replace the current configuration by a copy the given configuration
         *
         * @param configuration The configuration to set
         * @return This builder.
         */
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
    interface Configuration {

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
         * @param <T> The type of the crypt parts
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
    class HasherBuilder implements Builder, Configuration {

        Conf conf = new Conf();

        static class Conf implements Configuration, Cloneable {

            private Class<? extends Builder> builder;
            private Class<? extends Hasher> clazz;
            private String scheme;
            private String algorithm;
            private String variant;
            private Charset charset = StandardCharsets.UTF_8;
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
                    props.put(Field.saltByteSize.name(), this.saltByteSize);
                }
                if (this.hashByteSize != -1) {
                    props.put(Field.hashByteSize.name(), this.hashByteSize);
                }
                if (this.iterations != -1) {
                    props.put(Field.iterations.name(), this.iterations);
                }
                if (this.logRounds != -1) {
                    props.put(Field.logRounds.name(), this.logRounds);
                }
                if (this.scheme != null) {
                    props.setProperty(Field.scheme.name(), this.scheme);
                }
                if (this.algorithm != null) {
                    props.setProperty(Field.algorithm.name(), this.algorithm);
                }
                if (this.variant != null) {
                    props.setProperty(Field.variant.name(), this.variant);
                }
                props.put(Field.encoding.name(), this.encoding);
                props.setProperty(Field.charset.name(), this.charset.name());
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
        public Builder setClass(String clazz) {
            this.conf.clazz = Thrower.safeCall(() ->
                (Class<? extends Hasher>) Class.forName(clazz)
            );
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
        public int getHashByteSize() {
            return this.conf.hashByteSize;
        }

        @Override
        public Builder setHashByteSize(int hashByteSize) {
            this.conf.hashByteSize = hashByteSize;
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
        public BytesEncoding getEncoding() {
            return this.conf.encoding;
        }

        @Override
        public Builder setEncoding(BytesEncoding encoding) {
            this.conf.encoding = encoding;
            return this;
        }

        @Override
        public Builder setEncoding(String encoding) {
            this.conf.encoding = Thrower.safeCall(() ->
                (BytesEncoding) Class.forName(encoding).newInstance()
            );
            return this;
        }

        @Override
        public Builder setEncoder(String encoder) {
            try {
                this.conf.encoding = BytesEncoder.valueOf(encoder);
            } catch (IllegalArgumentException e) {
                try {
                    this.conf.encoding = DiscoveryService.lookupSingleton(
                        BytesEncoding.class.getName() + "/" + encoder
                    );
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
                Integer sbs = (Integer) properties.get(Field.saltByteSize.name());
                if (sbs != null) {
                    p++;
                    setSaltByteSize(sbs.intValue());
                }
            } catch (ClassCastException e) {
                throw new InvalidAlgorithmParameterException(Field.saltByteSize.name()
                        + " must be an integer", e);
            }
            try {
                Integer hbs = (Integer) properties.get(Field.hashByteSize.name());
                if (hbs != null) {
                    p++;
                    setHashByteSize(hbs.intValue());
                }
            } catch (ClassCastException e) {
                throw new InvalidAlgorithmParameterException(Field.hashByteSize.name()
                        + " must be an integer", e);
            }
            try {
                Integer i = (Integer) properties.get(Field.iterations.name());
                if (i != null) {
                    p++;
                    if (i <= 0 ) {
                        throw new InvalidAlgorithmParameterException(Field.iterations.name()
                                + " must be greater than 0.");
                    }
                    setIterations(i.intValue());
                }
            } catch (ClassCastException e) {
                throw new InvalidAlgorithmParameterException(Field.iterations.name()
                        + " must be an integer", e);
            }
            try {
                Integer lr = (Integer) properties.get(Field.logRounds.name());
                if (lr != null) {
                    p++;
                    setLogRounds(lr.intValue());
                }
            } catch (ClassCastException e) {
                throw new InvalidAlgorithmParameterException(Field.logRounds.name()
                        + " must be an integer", e);
            }
            String encoding = (String) properties.get(Field.encoding.name());
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
         * @see Field#algorithm
         * @see Field#charset
         * @see Field#saltByteSize
         * @see Field#hashByteSize
         * @see Field#iterations
         * @see Field#encoding
         * @see Field#logRounds
         */
        protected List<String> getPropertyNames() {
            return Arrays.asList(
                Field.algorithm.name(),
                Field.charset.name(),
                Field.saltByteSize.name(),
                Field.hashByteSize.name(),
                Field.iterations.name(),
                Field.encoding.name(),
                Field.logRounds.name()
            );
        }

        @Override
        public Hasher build() {
            try {
                Hasher hr = this.conf.clazz.getConstructor(Builder.class)
                        .newInstance(this);
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
