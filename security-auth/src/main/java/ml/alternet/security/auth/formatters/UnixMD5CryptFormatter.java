package ml.alternet.security.auth.formatters;

import java.util.Optional;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.auth.hashers.UnixHashers;

/**
 * Trivial formatter for a crypt made of just a hash field.
 *
 * @author Philippe Poulard
 */
public class UnixMD5CryptFormatter implements CryptFormatter<CryptParts> {

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

}