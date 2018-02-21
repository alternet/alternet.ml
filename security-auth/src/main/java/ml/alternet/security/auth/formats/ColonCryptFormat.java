package ml.alternet.security.auth.formats;

import java.util.Optional;

import javax.inject.Singleton;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.encode.BytesEncoding;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.hashers.CurlyBracesCryptFormatHashers;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.util.StringUtil;

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
 * @author Philippe Poulard
 */
@Singleton
public class ColonCryptFormat implements CryptFormat {

    @Override
    public Optional<Hasher> resolve(String crypt) {
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
                        .get().getBuilder()
                        .setFormatter(COLON_CRYPT_FORMATTER);
                } catch (Exception e) {
                    LOGGER.fine("No crypt format found for " + scheme + " for " + family());
                }
            }
        }
        return Optional.ofNullable(b).map(Hasher.Builder::build);
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

    /**
     * The colon crypt formatter :
     *
     * <p>"<tt>PBKDF2:999:0314E17362D0D966C8F999A66045210DBE7EA897F024E07F:3AE344F7AB5AA17308A49FDAD997105340DD6E348FDF5623</tt>"</p>
     *
     * @see ModularCryptFormatHashers
     */
    public static final CryptFormatter<WorkFactorSaltedParts> COLON_CRYPT_FORMATTER = new CryptFormatter<WorkFactorSaltedParts>() {
        @Override
        public WorkFactorSaltedParts parse(String crypt, Hasher hr) {
            WorkFactorSaltedParts parts = new WorkFactorSaltedParts(hr);
            String[] stringParts = crypt.split(":");
            if (stringParts.length > 1 && ! StringUtil.isVoid(stringParts[1])) {
                parts.workFactor = Integer.parseInt(stringParts[1]);
            }
            BytesEncoding encoding = hr.getConfiguration().getEncoding();
            if (stringParts.length > 2 && ! StringUtil.isVoid(stringParts[2])) {
                String salt = stringParts[2];
                parts.salt = encoding.decode(salt);
            }
            if (stringParts.length > 3 && ! StringUtil.isVoid(stringParts[3])) {
                String hash = stringParts[3];
                parts.hash = encoding.decode(hash);
            }
            return parts;
        }

        @Override
        public String format(WorkFactorSaltedParts parts) {
            StringBuffer crypt = new StringBuffer(60);
            BytesEncoding encoding = parts.hr.getConfiguration().getEncoding();
            crypt.append(parts.hr.getConfiguration().getScheme())
                .append(':')
                .append(Integer.toString(parts.workFactor))
                .append(':')
                .append(encoding.encode(parts.salt));
            if (parts.hash != null && parts.hash.length > 0) {
                crypt.append(':')
                    .append(encoding.encode(parts.hash));
            }
            return crypt.toString();
        }

        @Override
        public CryptFormat getCryptFormat() {
            return new ColonCryptFormat();
        }
    };

}
