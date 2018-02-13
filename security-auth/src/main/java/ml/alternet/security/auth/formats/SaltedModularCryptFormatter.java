package ml.alternet.security.auth.formats;

import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.SaltedParts;
import ml.alternet.security.binary.BytesEncoding;
import ml.alternet.util.StringUtil;

/**
 *
 * @author Philippe Poulard
 */
public class SaltedModularCryptFormatter implements CryptFormatter<SaltedParts> {

    public static final CryptFormatter<SaltedParts> INSTANCE = new SaltedModularCryptFormatter();

    @Override
    public SaltedParts parse(String crypt, Hasher hr) {
        String[] fields = crypt.split("\\$");
        BytesEncoding encoding = hr.getConfiguration().getEncoding();
        SaltedParts parts = new SaltedParts(hr);
        if (fields.length > 2 && ! StringUtil.isVoid(fields[2])) {
            parts.salt = encoding.decode(fields[2]);
        }
        if (fields.length > 3 && ! StringUtil.isVoid(fields[3])) {
            parts.hash = encoding.decode(fields[3]);
        }
        return parts;
    }

    @Override
    public String format(SaltedParts parts) {
        StringBuffer buf = new StringBuffer();
        buf.append('$');
        String code = parts.hr.getConfiguration().getVariant();
        if (StringUtil.isVoid(code)) {
            code = parts.hr.getConfiguration().getAlgorithm();
        }
        buf.append(code);
        buf.append('$');
        if (parts.salt != null && parts.salt.length > 0) {
            buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.salt));
        }
        buf.append('$');
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
