package ml.alternet.security.auth.formats;

import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Singleton;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formatters.UnixCryptFormatter;
import ml.alternet.security.auth.formatters.UnixMD5CryptFormatter;
import ml.alternet.security.auth.hasher.MessageHasher;
import ml.alternet.security.auth.hasher.UnixCryptHasher;

/**
 * Legacy Unix Crypt Formats :
 *
 * <ul>
 * <li>A DES-crypt hash string consists of 13 characters, drawn from [./0-9A-Za-z] :<br>
 * <tt>password</tt> -&gt; <tt>JQMuyS6H.AGMo</tt>
 * </li>
 * <li>A fallback to MD5 32 hexa character hash :<br>
 * <tt>11fbe079ed3476f7712030d24042ca35</tt>
 * </li>
 * </ul>
 *
 * @author Philippe Poulard
 */
@Singleton
public class UnixCryptFormat implements CryptFormat {

    @Override
    public Optional<Hasher> resolve(String crypt) {
        if (crypt.length() == 13) {
            // unix crypt
            return Optional.of(Hashers.UNIX_CRYPT.get());
        } else if (crypt.length() == 32) {
            // traditional MD5 : 32 hexa characters
            return Optional.of(Hashers.MD5.get());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String family() {
        return "UNIX_CRYPT";
    }

    @Override
    public String infoTemplate() {
        return "sshhhhhhhhhhh | xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    }

    /**
     * Unix algorithms.
     *
     * <h1>Configuration</h1>
     *
     * You can examine the configuration of each value
     * <a href="https://github.com/alternet/alternet.ml/blob/master/security-auth/src/main/java/ml/alternet/security/formats/UnixCryptFormat.java">here</a>.
     *
     * @author Philippe Poulard
     */
    public enum Hashers implements Supplier<Hasher> {

        /**
         * DES, the traditional UnixCrypt algorithm.
         * Only the first 8 chars of the passwords are used in the DES algorithm !
         */
        UNIX_CRYPT(
            Hasher.Builder.builder()
                .setClass(UnixCryptHasher.class)
                .setScheme("CRYPT")
                .setFormatter(new UnixCryptFormatter())
                .setEncoding(BytesEncoder.h64)
        ),

        /**
         * MD5 32 hexa character hash.
         */
        MD5(
            Hasher.Builder.builder()
                .setClass(MessageHasher.class)
                .setAlgorithm("MD5")
                .setEncoding(BytesEncoder.hexa)
                .setFormatter(new UnixMD5CryptFormatter())
        );

        private Hasher.Builder builder;

        @Override
        public Hasher get() {
            return this.builder.build();
        }

        Hashers(Hasher.Builder builder) {
            this.builder = builder;
        }

    }

}
