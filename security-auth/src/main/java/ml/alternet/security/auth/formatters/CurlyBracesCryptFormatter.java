package ml.alternet.security.auth.formatters;

import static ml.alternet.misc.Thrower.safeCall;
import static ml.alternet.security.auth.formatters.Util.decode;
import static ml.alternet.security.auth.formatters.Util.ensure;

import java.util.Optional;

import ml.alternet.encode.BytesEncoding;
import ml.alternet.scan.Scanner;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat.Hashers;
import ml.alternet.security.auth.formats.SchemeWithEncoding;

/**
 * Crypt formatter for <tt>{scheme}hash</tt> and <tt>{scheme.encoding}hash</tt>.
 *
 * The encoding can be "HEX" (hexa) or "b64" (base64), set as the variant "withEncoding".
 *
 * @author Philippe Poulard
 *
 * @see Hashers#MD5
 * @see Hashers#SHA
 */
public class CurlyBracesCryptFormatter implements CryptFormatter<CryptParts> {

    public static Hasher parseScheme(Scanner crypt, Hasher hr) {
        return safeCall(() -> {
            Hasher hasher = hr;
            SchemeWithEncoding swe = new SchemeWithEncoding(
                crypt,
                c -> safeCall(() -> c.hasNextChar('{', true)),
                '}'
            );
            swe.scheme.orElseThrow(() -> safeCall(() -> new IllegalArgumentException(crypt.getRemainderString().get())));
            Optional<BytesEncoding> encoding = swe.encoding.get();
            if (encoding.isPresent()) {
                hasher = hr.getBuilder().setEncoding(encoding.get())
                    .setVariant("withEncoding")
                    .build();
            };
            ensure(crypt.hasNextChar('}', true));
            return hasher;
        });
    }

    @Override
    public CryptParts parse(String crypt, Hasher hr) {
        Scanner scanner = Scanner.of(crypt);
        Hasher hasher = parseScheme(scanner, hr);
        CryptParts parts = new CryptParts(hasher);
        parts.hash = decode(hr, scanner, c -> true); // decode till the end
        return parts;
    }

    @Override
    public String format(CryptParts parts) {
        StringBuffer buf = new StringBuffer();
        buf.append('{');
        buf.append(parts.hr.getConfiguration().getScheme());
        String variant = parts.hr.getConfiguration().getVariant();
        if ("withEncoding".equals(variant)) {
            BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
            String encName = encoding.name();
            if (encName.startsWith("hexa")) {
                buf.append(".HEX");
            } else if (encName.startsWith("base64")) {
                buf.append(".b64");
            }
        }
        buf.append('}');
        if (parts.hash != null && parts.hash.length > 0) {
            buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.hash));
        }
        return buf.toString();
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new CurlyBracesCryptFormat();
    }

}
