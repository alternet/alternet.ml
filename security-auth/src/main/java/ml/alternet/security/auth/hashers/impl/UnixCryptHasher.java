package ml.alternet.security.auth.hashers.impl;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import ml.alternet.security.Password;
import ml.alternet.security.algorithms.UnixCrypt;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.CryptFormatter;
import ml.alternet.security.auth.formats.SaltedParts;
import ml.alternet.security.auth.formats.UnixCryptFormat;
import ml.alternet.security.binary.BytesEncoder;
import ml.alternet.security.binary.BytesEncoding;
import ml.alternet.security.binary.SafeBuffer;

/**
 * The Unix Crypt hasher.
 *
 * @author Philippe Poulard
 */
public class UnixCryptHasher extends HasherBase<SaltedParts> {

    /**
     * The builder for this hasher.
     *
     * @return The Unix Crypt hasher builder.
     */

    /**
     * UnixCryptHasher constructor.
     *
     * @param config The configuration of this hasher.
     */
    public UnixCryptHasher(Configuration config) {
        super(config);
    }

    /**
     * The Unix Crypt formatter : 2 characters salt followed by the hash,
     * each character encoded in the h64 encoder value space.
     */
    public static final CryptFormatter<SaltedParts> UNIXCRYPT_FORMATTER = new CryptFormatter<SaltedParts>() {
        @Override
        public SaltedParts parse(String crypt, Hasher hr) {
            SaltedParts parts = new SaltedParts(hr);
            BytesEncoding encoding = hr.getConfiguration().getEncoding();
            String b64 = new String(BytesEncoder.ValueSpace.valueSpace(encoding));
            // characters are individually mapped to the value space
            parts.salt = new byte[2];
            parts.salt[0] = (byte) b64.indexOf(crypt.charAt(0));
            parts.salt[1] = (byte) b64.indexOf(crypt.charAt(1));
            parts.hash = new byte[11];
            for (int i = 2 ; i < 13 ; i++) {
                parts.hash[i-2] = (byte) b64.indexOf(crypt.charAt(i));
            }
            return parts;
        }

        @Override
        public String format(SaltedParts parts) {
            byte[] b = new byte[13];
            b[0] = parts.salt[0];
            b[1] = parts.salt[1];
            System.arraycopy(parts.hash, 0, b, 2, 11);
            return new String(b, 0, 13, StandardCharsets.US_ASCII);
        }

		@Override
		public CryptFormat getCryptFormat() {
			return new UnixCryptFormat();
		}
    };

    @Override
    public String getScheme() {
        return "CRYPT";
    }

    @Override
    public byte[] encrypt(Credentials credentials, SaltedParts parts) {
        long keyword = 0L;
        int salt = (parts.salt[1] << 6) | parts.salt[0];
        try (Password.Clear clear = credentials.getPassword().getClearCopy()) {
            byte[] pwd = SafeBuffer.getData(
                SafeBuffer.encode(
                    CharBuffer.wrap(clear.get()), getConfiguration().getCharset()
                )
            );
            for (int i = 0; i < 8; i++) {
                                                                // cast byte to int
                keyword = (keyword << 8) | ((i < pwd.length) ? 2 * (pwd[i]  &  0xff) : 0);
            }
        }
        try {
            return UnixCrypt.encrypt(salt, keyword);
        } finally {
            keyword = 0L;
        }
    }

    @Override
    public SaltedParts initializeParts() {
        SaltedParts parts = new SaltedParts(this);
        SecureRandom rand = new SecureRandom();
        parts.salt = new byte[2];
        rand.nextBytes(parts.salt);
        // bytes are not in the right range
        parts.salt[0] %= 64;
        parts.salt[1] %= 64;
        return parts;
    }

}
