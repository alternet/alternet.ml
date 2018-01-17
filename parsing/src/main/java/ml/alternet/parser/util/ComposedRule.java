package ml.alternet.parser.util;

import java.util.stream.Stream;

import ml.alternet.parser.Grammar;

/**
 * Used for a rule that wraps another rule or that is a
 * combination of several rules.
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

    Stream<Grammar.Rule> getComposedRules();

}