package ml.alternet.security.auth;

import ml.alternet.security.auth.crypt.CryptParts;

/**
 * Bridge between the string representation of a crypt and its parts.
 *
 * @author Philippe Poulard
 *
 * @param <T> Concrete crypt parts.
 */
public interface CryptFormatter<T extends CryptParts> {

    /**
     * Breakdown a crypt to its parts. Parsers can
     * parse a partial crypt, in order for example
     * to extract the salt.
     *
     * @param crypt The crypt string to parse.
     * @param hr The hasher that can process the crypt ;
     *      also hold the configuration such as the byte
     *      encoder.
     *
     * @return The crypt parts of the given crypt.
     */
    T parse(String crypt, Hasher hr);

    /**
     * Format crypt parts to a crypt string.
     *
     * @param parts The crypt parts to format.
     *
     * @return The formatted crypt.
     */
    String format(T parts);

    /**
     * The general format of this formatter.
     *
     * @return The crypt format family.
     */
    CryptFormat getCryptFormat();

}
