package ml.alternet.security.auth.hasher;

import java.nio.CharBuffer;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.Password;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.Hasher.Builder;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.binary.SafeBuffer;

/**
 * Just encode the chars with the charset supplied without hashing ;
 * DO NOT USE IN PRODUCTION.
 *
 * @author Philippe Poulard
 */
public class PlainTextHasher extends HasherBase<CryptParts> {

    /**
     * Create a plain text hasher.
     *
     * @param conf The configuration.
     */
    public PlainTextHasher(Builder conf) {
        super(conf);
    }
    @Override
    public byte[] encrypt(Credentials credentials, CryptParts parts) {
        try (Password.Clear pwd = credentials.getPassword().getClearCopy()) {
            BytesEncoding encoding  = parts.hr.getConfiguration().getEncoding();
            if (encoding == null) {
                return SafeBuffer.getData(
                    SafeBuffer.encode(
                        CharBuffer.wrap(pwd.get()),
                        parts.hr.getConfiguration().getCharset()
                    )
                );
            } else {
                // prefer a byte encoder if one is supplied
                return encoding.decode(new String(pwd.get()));
            }
        }
    }
    @Override
    public CryptParts initializeParts() {
        return new CryptParts(this);
    }
}