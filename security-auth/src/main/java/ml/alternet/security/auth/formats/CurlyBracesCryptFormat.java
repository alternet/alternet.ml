package ml.alternet.security.auth.formats;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.inject.Singleton;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.encode.BytesEncoding;
import ml.alternet.misc.Thrower;
import ml.alternet.scan.Scanner;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.Hasher.Builder;
import ml.alternet.security.auth.formatters.CurlyBracesCryptFormatter;
import ml.alternet.security.auth.formatters.CurlyBracesCryptFormatterWrapper;
import ml.alternet.security.auth.formatters.IterativeSaltedCurlyBracesCryptFormatter;
import ml.alternet.security.auth.formatters.SaltedCurlyBracesCryptFormatter;
import ml.alternet.security.auth.hasher.MessageHasher;
import ml.alternet.security.auth.hasher.PBKDF2Hasher;
import ml.alternet.security.auth.hasher.SaltedMessageHasher;
import ml.alternet.util.EnumUtil;

import static ml.alternet.misc.Thrower.safeCall;

/**
 * The scheme of this format appears in curly braces.
 *
 * <h1>Examples :</h1>
 * <ul>
 * <li>Contains the password encoded to base64 (just like {SSHA}) :
 * <pre>{SSHA.b64}986H5cS9JcDYQeJd6wKaITMho4M9CrXM</pre></li>
 * <li>Contains the password encoded to hexa :
 * <pre>{SSHA.HEX}3f5ca6203f8cdaa44d9160575c1ee1d77abcf59ca5f852d1</pre></li>
 * </ul>
 *
 * SSHA : Salted SHA
 *
 * @author Philippe Poulard
 */
@Singleton
public class CurlyBracesCryptFormat implements CryptFormat, DiscoverableCryptFormat {

    /**
     * Return the end boundary of the scheme.
     */
    protected char shemeEndChar() {
        return '}';
    }

    /**
     * Return the start boundary of the scheme that will test
     * and consume the characters before the scheme, if any.
     */
    protected Predicate<Scanner> schemeStartCondition() {
        return c -> safeCall(() -> c.hasNextChar('{', true));
    }

    @Override
    public Optional<Hasher> resolve(String crypt) {
        try {
            Scanner scanner = Scanner.of(crypt);
            SchemeWithEncoding swe = new SchemeWithEncoding(
                scanner,
                schemeStartCondition(),
                shemeEndChar()
            );
            return swe.scheme.map(scheme -> Thrower.safeCall(() -> {
                if (scanner.hasNextChar(shemeEndChar(), true)) {
                    Hasher.Builder builder = null;
                    if ("CRYPT".equals(scheme)) {
                        String mcfPart = scanner.getRemainderString().get();
                        builder = new ModularCryptFormat().resolve(mcfPart).get().getBuilder();
                        builder.setFormatter(new CurlyBracesCryptFormatterWrapper<>(builder.getFormatter(), "CRYPT", new ModularCryptFormat()));
                    } else {
                        try {
                            builder = Hashers.valueOf(scheme).get().getBuilder();
                        } catch (IllegalArgumentException e) { // scheme not within the enum
                            builder = lookup(scheme);
                        }
                    }
                    if (builder != null) {
                        Optional<BytesEncoding> encoding = swe.encoding.get();
                        if (encoding.isPresent()) {
                            builder.setEncoding(encoding.get());
                            builder.setVariant("withEncoding");
                        };
                        return builder.use(crypt).build();
                    }
                } // else that format is not matching
                return null;
            }));
        } catch (Exception ex) {
            return Thrower.doThrow(ex);
        }
    }

    /**
     * @return "CurlyBracesCryptFormat"
     */
    @Override
    public String family() {
        return "CurlyBracesCryptFormat";
    }

    /**
     * @return "{[scheme]}:[shemeSpecificPart]"
     */
    @Override
    public String infoTemplate() {
        return "{[scheme]}[shemeSpecificPart]";
    }

    /**
     * LDAP hash formats specified by <a href="https://tools.ietf.org/html/rfc2307.html">RFC 2307</a>
     * including standard formats {MD5}, {SMD5}, {SHA}, {SSHA} and extensions. These schemes range
     * from somewhat to very insecure, and should not be used except when required.
     *
     * <h1>Configuration</h1>
     *
     * You can examine the configuration of each value
     * <a href="https://github.com/alternet/alternet.ml/blob/master/security-auth/src/main/java/ml/alternet/security/auth/formats/CurlyBracesCryptFormat.java">here</a>.
     *
     * @author Philippe Poulard
     */
    public enum Hashers implements Supplier<Hasher> {

        /**
         * LDAP’s plain MD5 format.
         *
         * <p>"<tt>password</tt>" -&gt; "<tt>{MD5}X03MO1qnZdYdgyfeuILPmQ==</tt>"</p>
         */
        MD5(
            Hasher.Builder.builder()
                .setClass(MessageHasher.class)
                .setScheme("MD5")
                .setAlgorithm("MD5")
                .setEncoding(BytesEncoder.base64)
                .setFormatter(new CurlyBracesCryptFormatter())
        ),

        /**
         * LDAP’s plain SHA1 format.
         *
         * <p>"<tt>password</tt>" -&gt; "<tt>{SHA}W6ph5Mm5Pz8GgiULbPgzG37mj9g=</tt>"</p>
         */
        SHA(
            Hasher.Builder.builder()
                .setClass(MessageHasher.class)
                .setScheme("SHA")
                .setAlgorithm("SHA1")
                .setEncoding(BytesEncoder.base64)
                .setFormatter(new CurlyBracesCryptFormatter())
        ),

        /**
         * LDAP’s salted MD5 format with 4 bytes salt.
         *
         * <p>"<tt>password</tt>" -&gt; "<tt>{SMD5}jNoSMNY0cybfuBWiaGlFw3Mfi/U=</tt>"</p>
         */
        SMD5(
            Hasher.Builder.builder()
                .setClass(SaltedMessageHasher.class)
                .setScheme("SMD5")
                .setAlgorithm("MD5")
                .setEncoding(BytesEncoder.base64)
                .setSaltByteSize(4)
                .setFormatter(new SaltedCurlyBracesCryptFormatter())
        ),

        /**
         * LDAP’s salted SHA1 format with 4 bytes salt.
         *
         * <p>"<tt>password</tt>" -&gt; "<tt>{SSHA}pKqkNr1tq3wtQqk+UcPyA3HnA2NsU5NJ</tt>"</p>
         */
        SSHA(
            Hasher.Builder.builder()
                .setClass(SaltedMessageHasher.class)
                .setScheme("SSHA")
                .setAlgorithm("SHA1")
                .setEncoding(BytesEncoder.base64)
                .setSaltByteSize(4)
                .setFormatter(new SaltedCurlyBracesCryptFormatter())
        ),

        /**
         * PBKDF2 with SHA1 algorithm.
         *
         * <p>"<tt>password</tt>" -&gt; "<tt>{PBKDF2}131000$tLbWWssZ45zzfi9FiDEmxA$dQlpmhY4dGvmx4MOK/uOj/WU7Lg</tt>"</p>
         */
        PBKDF2(
            Hasher.Builder.builder()
                .setClass(PBKDF2Hasher.class)
                .setScheme("PBKDF2")
                .setAlgorithm("PBKDF2WithHmacSHA1")
                .setEncoding(BytesEncoder.abase64)
                .setSaltByteSize(24)
                .setHashByteSize(20)
                .setIterations(29000)
                .setFormatter(new IterativeSaltedCurlyBracesCryptFormatter())
        ),

        /**
         * PBKDF2 with SHA256 algorithm.
         */
        PBKDF2_SHA256(
            PBKDF2.get().getBuilder()
                .setHashByteSize(32)
                .setAlgorithm("PBKDF2WithHmacSHA256")
        ),

        /**
         * PBKDF2 with SHA512 algorithm.
         */
        PBKDF2_SHA512(
            PBKDF2.get().getBuilder()
                .setHashByteSize(64)
                .setAlgorithm("PBKDF2WithHmacSHA512")
        ),

        /**
         * Alternate method for storing plaintext passwords by using
         * the identifying prefix <tt>{plaintext}</tt>.
         */
        plaintext(plainTextBuilder());

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static Builder plainTextBuilder() {
            Builder b = PlainTextCryptFormat.get().getBuilder();
            b.setFormatter(new CurlyBracesCryptFormatterWrapper(b.getFormatter(), "plaintext", new PlainTextCryptFormat()));
            return b;
        }

        private Hasher.Builder builder;

        @Override
        public Hasher get() {
            return this.builder.build();
        }

        Hashers(Hasher.Builder builder) {
            this.builder = builder;
            EnumUtil.replace(this, s -> s.replace("_", "-"));
        }

    }

}
