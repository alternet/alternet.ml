package ml.alternet.parser.util;

import java.util.Optional;
import java.util.stream.Stream;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.Grammar.Rule;

/**
 * Used for a rule that wraps another rule or that is a
 * combination of several rules.
 *
 * There are 2 optimization axis :
 * <ul>
 *  <li>Flattenization (horizontal optimization) : for example, a sequence made of sequence
 *  will be flatten to a single sequence</li>
 *  <li>Simplification (vertical optimization): for example, a repeatable rule of a repeatable
 *  rule may be simplify to a single merged repeatable rule that combine the boundaries</li>
 * </ul>
 * Those optimizations can occur on rules that are compatible : they must be of the same type,
 * must have the same modifiers ({@literal @}Fragment, {@literal @}Skip), and must not be
 * subject to substitution ({@link Rule#isGrammarField()})
 *
 * @author Philippe Poulard
 *
 * @param <T> The type of the composition : Rule or List&lt;Rule&gt;
 */
public interface ComposedRule<T> {

    /**
     * The rule(s).
     *
     * @return The component.
     */
    T getComponent();

    /**
     * Set the combined rules within.
     *
     * @param component The actual rule(s).
     */
    void setComponent(T component);

    /**
     * Get a stream of composed rules.
     *
     * @return The stream of composed rules.
     */
    Stream<Rule> getComponents();

    /**
     * Recursively flatten **and simplify** when possible the
     * rules within, and flatten with this rule.
     */
    void flatten();

    /**
     * Return a new rule if this rule can be simplified.
     *
     * @return The new rule that must replace this one,
     *      by default none.
     */
    default Optional<Rule> simplify() {
        return Optional.empty();
    }

}
