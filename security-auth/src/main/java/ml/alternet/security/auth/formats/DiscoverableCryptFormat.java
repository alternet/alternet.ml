package ml.alternet.security.auth.formats;

import ml.alternet.discover.DiscoveryService;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.Hasher.Builder;

/**
 * Allow to lookup a hasher builder with the discovery service.
 *
 * When requesting the discovery service, the lookup key is made
 * of <code>[{@link Builder}]/[{@link CryptFormat#family()}]/[scheme]</code>
 *
 * @author Philippe Poulard
 *
 * @see DiscoveryService
 */
public interface DiscoverableCryptFormat extends CryptFormat {

    /**
     * Lookup the hasher builder for the given scheme.
     *
     * @param scheme The scheme.
     *
     * @return The hasher builder, or {@code null}
     */
    default Hasher.Builder lookup(String scheme) {
        try {
            String lookupKey = Hasher.Builder.class.getCanonicalName() + "/" + family() + "/" + scheme;
            Class<Hasher.Builder> clazz = DiscoveryService.lookup(lookupKey);
            return clazz.newInstance();
        } catch (Exception ex) {
            LOGGER.fine("No crypt format found for " + scheme + " for " + family());
            return null;
        }
    }

}