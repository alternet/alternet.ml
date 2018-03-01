package ml.alternet.security.auth.formats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Singleton;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.scan.EnumValues;
import ml.alternet.scan.Scanner;
import ml.alternet.scan.StringConstraint;
import ml.alternet.security.algorithms.BCrypt;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.CryptFormatBase;
import ml.alternet.security.auth.formatters.Argon2CryptFormatter;
import ml.alternet.security.auth.formatters.BCryptFormatter;
import ml.alternet.security.auth.formatters.MD5ModularCryptFormatter;
import ml.alternet.security.auth.formatters.PBKDF2ModularCryptFormatter;
import ml.alternet.security.auth.formatters.SHA2ModularCryptFormatter;
import ml.alternet.security.auth.formatters.SaltlessModularCryptFormatter;
import ml.alternet.security.auth.hasher.Argon2Hasher;
import ml.alternet.security.auth.hasher.BCryptHasher;
import ml.alternet.security.auth.hasher.MD5BasedHasher;
import ml.alternet.security.auth.hasher.MessageHasher;
import ml.alternet.security.auth.hasher.PBKDF2Hasher;
import ml.alternet.security.auth.hasher.SHA2Hasher;
import ml.alternet.util.EnumUtil;

/**
 * The <a href="https://pythonhosted.org/passlib/modular_crypt_format.html">Modular Crypt Format</a>
 * is an ad-hoc standard used in popular linux-based systems.
 *
 * Format : <code>$[scheme]$[salt]$[crypt]</code>
 *
 * <table summary="">
 * <tr><th>ID</th><th>Method</th></tr>
 * <tr><td>1</td><td>MD5</td></tr>
 * <tr><td>2a</td><td>Blowfish</td></tr>
 * <tr><td>5</td><td>SHA-256</td></tr>
 * <tr><td>6</td><td>SHA-512</td></tr>
 * </table>
 *
 * <h1>Examples :</h1>
 * <ul>
 * <li>Apache MD5 crypt format :
 * <pre>$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1</pre></li>
 * <li>Crypt MD5 :
 * <pre>$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.</pre></li>
 * </ul>
 *
 * @author Philippe Poulard
 *
 * @see Hashers
 */
@Singleton
public class ModularCryptFormat extends CryptFormatBase implements CryptFormat {

    static EnumValues<Hashers> HASHERS = EnumValues.from(Hashers.class);

    @Override
    protected EnumValues<? extends Supplier<Hasher>> getEnumValues() {
        return HASHERS;
    }

    @Override
    public Optional<String> parseScheme(Scanner scanner) throws IOException {
        if (scanner.hasNextChar('$', true)) {
            return Optional.of(
                scanner.nextString(new StringConstraint.ReadUntilSingleChar('$')).get()
            );
        } else {
            return Optional.empty();
        }
    }

    /**
     * This family name, used as the lookup key if a scheme is not found
     * within the enum.
     *
     * @return "ModularCryptFormat"
     *
     * @see Hashers
     */
    @Override
    public String family() {
        return "ModularCryptFormat";
    }

    /**
     * Informations about the template.
     *
     * @return "$[scheme]$[salt]$[crypt]"
     */
    @Override
    public String infoTemplate() {
        return "$[scheme]$[salt]$[crypt]";
    }

    /**
     * Out-of-the box modular crypt format hashers.
     *
     * <h1>Configuration</h1>
     *
     * You can examine the configuration of each value
     * <a href="https://github.com/alternet/alternet.ml/blob/master/security-auth/src/main/java/ml/alternet/security/auth/formats/ModularCryptFormat.java">here</a>.
     *
     * @author Philippe Poulard
     */
    public enum Hashers implements Supplier<Hasher> {

        /**
         * The Apache variant (apr1) of the MD5 based BSD password algorithm 1.
         */
        $apr1$(
            Hasher.Builder.builder()
                .setClass(MD5BasedHasher.class)
                .setScheme("MD5")
                .setVariant("apr1")
                .setEncoding(BytesEncoder.h64be)
                .setFormatter(new MD5ModularCryptFormatter())
        ),

        /**
         * The MD5 based BSD password algorithm 1.
         */
        $1$(
            $apr1$.get().getBuilder()
                .setVariant("1")
        ),

        /**
         * The BCrypt/Blowfish algorithm.
         */
        $2$(
            Hasher.Builder.builder()
                .setClass(BCryptHasher.class)
                .setScheme("Blowfish")
                .setVariant("2")
                .setEncoding(BytesEncoder.bcrypt64)
                .setFormatter(new BCryptFormatter())
                .setSaltByteSize(BCrypt.BCRYPT_SALT_LEN)
                .setLogRounds(10)
        ),

        /**
         * The 'a' variant of the BCrypt/Blowfish algorithm.
         *
         * <p>"<tt>password</tt>" -&gt; "<tt>$2a$08$YkG5/ze2FPw8C6vuAs7WHuvS0IeyyQfLgE7Ti8tT5F2sMEkVJlNo.</tt>"</p>
         */
        $2a$(
            $2$.get().getBuilder()
                .setVariant("2a")
        ),

        /**
         * The 'b' variant of the BCrypt/Blowfish algorithm.
         */
        $2b$(
            $2a$.builder
        ),


        /**
         * The 'y' variant of the BCrypt/Blowfish algorithm.
         */
        $2y$(
            $2a$.builder
        ),

        /**
         * The NT-HASH algorithm, used by Microsoft Windows NT and successors.
         *
         * <p>"<tt>password</tt>" -&gt; "<tt>$3$$8846f7eaee8fb117ad06bdd830b7586c</tt>"</p>
         */
        $3$(
            Hasher.Builder.builder()
                .setClass(MessageHasher.class)
                .setScheme("MD4")
                .setAlgorithm("MD4")
                .setVariant("3")
                .setEncoding(BytesEncoder.hexa)
                .setCharset(StandardCharsets.UTF_16LE)
                .setFormatter(new SaltlessModularCryptFormatter())
        ),

        /**
         * The SHA-256 hash algorithms in a libc6 crypt() compatible way.
         *
         * <p>"<tt>password</tt>" -&gt; "<tt>$5$Gbk2Pwra$NpET.3X2eOP/fE7wUQIKbghUdN73SfiJIWMJ2fq1lX1</tt>"</p>
         */
        $5$(
            Hasher.Builder.builder()
                .setClass(SHA2Hasher.class)
                .setScheme("SHA-256")
                .setVariant("5")
                .setAlgorithm("SHA-256")
                .setEncoding(BytesEncoder.h64be)
                .setFormatter(new SHA2ModularCryptFormatter())
        ),

        /**
         * The SHA-512 hash algorithms in a libc6 crypt() compatible way.
         *
         * <p>"<tt>password</tt>" -&gt; "<tt>$6$rounds=656000$dWon8CGHFnBG3arr$HRumKDkl.jZRScX0ToRnA6i2tA448SRvtqHkpamcN/3ioy/g5tLG83.Is/ZpKjN8c2MOwRgi9jYBcpxaQ7wfh.</tt>"</p>
         */
        $6$(
            $5$.get().getBuilder()
                .setScheme("SHA-512")
                .setVariant("6")
                .setAlgorithm("SHA-512")
        ),

        /**
         * MD5.
         */
        $md5$(
            Hasher.Builder.builder()
                .setClass(MessageHasher.class)
                .setScheme("MD5")
                .setAlgorithm("MD5")
        ),

        /**
         * Modified BCrypt for SHA-256.
         */
        $bcrypt_sha256$(
            $2a$.get().getBuilder()
                .setClass(BCryptHasher.Digest.class)
                .setAlgorithm("SHA-256")
        ),

        /**
         * Modified BCrypt for SHA-512.
         */
        $bcrypt_sha512$(
            $2a$.get().getBuilder()
                .setClass(BCryptHasher.Digest.class)
                .setAlgorithm("SHA-512")
        ),

        /**
         * PBKDF2 with HMac SHA-1.
         */
        $pbkdf2_sha1$(
            Hasher.Builder.builder()
                .setClass(PBKDF2Hasher.class)
                .setScheme("PBKDF2")
                .setAlgorithm("PBKDF2WithHmacSHA1")
                .setEncoding(BytesEncoder.abase64)
                .setSaltByteSize(16)
                .setHashByteSize(20)
                .setIterations(29000)
                .setFormatter(new PBKDF2ModularCryptFormatter())
        ),

        /**
         * PBKDF2 with HMac SHA-256.
         */
        $pbkdf2_sha256$(
            $pbkdf2_sha1$.get().getBuilder()
                .setAlgorithm("PBKDF2WithHmacSHA256")
                .setHashByteSize(32)
        ),

        /**
         * PBKDF2 with HMac SHA-512.
         */
        $pbkdf2_sha512$(
            $pbkdf2_sha1$.get().getBuilder()
                .setAlgorithm("PBKDF2WithHmacSHA512")
                .setHashByteSize(64)
        ),

        /**
         * Argon2 with variant argon2i.
         */
        $argon2i$(
            Hasher.Builder.builder()
                .setClass(Argon2Hasher.class)
                .setScheme("Argon2")
                .setVariant("argon2i")
                .setAlgorithm("Blake2b") // because it is its name
                .setHashByteSize(32)
                .setSaltByteSize(16)
                .setEncoding(BytesEncoder.base64_no_padding)
                .setFormatter(new Argon2CryptFormatter())
        ),

        /**
         * Argon2 with variant argon2d.
         */
        $argon2d$(
            $argon2i$.get().getBuilder()
                .setVariant("argon2d")
        ),

        /**
         * Argon2 with variant argon2id.
         */
        $argon2id$(
            $argon2i$.get().getBuilder()
                .setVariant("argon2id")
        );

        private Hasher.Builder builder;

        @Override
        public Hasher get() {
            return this.builder.build();
        }

        Hashers(Hasher.Builder builder) {
            this.builder = builder;
            EnumUtil.replace(this, s -> s.replace('_', '-'));
        }

    }

}
