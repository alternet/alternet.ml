package ml.alternet.security.auth.hashers.impl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.stream.IntStream;

import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;
import ml.alternet.security.algorithms.BCrypt;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.CryptFormatter;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.formats.WorkFactorSaltedParts;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.security.binary.BytesEncoder;
import ml.alternet.security.binary.BytesEncoding;
import ml.alternet.security.binary.SafeBuffer;
import ml.alternet.util.StringUtil;

/**
 * The BCrypt hasher.
 *
 * @author Philippe Poulard
 */
public class BCryptHasher extends HasherBase<WorkFactorSaltedParts> {

    /**
     * Create a BCrypt hasher.
     *
     * @param config The configuration of this hasher.
     */
    public BCryptHasher(Configuration config) {
        super(config);
    }

    static char getVersion(Hasher hr) {
        String variant = hr.getConfiguration().getVariant();
        if (variant != null && variant.length() > 1) {
            return variant.charAt(1);
        } else {
            return (char) 0;
        }
    }

    @Override
    public byte[] encrypt(Credentials credentials, WorkFactorSaltedParts parts) {
        ByteBuffer bb;
        try (Password.Clear pwd = credentials.getPassword().getClearCopy()) {
            bb = SafeBuffer.encode(CharBuffer.wrap(pwd.get()), getConfiguration().getCharset());
        }
        if (getVersion(this) >= 'a') {
            // append some more bytes to the buffer
            byte[] minorBytes = "\000".getBytes(getConfiguration().getCharset());
            bb = SafeBuffer.append(bb, minorBytes);
        }
        byte[] passwordb = SafeBuffer.getData(bb);
        byte[] hash = new BCrypt().crypt_raw(passwordb, parts.salt, parts.workFactor);
        Arrays.fill(passwordb, (byte) 0);

        // adjust size
        if (hash.length > BCrypt.resLen) {
            byte[] b = new byte[BCrypt.resLen];
            System.arraycopy(hash, 0, b, 0, BCrypt.resLen);
            hash = b;
        }
        return hash;
    }

    /**
     * The most popular formatter for BCrypt is the Modular Crypt Format.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>$2a$08$YkG5/ze2FPw8C6vuAs7WHuvS0IeyyQfLgE7Ti8tT5F2sMEkVJlNo.</tt>"</p>
     * <p>"<tt>password</tt>" -&gt; "<tt>$bcrypt-sha256$2a,12$LrmaIX5x4TRtAwEfwJZa1.$2ehnw6LvuIUTM0iz4iz9hTxv21B6KFO</tt>"</p>
     *
     * @see ModularCryptFormatHashers
     */
    public static final CryptFormatter<WorkFactorSaltedParts> BCRYPT_FORMATTER = new CryptFormatter<WorkFactorSaltedParts>() {
        @Override
        public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
            boolean hasAlgo = false;
            String[] stringParts = crypt.split("\\$");
            if (stringParts[1].startsWith("bcrypt-")) {
                hasAlgo = true;
                String algo = stringParts[1].substring("bcrypt-".length()); // sha256
                if (algo.startsWith("sha")) {
                    algo = "SHA-" + algo.substring("sha".length());
                } // SHA-256
                hr = hr.getBuilder()
                    .setClass(BCryptHasher.Digest.class)
                    .setAlgorithm(algo)
                    .build();
                String[] varRound = stringParts[2].split(","); // 2a,12
                stringParts[1] = varRound[0]; // 2a
                stringParts[2] = varRound[1]; // 12
            } // go on normally
            WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hr);
            if (! stringParts[1].equals(parts.hr.getConfiguration().getVariant())) {
                hr = parts.hr.getBuilder().setVariant(stringParts[1]).build();
                parts.hr = hr;
            }
            if (stringParts.length > 2 && ! StringUtil.isVoid(stringParts[2])) {
                parts.workFactor = Integer.parseInt(stringParts[2]);
            }
            if (stringParts.length > 3 && ! StringUtil.isVoid(stringParts[3])) {
                String salt;
                if (hasAlgo) {
                    salt = stringParts[3];
                } else {
                    salt =stringParts[3].substring(0, 22);
                }
                BytesEncoding encoding = hr.getConfiguration().getEncoding();
                parts.salt = encoding.decode(salt);
                String hash = "";
                if (hasAlgo) {
                    if (stringParts.length > 4 && ! StringUtil.isVoid(stringParts[4])) {
                        hash = stringParts[4];
                    }
                } else {
                    hash = stringParts[3].substring(22);
                }
                if (hash.length() > 0) {
                    parts.hash = encoding.decode(hash);
                }
            }
            return parts;
        }

        @Override
        public String format(WorkFactorSaltedParts parts) {
            String algo = parts.hr.getConfiguration().getAlgorithm(); // SHA-256
            boolean hasAlgo = ! StringUtil.isVoid(algo);
            StringBuffer crypt = new StringBuffer(60);
            if (hasAlgo) {
                if (algo.startsWith("SHA-")) {
                    algo = "sha" + algo.substring("SHA-".length());
                }
                crypt.append("$bcrypt-").append(algo); // sha256
            }
            crypt.append("$2");
            char version = getVersion(parts.hr);
            if (version >= 'a') {
                crypt.append(version);
            }
            crypt.append(hasAlgo ? ',' : '$');
            if (parts.workFactor < 10)
                crypt.append("0");
            if (parts.workFactor > 30) {
                throw new IllegalArgumentException(
                        "log_rounds exceeds maximum (30)");
            }
            crypt.append(Integer.toString(parts.workFactor));
            crypt.append("$");
            BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
            crypt.append(encoding.encode(parts.salt));
            if (parts.hash != null && parts.hash.length > 0) {
                if (hasAlgo) {
                    crypt.append('$');
                }
                crypt.append(encoding.encode(parts.hash));
            }
            return crypt.toString();
        }

        @Override
        public CryptFormat getCryptFormat() {
            return new ModularCryptFormat();
        }
    };

    @Override
    public WorkFactorSaltedParts initializeParts() {
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(this);
        parts.workFactor = getConfiguration().getLogRounds();
        parts.generateSalt();
        return parts;
    }

    /**
     * BCrypt truncate passwords to 72 bytes, and some other minor quirks. This class works around that
     * issue by first running the password through a digest such as SHA2-256.
     *
     * @author Philippe Poulard
     */
    public static class Digest extends BCryptHasher {

        /**
         * Create a BCrypt hasher.
         *
         * @param config The configuration of this hasher.
         */
        public Digest(Configuration config) {
            super(config);
        }

        @Override
        public byte[] encrypt(Credentials credentials, WorkFactorSaltedParts parts) {
            ByteBuffer bb;
            try (Password.Clear pwd = credentials.getPassword().getClearCopy()) {
                bb = SafeBuffer.encode(CharBuffer.wrap(pwd.get()), getConfiguration().getCharset());
            }
            MessageDigest digest = Thrower.safeCall(() ->
                MessageDigest.getInstance(getConfiguration().getAlgorithm())
            );
            digest.update(bb);
            byte[] bytes = digest.digest();

            // same as String key = BytesEncoder.base64.encode(bytes);
            // but with safe buffers
            CharBuffer[] cbuf = { CharBuffer.allocate(bytes.length * 2) };
            cbuf[0].flip();
            BytesEncoder.base64.encode(IntStream.range(0, bytes.length).map(i -> bytes[i]))
                .forEach(c -> cbuf[0] = SafeBuffer.append(cbuf[0], c));
            credentials = Credentials.fromPassword(SafeBuffer.getData(cbuf[0]));

            return super.encrypt(credentials, parts);
        }

    }

}
