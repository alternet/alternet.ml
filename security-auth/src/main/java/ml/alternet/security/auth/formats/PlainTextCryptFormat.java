package ml.alternet.security.auth.formats;

import java.util.Optional;

import javax.inject.Singleton;

import ml.alternet.security.auth.CryptFormat;
import ml.alternet.security.auth.Hasher;
import ml.alternet.security.auth.formatters.PlainTextCryptFormatter;
import ml.alternet.security.auth.hasher.PlainTextHasher;

/**
 * A fallback format for passwords stored in plain text ;
 * DO NOT USE IN PRODUCTION.
 *
 * Can be use as the last crypt format to resolve.
 *
 * @author Philippe Poulard
 */
@Singleton
public class PlainTextCryptFormat implements CryptFormat {

    /**
     * Get a configured plain text hasher.
     *
     * @return The new instance.
     */
    public static Hasher get() {
        return Hasher.Builder.builder()
            .setClass(PlainTextHasher.class)
            .setScheme("[CLEAR PASSWORD]")
            .setFormatter(new PlainTextCryptFormatter())
            .build();
    }

    @Override
    public Optional<Hasher> resolve(String crypt) {
        return Optional.of(get());
    }

    /**
     * @return ""
     */
    @Override
    public String family() {
        return "PLAIN_TEXT";
    }

    /**
     * @return "[CLEAR PASSWORD]"
     */
    @Override
    public String infoTemplate() {
        return "[CLEAR PASSWORD]";
    }

}
