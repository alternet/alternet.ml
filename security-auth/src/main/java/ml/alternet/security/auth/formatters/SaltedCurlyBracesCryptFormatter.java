package ml.alternet.security.auth.formatters;

import static ml.alternet.security.auth.formatters.Util.decode;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.misc.Thrower;
import ml.alternet.scan.Scanner;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.SaltedParts;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat.Hashers;
import ml.alternet.security.auth.formats.SchemeWithEncoding;

/**
 * Salted crypt formatter for <tt>{scheme}hash</tt> and <tt>{scheme.encoding}hash</tt>.
 *
 * <ul>
 * <li>The encoding can be "HEX" (hexa) or "b64" (base64),
 * set as the variant "withEncoding".</li>
 * <li>{scheme} is {SMD5} or {SSHA}.</li>
 * <li>{hash} is the base64 encoding of {checksum}{salt};
 * and in turn {salt} is a multi-byte binary salt, and
 * {checksum} is the raw digest of the the string {password}{salt},
 * using the appropriate digest algorithm.</li>
 * </ul>
 *
 * @author Philippe Poulard
 *
 * @see Hashers#SMD5
 * @see Hashers#SSHA
 */
public class SaltedCurlyBracesCryptFormatter implements CryptFormatter<SaltedParts> {

    @Override
    public SaltedParts parse(String crypt, Hasher hr) {
        Scanner scanner = Scanner.of(crypt);
        Hasher hasher = CurlyBracesCryptFormatter.parseScheme(scanner, hr);
        SaltedParts parts = new SaltedParts(hasher);
        return Thrower.safeCall(() -> {
            if (scanner.hasNext()) {
                byte[] bytes = decode(hr, scanner, c -> true); // decode till the end
                int saltSize = hr.getConfiguration().getSaltByteSize();
                parts.salt = new byte[saltSize];
                System.arraycopy(bytes, bytes.length - saltSize, parts.salt, 0, saltSize);
                parts.hash = new byte[bytes.length - saltSize];
                System.arraycopy(bytes, 0, parts.hash, 0, bytes.length - saltSize);
            }
            return parts;
        });
    }

    @Override
    public String format(SaltedParts parts) {
        StringBuffer buf = new StringBuffer();
        buf.append('{');
        buf.append(parts.hr.getConfiguration().getScheme());
        String variant = parts.hr.getConfiguration().getVariant();
        if ("withEncoding".equals(variant)) {
            BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
            SchemeWithEncoding.code(encoding).ifPresent(enc -> buf.append('.').append(enc));
        }
        buf.append('}');
        if (parts.hash != null && parts.hash.length > 0) {
            byte[] bytes = new byte[parts.hash.length + parts.salt.length];
            System.arraycopy(parts.hash, 0, bytes, 0, parts.hash.length);
            System.arraycopy(parts.salt, 0, bytes, parts.hash.length, parts.salt.length);
            buf.append(parts.hr.getConfiguration().getEncoding().encode(bytes));
        }
        return buf.toString();
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new CurlyBracesCryptFormat();
    }
}