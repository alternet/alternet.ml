package ml.alternet.parser.util;

import ml.alternet.parser.Handler;
import ml.alternet.parser.Grammar.Match;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.scan.Scanner;

/**
 * Embeds the parsing logic on behalf of a kind of rule.
 *
 * @author Philippe Poulard
 *
 * @param <T> The kind of rule.
 */
public interface Parser<T extends Rule>  {

    /**
     * Parse an input with a rule.
     *
     * @param rule The rule.
     * @param scanner The input.
     * @param handler The receiver.
     *
     * @return Indicates whether the rule matched or not.
     */
    Match parse(T rule, Scanner scanner, Handler handler);

}
