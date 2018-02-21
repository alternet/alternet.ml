package ml.alternet.security.auth.formats;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Optional;

import javax.inject.Singleton;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.Password;
import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.auth.hasher.HasherBase;
import ml.alternet.security.binary.SafeBuffer;

/**
 * A fallback format for passwords stored in plain text ;
 * DO NOT USE IN PRODUCTION.
 *
 * Can be use as the last crypt format to resolve.
 *
 * @author Philippe Poulard
 */
@Singleton
public class PlainTextCryptFormat implements CryptFormat {

    public static Hasher get() {
        return Hasher.Builder.builder()
            .setClass(PlainTextHasher.class)
            .setScheme("[CLEAR PASSWORD]")
            .setFormatter(PLAIN_FORMATTER)
            .build();
    }

    @Override
    public Optional<Hasher> resolve(String crypt) {
        return Optional.of(get());
    }

    /**
     * Just encode the chars with the charset supplied without hashing ;
     * DO NOT USE IN PRODUCTION.
     *
     * @author Philippe Poulard
     */
    public static class PlainTextHasher extends HasherBase<CryptParts> {
        public PlainTextHasher(Configuration conf) {
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

    /**
     * Convert string to bytes and vice-versa with the supplied
     * bytes encoder or by default with the supplied charset.
     */
    private static final CryptFormatter<? extends CryptParts> PLAIN_FORMATTER = new CryptFormatter<CryptParts>() {
        @Override
        public CryptParts parse(String crypt, Hasher hr) {
            CryptParts parts = new CryptParts(hr);
            BytesEncoding encoding  = parts.hr.getConfiguration().getEncoding();
            if (encoding == null) {
                parts.hash = SafeBuffer.getData(
                    hr.getConfiguration().getCharset().encode(crypt)
                );
            } else {
                // prefer a byte encoder if one is supplied
                parts.hash = encoding.decode(crypt);
            }
            return parts;
        }
        @Override
        public String format(CryptParts parts) {
            BytesEncoding encoding  = parts.hr.getConfiguration().getEncoding();
            if (encoding == null) {
                return parts.hr.getConfiguration()
                    .getCharset()
                    .decode(ByteBuffer.wrap(parts.hash))
                    .toString();
            } else {
                // prefer a byte encoder if one is supplied
                return encoding.encode(parts.hash);
            }
        }

        @Override
        public CryptFormat getCryptFormat() {
            return new PlainTextCryptFormat();
        }

    };

    /**
     * @return ""
     */
    @Override
    public String family() {
        return "";
    }

    /**
     * @return "[CLEAR PASSWORD]"
     */
    @Override
    public String infoTemplate() {
        return "[CLEAR PASSWORD]";
    }

}
