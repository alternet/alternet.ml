package ml.alternet.security.auth.formats;

import java.util.Optional;

import javax.inject.Singleton;

import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.hashers.UnixHashers;

/**
 * Legacy Unix Crypt Formats :
 *
 * <ul>
 * <li>A des-crypt hash string consists of 13 characters, drawn from [./0-9A-Za-z] :<br>
 * <tt>password</tt> -&gt; <tt>JQMuyS6H.AGMo</tt>
 * </li>
 * <li>A fallback to MD5 32 hexa character hash :<br>
 * <tt>11fbe079ed3476f7712030d24042ca35</tt>
 * </li>
 * </ul>
 *
 * @author Philippe Poulard
 */
@Singleton
public class UnixCryptFormat implements CryptFormat {

    @Override
    public Optional<Hasher.Builder> resolve(String crypt) {
        if (crypt.length() == 13) {
            // unix crypt
            return Optional.of(UnixHashers.UNIX_CRYPT.get());
        } else if (crypt.length() == 32) {
            // traditional MD5 : 32 hexa characters
            return Optional.of(UnixHashers.MD5.get());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String family() {
        return "UNIX_CRYPT";
    }

    @Override
    public String infoTemplate() {
        return "sshhhhhhhhhhh [or] xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    }

}
