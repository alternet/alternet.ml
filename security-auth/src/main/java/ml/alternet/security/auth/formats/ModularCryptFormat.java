package ml.alternet.security.auth.formats;

import java.util.Optional;

import javax.inject.Singleton;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.hashers.ModularCryptFormatHashers;
import ml.alternet.util.StringUtil;

/**
 * The <a href="https://pythonhosted.org/passlib/modular_crypt_format.html">Modular Crypt Format</a>
 * is an ad-hoc standard used in popular linux-based systems.
 *
 * Format : <code>$[scheme]$[salt]$[crypt]</code>
 *
 * <table summary="">
 * <tr><th>ID</th><th>Method</th></tr>
 * <tr><td>1</td><td>MD5</td></tr>
 * <tr><td>2a</td><td>Blowfish</td></tr>
 * <tr><td>5</td><td>SHA-256</td></tr>
 * <tr><td>6</td><td>SHA-512</td></tr>
 * </table>
 *
 * <h1>Examples :</h1>
 * <ul>
 * <li>Apache MD5 crypt format :
 * <pre>$apr1$jgwedrkq$jzeetEHMGal5H0SUFDMEl1</pre></li>
 * <li>Crypt MD5 :
 * <pre>$1$3iuE5z/b$JHyXMzQOIq3cl6WlEMoZC.</pre></li>
 * </ul>
 *
 * @author Philippe Poulard
 */
@Singleton
public class ModularCryptFormat implements CryptFormat {

    @Override
    public Optional<Hasher.Builder> resolve(String crypt) {
        String[] parts = crypt.split("\\$");
        Hasher.Builder b = null;
        if (parts.length > 1 && StringUtil.isVoid(parts[0])) {
            String scheme = parts[1];
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
                    b = ModularCryptFormatHashers.valueOf('$' + scheme + '$')
                            .get();
                } catch (Exception e) {
                    LOGGER.fine("No crypt format found for " + scheme + " for " + family());
                }
            }
        }
        return Optional.ofNullable(b);
    }

    /**
     * @return "ModularCryptFormat"
     */
    @Override
    public String family() {
        return "ModularCryptFormat";
    }

    /**
     * @return "$[scheme]$[salt]$[crypt]"
     */
    @Override
    public String infoTemplate() {
        return "$[scheme]$[salt]$[crypt]";
    }

}
