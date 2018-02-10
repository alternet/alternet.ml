package ml.alternet.security.auth.hashers;

import java.util.function.Supplier;

import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.Hasher.Builder;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.PlainTextCryptFormat;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat.CryptFormatterWrapper;
import ml.alternet.security.auth.hashers.impl.MessageHasher;
import ml.alternet.security.auth.hashers.impl.PBKDF2Hasher;
import ml.alternet.security.auth.hashers.impl.SaltedMessageHasher;
import ml.alternet.security.binary.BytesEncoder;
import ml.alternet.util.EnumUtil;

/**
 * LDAP hash formats specified by RFC 2307 including standard formats {MD5}, {SMD5}, {SHA}, {SSHA}
 * and extensions. These schemes range from somewhat to very insecure, and should not be used except when required.
 *
 * <h1>Configuration</h1>
 *
 * You can examine the configuration of each value
 * <a href="https://github.com/alternet/alternet.ml/blob/master/security-auth/src/main/java/ml/alternet/security/auth/hashers/CurlyBracesCryptFormatHashers.java">here</a>.
 *
 * @author Philippe Poulard
 */
public enum CurlyBracesCryptFormatHashers implements Supplier<Hasher> {

    /**
     * LDAP’s plain MD5 format.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>{MD5}X03MO1qnZdYdgyfeuILPmQ==</tt>"</p>
     */
    MD5 {
        @Override
        public Hasher get() {
            return Hasher.Builder.builder()
                .setClass(MessageHasher.class)
                .setAlgorithm("MD5")
                .setEncoding(BytesEncoder.base64)
                .setFormatter(CurlyBracesCryptFormat.CryptFormatter.INSTANCE)
                .build();
        }
    },

    /**
     * LDAP’s plain SHA1 format.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>{SHA}W6ph5Mm5Pz8GgiULbPgzG37mj9g=</tt>"</p>
     */
    SHA {
        @Override
        public Hasher get() {
            return Hasher.Builder.builder()
                .setClass(MessageHasher.class)
                .setAlgorithm("SHA1")
                .setEncoding(BytesEncoder.base64)
                .setFormatter(CurlyBracesCryptFormat.CryptFormatter.INSTANCE)
                .build();
        }
    },

    /**
     * LDAP’s salted MD5 format with 4 bytes salt.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>{SMD5}jNoSMNY0cybfuBWiaGlFw3Mfi/U=</tt>"</p>
     */
    SMD5 {
        @Override
        public Hasher get() {
            return Hasher.Builder.builder()
                .setClass(SaltedMessageHasher.class)
                .setAlgorithm("MD5")
                .setEncoding(BytesEncoder.base64)
                .setSaltByteSize(4)
                .setFormatter(CurlyBracesCryptFormat.SaltedCryptFormatter.INSTANCE)
                .build();
        }
    },

    /**
     * LDAP’s salted SHA1 format with 4 bytes salt.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>{SSHA}pKqkNr1tq3wtQqk+UcPyA3HnA2NsU5NJ</tt>"</p>
     */
    SSHA {
        @Override
        public Hasher get() {
            return Hasher.Builder.builder()
                .setClass(SaltedMessageHasher.class)
                .setAlgorithm("SHA1")
                .setEncoding(BytesEncoder.base64)
                .setSaltByteSize(4)
                .setFormatter(CurlyBracesCryptFormat.SaltedCryptFormatter.INSTANCE)
                .build();
        }
    },

    /**
     * PBKDF2 with SHA1 algorithm.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>{PBKDF2}131000$tLbWWssZ45zzfi9FiDEmxA$dQlpmhY4dGvmx4MOK/uOj/WU7Lg</tt>"</p>
     */
    PBKDF2 {
        @Override
        public Hasher get() {
            return Hasher.Builder.builder()
                .setClass(PBKDF2Hasher.class)
                .setAlgorithm("PBKDF2WithHmacSHA1")
                .setEncoding(BytesEncoder.abase64)
                .setSaltByteSize(24)
                .setHashByteSize(20)
                .setIterations(29000)
                .setFormatter(CurlyBracesCryptFormat.IterativeSaltedFormatter.INSTANCE)
                .build();
        }
    },

    PBKDF2_SHA256 {
        @Override
        public Hasher get() {
            return PBKDF2.get().getBuilder()
                .setHashByteSize(32)
                .setAlgorithm("PBKDF2WithHmacSHA256")
                .build();
        }
    },

    PBKDF2_SHA512 {
        @Override
        public Hasher get() {
            return PBKDF2.get().getBuilder()
                .setHashByteSize(64)
                .setAlgorithm("PBKDF2WithHmacSHA512")
                .build();
        }
    },

    /**
     * Alternate method for storing plaintext passwords by using
     * the identifying prefix <tt>{plaintext}</tt>.
     */
    plaintext {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public Hasher get() {
            Builder b = PlainTextCryptFormat.get().getBuilder();
            b.setFormatter(new CryptFormatterWrapper(b.getFormatter()));
            return b.build();
        }
    };

    @Override
    public abstract Hasher get();

    private CurlyBracesCryptFormatHashers() {
        EnumUtil.replace(this, s -> s.replace("_", "-"));
    }

}
