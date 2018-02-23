package ml.alternet.security.auth.formatters;

import java.nio.ByteBuffer;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.auth.formats.PlainTextCryptFormat;
import ml.alternet.security.binary.SafeBuffer;

/**
 * Convert string to bytes and vice-versa with the supplied
 * bytes encoder or by default with the supplied charset.
 *
 * @author Philippe Poulard
 */
public class PlainTextCryptFormatter implements CryptFormatter<CryptParts> {

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

}