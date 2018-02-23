package ml.alternet.security.auth.hasher;

import java.nio.CharBuffer;
import java.security.MessageDigest;

import ml.alternet.security.Password;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.crypt.SaltedParts;
import ml.alternet.security.auth.hashers.CurlyBracesCryptFormatHashers;
import ml.alternet.security.binary.SafeBuffer;

/**
 * Compute a checksum as the raw digest of the the string {password}{salt}.
 *
 * Hash generation can use a salt with a size between 4 and 16 bytes.
 *
 * The hasher is based on Java's message digests. If the platform doesn't
 * supply the expected message digest, the discovery service tries to find
 * one with the algorithm name.
 *
 * @see CurlyBracesCryptFormatHashers#SMD5
 * @see CurlyBracesCryptFormatHashers#SSHA
 *
 * @see MessageHasher#lookup(String)
 *
 * @author Philippe Poulard
 */
public class SaltedMessageHasher extends HasherBase<SaltedParts> {

    /**
     * Create a hasher.
     *
     * @param config The configuration of this hasher.
     */
    public SaltedMessageHasher(Builder config) {
        super(config);
    }

    @Override
    public byte[] encrypt(Credentials credentials, SaltedParts parts) {
        MessageDigest md = MessageHasher.lookup(getConfiguration().getAlgorithm());
        try (Password.Clear clear = credentials.getPassword().getClearCopy()) {
            return md.digest(
                SafeBuffer.getData(
                    SafeBuffer.append(
                        SafeBuffer.encode(
                            CharBuffer.wrap(clear.get()), getConfiguration().getCharset()
                        ),
                        parts.salt
                    )
                )
            );
        }
    }

    @Override
    public SaltedParts initializeParts() {
        int min = 4;
        int max = 16;
        SaltedParts parts = new SaltedParts(this);
        int saltSize = getConfiguration().getSaltByteSize();
        if (saltSize < min || saltSize > max) {
            throw new IllegalArgumentException("Invalid salt size " + saltSize + " ; must be between "
                        + min + " and " + max + " bytes.");
        }
        parts.generateSalt();
        return parts;
    }

}
