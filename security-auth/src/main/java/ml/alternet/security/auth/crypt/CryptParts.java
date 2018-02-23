package ml.alternet.security.auth.crypt;

import java.util.Optional;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formats.CryptFormatter;
import ml.alternet.security.auth.hashers.UnixHashers;

/**
 * Base class for crypt parts.
 *
 * Subclasses contains additional fields when necessary
 * (typically, a salt).
 *
 * @see CryptFormatter
 *
 * @author Philippe Poulard
 */
public class CryptParts {

    /**
     * Create some crypt for a hasher
     *
     * @param hr The actual hasher.
     */
    public CryptParts(Hasher hr) {
        this.hr = hr;
    }

    /**
     * The hash field maybe initialized after parsing
     * when the purpose of the crypt is for checking some
     * credentials, or left blank on creation when the
     * purpose of the crypt is to compute a hash (then set
     * with the hash result before formatting a string
     * representation of this parts).
     */
    public byte[] hash;

    /**
     * The hasher that can process this crypt
     */
    public Hasher hr;

    /**
     * Trivial formatter for a crypt made of just a hash field.
     */
    public static final CryptFormatter<? extends CryptParts> CRYPT_FORMATTER = new CryptFormatter<CryptParts>() {
        @Override
        public CryptParts parse(String crypt, Hasher hr) {
            CryptParts parts = new CryptParts(hr);
            BytesEncoding encoding = hr.getConfiguration().getEncoding();
            parts.hash = encoding.decode(crypt);
            return parts;
        }
        @Override
        public String format(CryptParts parts) {
            return parts.hr.getConfiguration().getEncoding().encode(parts.hash);
        }
        @Override
        public CryptFormat getCryptFormat() {
            return new CryptFormat() {
                @Override
                public Optional<Hasher> resolve(String crypt) {
                    return Optional.of(UnixHashers.MD5.get());
                }
                @Override
                public String infoTemplate() {
                    return "";
                }
                @Override
                public String family() {
                    return "MD5 32 hexa character hash";
                }
            };
        }
    };

}
