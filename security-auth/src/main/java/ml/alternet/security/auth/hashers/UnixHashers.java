package ml.alternet.security.auth.hashers;

import java.util.function.Supplier;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formatters.UnixMD5CryptFormatter;
import ml.alternet.security.auth.hasher.MessageHasher;
import ml.alternet.security.auth.hasher.UnixCryptHasher;
import ml.alternet.security.auth.formatters.UnixCryptFormatter;

/**
 * Unix algorithms.
 *
 * <h1>Configuration</h1>
 *
 * You can examine the configuration of each value
 * <a href="https://github.com/alternet/alternet.ml/blob/master/security-auth/src/main/java/ml/alternet/security/auth/hashers/UnixHashers.java">here</a>.
 *
 * @author Philippe Poulard
 */
public enum UnixHashers implements Supplier<Hasher> {

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

    UnixHashers(Hasher.Builder builder) {
        this.builder = builder;
    }

}
