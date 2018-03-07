package ml.alternet.security.auth.formats;

import static ml.alternet.misc.Thrower.safeCall;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import ml.alternet.encode.BytesEncoder;
import ml.alternet.encode.BytesEncoding;
import ml.alternet.scan.Scanner;
import ml.alternet.scan.StringConstraint;

/**
 * A scheme with encoding, e.g. {@code SSHA.b64} or {@code SSHA.HEX}.
 *
 * Works with a scanner in order to let progress the parsing after creating an instance.
 *
 * @author Philippe Poulard
 *
 * @see BytesEncoder#hexa
 * @see BytesEncoder#base64
 */
public class SchemeWithEncoding {

    /** The scheme, if any. */
    public Optional<String> scheme = Optional.empty();
    /** The encoding, if any. */
    public Supplier<Optional<BytesEncoding>> encoding = () -> Optional.empty();

    /**
     * Extract the scheme and encoding if any.
     *
     * @param crypt The crypt to parse, wrapped in a scanner.
     * @param schemeStartCondition Test and consume the characters before the scheme, if necessary.
     * @param shemeEndChar The character that ends the scheme (not consumed by the scanner).
     * @throws IOException On I/O error.
     */
    public SchemeWithEncoding(Scanner crypt, Predicate<Scanner> schemeStartCondition, char shemeEndChar) throws IOException {
        if (schemeStartCondition.test(crypt)) {
            this.scheme = crypt.nextString(new StringConstraint.ReadUntilChar("." + shemeEndChar));
            if (crypt.hasNextChar('.', true)) {
                String encoding = crypt.nextString(new StringConstraint.ReadUntilSingleChar(shemeEndChar)).get();
                this.encoding = () -> safeCall(() -> {
                    if ("HEX".equalsIgnoreCase(encoding)) {
                        return Optional.of(BytesEncoder.hexa);
                    } else if ("b64".equalsIgnoreCase(encoding)) {
                        return Optional.of(BytesEncoder.base64);
                    } else {
                        CurlyBracesCryptFormat.LOGGER.warning("Unknown scheme variant \"" + encoding + "\" in " + "\"" + crypt + "\"");
                        return Optional.empty();
                    }
                });
            }
        }
    }

    /**
     * Return the code for the given encoding.
     *
     * @param encoding The encoding.
     * @return "HEX" or "b64" or empty
     */
    public static Optional<String> code(BytesEncoding encoding) {
        String name = encoding.name();
        if ("HEXA".equalsIgnoreCase(name)) {
            return Optional.of("HEX");
        } else if ("base64".equalsIgnoreCase(name)) {
            return Optional.of("b64");
        } else {
            return Optional.empty();
        }
    }

}