package ml.alternet.security.auth.hashers;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import ml.alternet.misc.TodoException;
import ml.alternet.security.algorithms.BCrypt;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.SaltlessModularCryptFormatter;
import ml.alternet.security.auth.hashers.impl.Argon2Hasher;
import ml.alternet.security.auth.hashers.impl.BCryptHasher;
import ml.alternet.security.auth.hashers.impl.MD5BasedHasher;
import ml.alternet.security.auth.hashers.impl.MessageHasher;
import ml.alternet.security.auth.hashers.impl.SHA2Hasher;
import ml.alternet.security.binary.BytesEncoder;
import ml.alternet.security.binary.BytesEncoder.ValueSpace;
import ml.alternet.util.EnumUtil;

/**
 * Out-of-the box hashers.
 *
 * <h1>Configuration</h1>
 *
 * You can examine the configuration of each value
 * <a href="https://github.com/alternet/alternet.ml/blob/master/security-auth/src/main/java/ml/alternet/security/auth/hashers/ModularCryptFormatHashers.java">here</a>.
 *
 * @author Philippe Poulard
 */
public enum ModularCryptFormatHashers implements Supplier<Hasher.Builder> {

//    __("_"),

    /**
     * The Apache variant (apr1) of the MD5 based BSD password algorithm 1.
     */
    $apr1$ {
        @Override
        public Hasher.Builder get() {
            return Hasher.builder()
                .setClass(MD5BasedHasher.class)
                .setVariant("apr1")
                .setEncoding(BytesEncoder.h64)
                .setFormatter(MD5BasedHasher.MD5CRYPT_FORMATTER);
        }
    },

    /**
     * The MD5 based BSD password algorithm 1.
     */
    $1$ {
        @Override
        public Hasher.Builder get() {
            return Hasher.builder()
                .setClass(MD5BasedHasher.class)
                .setVariant("1")
                .setEncoding(BytesEncoder.h64)
                .setFormatter(MD5BasedHasher.MD5CRYPT_FORMATTER);
        }
    },

    /**
     * The BCrypt/Blowfish algorithm.
     */
    $2$ {
        @Override
        public Hasher.Builder get() {
            return Hasher.builder()
                .setClass(BCryptHasher.class)
                .setVariant("2")
                .setEncoding(BytesEncoder.bcrypt64)
                .setFormatter(BCryptHasher.BCRYPT_FORMATTER)
                .setSaltByteSize(BCrypt.BCRYPT_SALT_LEN)
                .setLogRounds(Hasher.HasherBuilder.DEFAULT_GENSALT_LOG2_ROUNDS);
        }
    },

    /**
     * The 'a' variant of the BCrypt/Blowfish algorithm.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>$2a$08$YkG5/ze2FPw8C6vuAs7WHuvS0IeyyQfLgE7Ti8tT5F2sMEkVJlNo.</tt>"</p>
     */
    $2a$ {
        @Override
        public Hasher.Builder get() {
            return Hasher.builder()
                .setClass(BCryptHasher.class)
                .setVariant("2a")
                .setEncoding(BytesEncoder.bcrypt64)
                .setFormatter(BCryptHasher.BCRYPT_FORMATTER)
                .setSaltByteSize(BCrypt.BCRYPT_SALT_LEN)
                .setLogRounds(Hasher.HasherBuilder.DEFAULT_GENSALT_LOG2_ROUNDS);
        }
    },

    /**
     * The 'b' variant of the BCrypt/Blowfish algorithm.
     */
    $2b$ {
        @Override
        public Hasher.Builder get() {
            return $2a$.get();
        }
    },

    /**
     * The 'y' variant of the BCrypt/Blowfish algorithm.
     */
    $2y$ {
        @Override
        public Hasher.Builder get() {
            return $2a$.get();
        }
    },

    /**
     * The NT-HASH algorithm, used by Microsoft Windows NT and successors.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>$3$$8846f7eaee8fb117ad06bdd830b7586c</tt>"</p>
     */
    $3$ {
        @Override
        public Hasher.Builder get() {
            return Hasher.builder()
                .setClass(MessageHasher.class)
                .setAlgorithm("MD4")
                .setVariant("3")
                .setEncoding(BytesEncoder.hexaLower)
                .setCharset(StandardCharsets.UTF_16LE)
                .setFormatter(SaltlessModularCryptFormatter.INSTANCE);
        }
    },

    /**
     * The SHA-256 hash algorithms in a libc6 crypt() compatible way.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>$5$Gbk2Pwra$NpET.3X2eOP/fE7wUQIKbghUdN73SfiJIWMJ2fq1lX1</tt>"</p>
     */
    $5$ {
        @Override
        public Hasher.Builder get() {
            return Hasher.builder()
                .setClass(SHA2Hasher.class)
                .setVariant("5")
                .setAlgorithm("SHA-256")
                .setEncoding(BytesEncoder.h64be)
                .setFormatter(SHA2Hasher.SHA2CRYPT_FORMATTER);
        }
    },
    /**
     * The SHA-512 hash algorithms in a libc6 crypt() compatible way.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>$6$rounds=656000$dWon8CGHFnBG3arr$HRumKDkl.jZRScX0ToRnA6i2tA448SRvtqHkpamcN/3ioy/g5tLG83.Is/ZpKjN8c2MOwRgi9jYBcpxaQ7wfh.</tt>"</p>
     */
    $6$ {
        @Override
        public Hasher.Builder get() {
            return Hasher.builder()
                .setClass(SHA2Hasher.class)
                .setVariant("6")
                .setAlgorithm("SHA-512")
                .setEncoding(BytesEncoder.h64be)
                .setFormatter(SHA2Hasher.SHA2CRYPT_FORMATTER);
        }
    },

    $md5$ {
        @Override
        public Hasher.Builder get() {
            return Hasher.builder()
                .setClass(MessageHasher.class)
                .setAlgorithm("MD5");
        }
    },
    $md5_("md,"),

    $sha1$,


    $bcrypt_sha256$("bcrypt-sha256"),

    $pbkdf2$,
    $pbkdf2_sha256$("pbkdf2-sha256"),
    $pbkdf2_sha512$("pbkdf2-sha512"),

    $argon2i$ {
        @Override
        public Hasher.Builder get() {
            return Hasher.builder()
                .setClass(Argon2Hasher.class)
                .setVariant("argon2i")
                .setAlgorithm("Blake2b") // because it is its name
                .setHashByteSize(32)
                .setSaltByteSize(16)
                .setEncoding(BytesEncoder.base64(ValueSpace.base64.get(), false))
                .setFormatter(Argon2Hasher.ARGON2_FORMATTER);
        }
    },
    $argon2d$ {
        @Override
        public Hasher.Builder get() {
            return $argon2i$.get()
                    .setVariant("argon2d");
        }
    },
    $argon2id$ {
        @Override
        public Hasher.Builder get() {
            return $argon2i$.get()
                    .setVariant("argon2id");
        }
    };

    ModularCryptFormatHashers() {
        EnumUtil.replace(ModularCryptFormatHashers.class, this, s -> s.replace("\\$", ""));
    }

    ModularCryptFormatHashers(String name) {
        EnumUtil.replace(ModularCryptFormatHashers.class, this, s -> name);
    }

    @Override
    public Hasher.Builder get() {
        throw new TodoException();
    }

}
