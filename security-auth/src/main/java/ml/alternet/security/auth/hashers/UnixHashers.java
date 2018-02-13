package ml.alternet.security.auth.hashers;

import java.util.function.Supplier;

import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.CryptParts;
import ml.alternet.security.auth.hashers.impl.MessageHasher;
import ml.alternet.security.auth.hashers.impl.UnixCryptHasher;
import ml.alternet.security.binary.BytesEncoder;

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
    UNIX_CRYPT {
        @Override
        public Hasher get() {
            return Hasher.Builder.builder()
                .setClass(UnixCryptHasher.class)
                .setScheme("CRYPT")
                .setFormatter(UnixCryptHasher.UNIXCRYPT_FORMATTER)
                .setEncoding(BytesEncoder.h64)
                .build();
        }
    },

    /**
     * MD5 32 hexa character hash.
     */
    MD5 {
        @Override
        public Hasher get() {
            return Hasher.Builder.builder()
                .setClass(MessageHasher.class)
                .setAlgorithm("MD5")
                .setEncoding(BytesEncoder.hexa)
                .setFormatter(CryptParts.CRYPT_FORMATTER)
                .build();
        }
    };

}
