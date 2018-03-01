package ml.alternet.security.auth.formatters;

import java.nio.charset.StandardCharsets;

import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.SaltedParts;
import ml.alternet.security.auth.hasher.MD5BasedHasher;
import ml.alternet.security.auth.formats.ModularCryptFormat;
import ml.alternet.security.auth.formats.ModularCryptFormat.Hashers;
import ml.alternet.util.StringUtil;

/**
 * The most popular formatter for this hasher is the Modular Crypt Format.
 *
 * <ul>
 * <li>Apache MD5 crypt format :
 * <pre>$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1</pre></li>
 * <li>Crypt MD5 :
 * <pre>$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.</pre></li>
 * </ul>
 *
 * @see Hashers#$apr1$
 * @see Hashers#$1$
 *
 * @author Philippe Poulard
 */
public class MD5ModularCryptFormatter implements CryptFormatter<SaltedParts> {

    @Override
    public SaltedParts parse(String crypt, Hasher hr) {
        String[] stringParts = crypt.split("\\$");
        SaltedParts parts = new SaltedParts(hr);
        if (! stringParts[1].equals(parts.hr.getConfiguration().getVariant())) {
            parts.hr = parts.hr.getBuilder().setVariant(stringParts[1]).build();
        }
        if (stringParts.length > 2 && ! StringUtil.isVoid(stringParts[2])) {
            parts.salt = stringParts[2].getBytes(StandardCharsets.US_ASCII);
        }
        if (stringParts.length > 3 && ! StringUtil.isVoid(stringParts[3])) {
            parts.hash = hr.getConfiguration().getEncoding().decode(stringParts[3]);
        }
        return parts;
    }

    @Override
    public String format(SaltedParts parts) {
        boolean isApacheVariant = MD5BasedHasher.isApacheVariant(parts.hr);
        StringBuffer buf = new StringBuffer(isApacheVariant ? 37 : 34);
        buf.append('$');
        buf.append(parts.hr.getConfiguration().getVariant());
        buf.append("$");
        buf.append(new String(parts.salt, StandardCharsets.US_ASCII));
        buf.append('$');
        buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.hash));
        return buf.toString();
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new ModularCryptFormat();
    }

}