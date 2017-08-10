package ml.alternet.security.auth.hashers.impl;

import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.formats.SaltedParts;
import ml.alternet.security.auth.hashers.CurlyBracesCryptFormatHashers;
import ml.alternet.security.binary.SafeBuffer;

/**
 * Compute a checksum as the raw digest of the the string {password}{salt}.
 *
 * Hash generation can use a salt with a size between 4 and 16 bytes.
 *
 * @see CurlyBracesCryptFormatHashers#SMD5
 * @see CurlyBracesCryptFormatHashers#SSHA
 *
 * @author Philippe Poulard
 */
public class SaltedMessageHasher extends HasherBase<SaltedParts> {

    public SaltedMessageHasher(Configuration config) {
        super(config);
    }

    @Override
    public String getScheme() {
        return getConfiguration().getAlgorithm();
    }

    @Override
    public byte[] encrypt(Credentials credentials, SaltedParts parts) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(getConfiguration().getAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            try {
                md = DiscoveryService.newInstance(MessageDigest.class.getCanonicalName() + '/' + getConfiguration().getAlgorithm());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e1) {
                return Thrower.doThrow(e);
            }
        }
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
        SaltedParts parts = new SaltedParts(this);
        parts.generateSalt(4, 16);
        return parts;
    }

}
