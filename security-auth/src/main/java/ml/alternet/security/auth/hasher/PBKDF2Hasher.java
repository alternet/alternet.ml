package ml.alternet.security.auth.hasher;

import java.security.InvalidAlgorithmParameterException;
import java.util.logging.Logger;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.misc.Thrower;
import ml.alternet.security.algorithms.PBKDF2;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formats.CryptFormatter;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;

/**
 * A slightly modified PBKDF2 hasher, that erase intermediate data.
 *
 * The underlying MAC algorithm is unchanged.
 *
 * @author Philippe Poulard
 */
public class PBKDF2Hasher extends HasherBase<WorkFactorSaltedParts> {

    static final Logger LOGGER = Logger.getLogger(PBKDF2Hasher.class.getName());

    /**
     * Create a PBKDF2 hasher.
     *
     * @param config The configuration of this hasher.
     */
    public PBKDF2Hasher(Builder config) {
        super(config);
    }

    @Override
    public byte[] encrypt(Credentials credentials, WorkFactorSaltedParts parts) {
        int hashByteSize = parts.hr.getConfiguration().getHashByteSize();
        try {
            return PBKDF2.hash(
                credentials.getPassword(),
                parts.salt,
                parts.hr.getConfiguration().getAlgorithm(),
                parts.workFactor,
                hashByteSize
            );
        } catch (InvalidAlgorithmParameterException e) {
            return Thrower.doThrow(e);
        }
    }

    @Override
    public WorkFactorSaltedParts initializeParts() {
        WorkFactorSaltedParts parts = new WorkFactorSaltedParts(this);
        parts.generateSalt(); // TODO: check parameter and min-max
        parts.workFactor = getConfiguration().getIterations();
        return parts;
    }

    /**
     * One popular formatter for PBKFD2 family is the Modular Crypt Format.
     *
     * <p>"<tt>password</tt>" -&gt; "<tt>$pbkdf2-sha256$6400$0ZrzXitFSGltTQnBWOsdAw$Y11AchqV4b0sUisdZd0Xr97KWoymNE0LNNrnEgY4H9M</tt>"</p>
     *
     * @see ModularCryptFormatHashers
     */
    public static final CryptFormatter<WorkFactorSaltedParts> MCF_FORMATTER = new CryptFormatter<WorkFactorSaltedParts>() {
        @Override
        public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
            // $pbkdf2-digest$rounds$salt$checksum
            String[] fields = crypt.split("\\$");
            String[] digest = fields[1].split("-");
            String algo = "PBKDF2WithHmac" + digest[1].toUpperCase(); // PBKDF2WithHmacSHA1
            WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hr);
            if (! algo.equals(parts.hr.getConfiguration().getAlgorithm())) {
                hr = parts.hr.getBuilder().setAlgorithm(algo).build();
                parts.hr = hr;
            }
            parts.workFactor = Integer.parseInt(fields[2]);
            BytesEncoding encoding = hr.getConfiguration().getEncoding();
            parts.salt = encoding.decode(fields[3]);
            parts.hash = encoding.decode(fields[4]);
            return parts;
        }

        @Override
        public String format(WorkFactorSaltedParts parts) {
            StringBuffer buf = new StringBuffer();
            buf.append('$');
            buf.append("pbkdf2-");
            String algo = parts.hr.getConfiguration().getAlgorithm(); // PBKDF2WithHmacSHA1
            buf.append(algo.substring("PBKDF2WithHmac".length()).toLowerCase());
            buf.append('$');
            buf.append(parts.workFactor);
            buf.append('$');
            buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.salt));
            buf.append('$');
            buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.hash));
            return buf.toString();
        }

        @Override
        public CryptFormat getCryptFormat() {
            return new ModularCryptFormat();
        }
    };

}
