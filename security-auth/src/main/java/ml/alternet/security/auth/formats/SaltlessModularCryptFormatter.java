package ml.alternet.security.auth.formats;

import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.binary.BytesEncoding;
import ml.alternet.util.StringUtil;

/**
 * Used by the NT-HASH algorithm (Microsoft Windows NT and successors) that looks
 * like a salted crypt, but without a salt field.
 *
 * <p>"<tt>password</tt>" -&gt; "<tt>$3$$8846f7eaee8fb117ad06bdd830b7586c</tt>"</p>
 *
 * @author Philippe Poulard
 */
public class SaltlessModularCryptFormatter implements CryptFormatter<CryptParts> {

    public static final CryptFormatter<CryptParts> INSTANCE = new SaltlessModularCryptFormatter();

    @Override
    public CryptParts parse(String crypt, Hasher hr) {
        String[] fields = crypt.split("\\$");
        BytesEncoding encoding = hr.getConfiguration().getEncoding();
        CryptParts parts = new CryptParts(hr);
        if (fields.length > 3 && ! StringUtil.isVoid(fields[3])) {
            parts.hash = encoding.decode(fields[3]);
        }
        return parts;
    }

    @Override
    public String format(CryptParts parts) {
        StringBuffer buf = new StringBuffer();
        buf.append('$');
        String code = parts.hr.getConfiguration().getVariant();
        if (StringUtil.isVoid(code)) {
            code = parts.hr.getConfiguration().getAlgorithm();
        }
        buf.append(code);
        buf.append("$$");
        if (parts.hash != null && parts.hash.length > 0) {
            buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.hash));
        }
        return buf.toString();
    }

	@Override
	public CryptFormat getCryptFormat() {
		return new ModularCryptFormat();
	}

}
