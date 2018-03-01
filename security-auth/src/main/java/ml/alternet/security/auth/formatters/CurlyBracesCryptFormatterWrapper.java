package ml.alternet.security.auth.formatters;

import ml.alternet.misc.Thrower;
import ml.alternet.scan.Scanner;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.CryptFormatter;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.crypt.CryptParts;
import ml.alternet.security.auth.formats.CurlyBracesCryptFormat;

/**
 * A wrapper around another formatter, for handling composed
 * crypts such as <tt>{CRYPT}$1$gwvn5BO0$3dyk8j.UTcsNUPrLMsU6/0</tt>
 *
 * @author Philippe Poulard
 *
 * @param <T> The crypt parts type
 */
public class CurlyBracesCryptFormatterWrapper<T extends CryptParts> implements CryptFormatter<T> {

    CryptFormatter<T> cf;
    String scheme;
    CryptFormat cryptFormat;

    /**
     * Create a crypt formatter wrapper
     *
     * @param cf The wrapped crypt formatter
     * @param scheme The scheme to display
     * @param cryptFormat The delegate crypt format
     */
    public CurlyBracesCryptFormatterWrapper(CryptFormatter<T> cf, String scheme, CryptFormat cryptFormat) {
        this.cf = cf;
        this.scheme = scheme;
        this.cryptFormat = cryptFormat;
    }

    @Override
    public T parse(String crypt, Hasher hr) {
        Scanner scanner = Scanner.of(crypt);
        CurlyBracesCryptFormatter.parseScheme(scanner, hr); // it takes its place
        return Thrower.safeCall(() -> {
            String mcfPart = scanner.getRemainderString().get();
            Hasher.Builder builder = this.cryptFormat.resolve(mcfPart).get().getBuilder();
            builder.setFormatter(this);
            return cf.parse(mcfPart, builder.build());
        });
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