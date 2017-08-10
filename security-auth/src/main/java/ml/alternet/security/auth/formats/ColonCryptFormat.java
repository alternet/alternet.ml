package ml.alternet.security.auth.formats;

import java.util.Optional;

import javax.inject.Singleton;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.hashers.CurlyBracesCryptFormatHashers;

/**
 * With the Colon Crypt format, the parts are separated by ":".
 *
 * <h1>Examples :</h1>
 * <ul>
 * <li><pre>PBKDF2:1000:uGWNzmy5WSU7dlwF6WQp0oFysI6bbnXD:u+BetVYiks7q3Gu
 * 9SR6B4i+8ccTMTq2/</pre></li>
 * <li><pre>PBKDF2:999:0314E17362D0D966C8F999A66045210DBE7EA897F024E07F:
 * 3AE344F7AB5AA17308A49FDAD997105340DD6E348FDF5623</pre></li>
 * </ul>
 *
 * Alternet Security supply an implementation in a separate module
 * (see <a href="http://alternet.ml/alternet-libs/security/security.html">the documentation</a>).
 *
 * @see ml.alternet.security.auth.hashers.impl.PBKDF2Hasher
 *
 * @author Philippe Poulard
 */
@Singleton
public class ColonCryptFormat implements CryptFormat {

    @Override
    public Optional<Hasher.Builder> resolve(String crypt) {
        String[] parts = crypt.split(":");
        Hasher.Builder b = null;
        if (parts.length > 0) {
            String scheme = parts[0];
            String lookupKey = Hasher.Builder.class.getCanonicalName() + "/" + family() + "/" + scheme;
            try {
                Class<Hasher.Builder> clazz = DiscoveryService.lookup(lookupKey);
                if (clazz != null) {
                    b = clazz.newInstance();
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException cnfe) {
                LOGGER.warning(cnfe.toString());
            }
            if (b == null) {
                try {
                    b = CurlyBracesCryptFormatHashers.valueOf(scheme)
                            .get();
                } catch (Exception e) {
                    LOGGER.fine("No crypt format found for " + scheme + " for " + family());
                }
            }
        }
        return Optional.ofNullable(b);
    }

    /**
     * @return "ColonCryptFormat"
     */
    @Override
    public String family() {
        return "ColonCryptFormat";
    }

    /**
     * @return "[scheme]:[shemeSpecificPart]"
     */
    @Override
    public String infoTemplate() {
        return "[scheme]:[shemeSpecificPart]";
    }

    public static class CryptFormatter implements ml.alternet.security.auth.formats.CryptFormatter<CryptParts> {

        @Override
        public SaltedParts parse(String crypt, Hasher hr) {
            return null;
//            String[] fields = crypt.split("\\$");
//            BytesEncoding encoding = hr.getConfiguration().getEncoding();
//            CryptParts parts = new SaltedParts(hr);
//            if (fields.length > 2 && ! StringUtil.isVoid(fields[2])) {
//                parts.salt = encoding.decode(fields[2]);
//            }
//            if (fields.length > 3 && ! StringUtil.isVoid(fields[3])) {
//                parts.hash = encoding.decode(fields[3]);
//            }
//            return parts;
        }

        @Override
        public String format(CryptParts parts) {
            return null;
//            StringBuffer buf = new StringBuffer();
//            buf.append('$');
//            String code = parts.hr.getConfiguration().getVariant();
//            if (StringUtil.isVoid(code)) {
//                code = parts.hr.getConfiguration().getAlgorithm();
//            }
//            buf.append(code);
//            buf.append('$');
//            if (parts.salt != null && parts.salt.length > 0) {
//                buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.salt));
//            }
//            buf.append('$');
//            if (parts.hash != null && parts.hash.length > 0) {
//                buf.append(parts.hr.getConfiguration().getEncoding().encode(parts.hash));
//            }
//            return buf.toString();
        }

    }

}
