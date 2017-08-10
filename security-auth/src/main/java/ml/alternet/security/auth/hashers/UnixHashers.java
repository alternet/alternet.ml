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
 * @author Philippe Poulard
 */
public enum UnixHashers implements Supplier<Hasher.Builder> {

    /**
     * DES, the traditional UnixCrypt algorithm.
     * Only the first 8 chars of the passwords are used in the DES algorithm !
     */
    UNIX_CRYPT {
        @Override
        public Hasher.Builder get() {
            return Hasher.builder()
                .setClass(UnixCryptHasher.class)
                .setFormatter(UnixCryptHasher.UNIXCRYPT_FORMATTER)
                .setEncoding(BytesEncoder.h64);
        }
    },

    /**
     * MD5 32 hexa character hash.
     */
    MD5 {
        @Override
        public Hasher.Builder get() {
            return Hasher.builder()
                .setClass(MessageHasher.class)
                .setAlgorithm("MD5")
                .setEncoding(BytesEncoder.hexa)
                .setFormatter(CryptParts.CRYPT_FORMATTER);
        }
    };

}
