package ml.alternet.security.auth.formatters;

import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat.SchemePart;

/**
 * A wrapper around another formatter, for handling composed
 * crypts such as <tt>{CRYPT}$1$gwvn5BO0$3dyk8j.UTcsNUPrLMsU6/0</tt>
 *
 * @author Philippe Poulard
 *
 * @param <T> The crypt parts type
 */
public class CurlyBracesCryptFormatterWrapper<T extends CryptParts>
    implements ml.alternet.security.auth.CryptFormatter<T>
{

    ml.alternet.security.auth.CryptFormatter<T> cf;
    String scheme;

    /**
     * Create a crypt formatter wrapper
     *
     * @param cf The wrapped crypt formatter
     * @param scheme The scheme to display
     */
    public CurlyBracesCryptFormatterWrapper(ml.alternet.security.auth.CryptFormatter<T> cf, String scheme) {
        this.cf = cf;
        this.scheme = scheme;
    }

    @Override
    public T parse(String crypt, Hasher hr) {
        SchemePart sp = new SchemePart(crypt);
        String mcfPart = crypt.substring( sp.rcb + 1);
        return cf.parse(mcfPart, hr);
    }

    @Override
    public String format(T parts) {
        return "{" + this.scheme + "}" + cf.format(parts);
    }

    @Override
    public CryptFormat getCryptFormat() {
        return new CurlyBracesCryptFormat();
    }

}