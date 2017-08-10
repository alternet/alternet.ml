package ml.alternet.security.auth.hashers.impl;

import java.nio.CharBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.misc.Thrower;
import ml.alternet.security.Password;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.formats.CryptParts;
import ml.alternet.security.binary.SafeBuffer;

public class MessageHasher extends HasherBase<CryptParts> {

    public MessageHasher(Configuration config) {
        super(config);
    }

    @Override
    public String getScheme() {
        return getConfiguration().getAlgorithm();
    }

    @Override
    public byte[] encrypt(Credentials credentials, CryptParts parts) {
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

}
