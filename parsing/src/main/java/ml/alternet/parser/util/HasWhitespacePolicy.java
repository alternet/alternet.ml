package ml.alternet.parser.util;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

import ml.alternet.parser.Grammar.WhitespacePolicy;
import ml.alternet.scan.Scanner;

/**
 * A token (or a rule) may apply a whitespace policy around its input.
 *
 * @see WhitespacePolicy
 *
 * @author Philippe Poulard
 */
public interface HasWhitespacePolicy {

    /**
     * Set the whitespace policy for this token.
     *
     * @param whitespacePolicy Indicates whether a character is
     *      a whitespace or not.
     */
    void setWhitespacePolicy(Optional<Predicate<Integer>> whitespacePolicy);

    /**
     * Return the function that filters out whitespaces
     *
     * @return The function that filters out whitespaces from
     *      a Unicode codepoint.
     */
    Optional<Predicate<Integer>> getWhitespacePolicy();

    /**
     * Apply the whitespace policy before the token,
     * eventually, mark the scanner position.
     *
     * @param scanner Hold the input
     * @return <code>true</code> if some whitespaces were trimmed,
     *      <code>false</code> otherwise.
     *
     * @throws IOException When an I/O exception occur.
     */
    default boolean applyWhitespacePolicyBefore(Scanner scanner) throws IOException {
        if (getWhitespacePolicy().isPresent()) {
            boolean marked = false;
            for (Predicate<Integer> wsp = getWhitespacePolicy().get();
                    wsp.test(scanner.lookAhead());
                    scanner.nextChar())
            {
                if (! marked) {
                    marked = true;
                    scanner.mark();
                }
            }
            return marked;
        } else {
            return false;
        }
    }

    /**
     * Apply the whitespace policy after the token.
     * This method is called when the token matches
     * the input.
     *
     * @param scanner Hold the input
     *
     * @throws IOException When an I/O exception occur.
     */
    default void applyWhitespacePolicyAfter(Scanner scanner) throws IOException {
        if (getWhitespacePolicy().isPresent()) {
            for (Predicate<Integer> wsp = getWhitespacePolicy().get();
                    wsp.test(scanner.lookAhead()) ;
                    scanner.nextChar())
            { }
        }
    }

}
