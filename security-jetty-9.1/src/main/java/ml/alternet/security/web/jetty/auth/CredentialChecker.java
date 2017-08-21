package ml.alternet.security.web.jetty.auth;

import java.security.InvalidAlgorithmParameterException;
import java.util.List;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import ml.alternet.security.auth.Credentials;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;

public interface CredentialChecker {

    static Logger LOG = Log.getLogger(CredentialChecker.class);

    default boolean check(String crypt, List<CryptFormat> formats, Credentials credentials) {
        return Hasher.resolve(crypt, formats)
                .map(hr -> {
                    try {
                        return hr.check(credentials, crypt);
                    } catch (InvalidAlgorithmParameterException e) {
                        LOG.warn("Unable to load hasher for crypt " + crypt, e);
                        return false;
                    }
                })
                .orElseGet(() -> {
                    LOG.warn("Unable to find a hasher for crypt " + crypt);
                    return false;
                });
    }

}
