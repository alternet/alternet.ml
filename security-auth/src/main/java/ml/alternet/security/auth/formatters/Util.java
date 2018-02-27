package ml.alternet.security.auth.formatters;

import java.util.NoSuchElementException;
import java.util.function.IntPredicate;

import ml.alternet.scan.Scanner;
import ml.alternet.security.auth.Hasher;
import ml.alternet.util.BytesUtil;

/**
 * Utilities for formatters.
 *
 * @author Philippe Poulard
 */
@ml.alternet.util.Util
public class Util {

    /**
     * Collect the decoded characters that satisfy a predicate to a byte array.
     *
     * @param hr Hold the decoder.
     * @param scanner The character stream
     * @param predicate Indicates the character to accept.
     * @return The byte array of characters.
     */
    public static byte[] decode(Hasher hr, Scanner scanner, IntPredicate predicate) {
        return hr.getConfiguration().getEncoding()
            .decode(
                scanner.nextChars(predicate)
                    .flatMap(c -> new String(Character.toChars(c)).codePoints())
                    .mapToObj(c -> (Character) (char) c))
            .mapToObj(i -> (Integer) i)
            .collect(BytesUtil.toByteArray());
    }

    /**
     * Ensure that a result is {@code true}, otherwise throw a {@code NoSuchElementException}
     *
     * @param whatever The result to check.
     * @throws NoSuchElementException
     */
    public static void ensure(boolean whatever) {
        if (! whatever) {
            throw new NoSuchElementException();
        }
    }

}
