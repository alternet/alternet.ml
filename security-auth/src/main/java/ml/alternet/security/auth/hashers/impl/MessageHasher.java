package ml.alternet.security.auth.hashers.impl;

import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;
import ml.alternet.security.algorithms.MD4;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.formats.CryptParts;
import ml.alternet.security.binary.SafeBuffer;

/**
 * The hasher is based on Java's message digests. If the platform doesn't
 * supply the expected message digest, the discovery service tries to find
 * one with the algorithm name.
 *
 * @see MessageDigest
 * @see MD4
 *
 * @author Philippe Poulard
 */
public class MessageHasher extends HasherBase<CryptParts> {

    /**
     * Create a hasher.
     *
     * @param config The configuration of this hasher.
     */
    public MessageHasher(Configuration config) {
        super(config);
    }

    @Override
    public byte[] encrypt(Credentials credentials, CryptParts parts) {
        MessageDigest md = lookup(getConfiguration().getAlgorithm());
        try (Password.Clear clear = credentials.getPassword().getClearCopy()) {
            return md.digest(
                SafeBuffer.getData(
                    SafeBuffer.encode(
                        CharBuffer.wrap(clear.get()), getConfiguration().getCharset()
                    )
                )
            );
        }
    }

    @Override
    public CryptParts initializeParts() {
        return new CryptParts(this);
    }

    /**
     * Lookup for a message digest algorithm.
     *
     * The message digest algorithm is first looked up on Java's message digests.
     * If the platform doesn't supply the expected message digest, the discovery
     * service tries to find one.
     *
     * @see DiscoveryService
     *
     * @param algorithm The algorithm name.
     * @return The implementation.
     */
    public static MessageDigest lookup(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            try {
                return DiscoveryService.newInstance(MessageDigest.class.getCanonicalName() + '/' + algorithm);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e1) {
                return Thrower.doThrow(e);
            }
        }
    }

}
