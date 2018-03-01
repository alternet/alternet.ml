package ml.alternet.security.auth.formats;

import java.util.Optional;
import java.util.function.Predicate;

import javax.inject.Singleton;

import ml.alternet.scan.Scanner;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.WorkFactorSaltedParts;
import ml.alternet.security.auth.formatters.ColonCryptFormatter;

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
 * @see WorkFactorSaltedParts
 *
 * @author Philippe Poulard
 */
@Singleton
public class ColonCryptFormat extends CurlyBracesCryptFormat implements CryptFormat {

    @Override
    protected char shemeEndChar() {
        return ':';
    }

    @Override
    protected Predicate<Scanner> schemeStartCondition() {
        return c -> true; // this scheme doesn't start by a specific char
    }

    @Override
    public Optional<Hasher> resolve(String crypt) {
        return super.resolve(crypt)
            .map(hr -> hr.getBuilder().setFormatter(new ColonCryptFormatter()).build());
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
    };

}
