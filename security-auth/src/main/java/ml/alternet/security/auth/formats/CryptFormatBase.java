package ml.alternet.security.auth.formats;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import ml.alternet.misc.Thrower;
import ml.alternet.scan.EnumValues;
import ml.alternet.scan.Scanner;
import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;

/**
 * A crypt format based on enum values.
 *
 * First, the hasher builder is looked up within the built-in hashers
 * available with {@link #getEnumValues()} ; if not found, the
 * scheme is extracted with {@link #parseScheme(Scanner)} and
 * looked up with the discovery service.
 *
 * @author Philippe Poulard
 */
public abstract class CryptFormatBase implements CryptFormat, DiscoverableCryptFormat {

    /**
     * Return the enum values for this crypt format.
     *
     * @return The enum values.
     */
    protected abstract EnumValues<? extends Supplier<Hasher>> getEnumValues();

    protected abstract Optional<String> parseScheme(Scanner scanner) throws IOException;

    /**
     * Return if it exist the built-in hasher builder for the given crypt.
     *
     * @param crypt The crypt, inside a scanner.
     * @return The hasher builder for that crypt.
     *
     * @throws IOException When an I/O error occurs.
     */
    protected Optional<Hasher.Builder> getBuiltinHasherBuilder(Scanner crypt) throws IOException {
        return crypt.nextEnumValue(getEnumValues())
            .map(Supplier<Hasher>::get)
            .map(Hasher::getBuilder);
    }

    @Override
    public Optional<Hasher> resolve(String crypt) {
        try {
            Optional<Hasher.Builder> builder = Optional.empty();
            Scanner scanner = Scanner.of(crypt);
            try {
                scanner.mark();
                builder = getBuiltinHasherBuilder(scanner);
                scanner.consume();
            } catch (IllegalArgumentException e) { // scheme not within the enum
                scanner.cancel();
                builder = parseScheme(scanner)
                    .map(scheme -> lookup(scheme));
            }
            return builder.map(b -> b.use(crypt)).map(Hasher.Builder::build);
        } catch (Exception ex) {
            LOGGER.fine("Unable to parse \"" + crypt + '"');
            return Thrower.doThrow(ex);
        }
    }

}
