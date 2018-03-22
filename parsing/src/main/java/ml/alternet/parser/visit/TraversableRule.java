package ml.alternet.parser.visit;

import java.util.List;
import java.util.stream.Stream;

import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.util.ComposedRule;

/**
 * This interface is used by Rules in order
 * to traverse them. It exists in 3 flavour :
 * standalone rules, simple rules that host a
 * single nested rule, and combined rules that
 * host several nested rules.
 *
 * @author Philippe Poulard
 *
 * @see Rule
 */
public interface TraversableRule {

    /**
     * Accept the given visitor. The visitor hold the state
     * during traversing and is responsible of avoiding infinite
     * loop during recursion.
     *
     * @param visitor Allow to visit different kinds of rules.
     */
    void accept(Visitor visitor);

    /**
     * Indicates whether this rule is defined as a field
     * in a grammar.
     *
     * @return <code>true</code> if this rule has a name
     *      <code>false</code> otherwise.
     */
    boolean isGrammarField();

    /**
     * A standalone rule doesn't embed nested rules.
     *
     * This interface can be used only by subclasses of Rule.
     *
     * @author Philippe Poulard
     */
    interface StandaloneRule extends TraversableRule {

        @Override
        default void accept(Visitor visitor) {
            visitor.visit(this);
        }

    }

    /**
     * Allow to traverse the nested rule embedded in this rule.
     *
     * This interface can be used only by subclasses of Rule.
     *
     * @author Philippe Poulard
     */
    interface SimpleRule extends TraversableRule, ComposedRule<Rule> {

        @Override
        default void accept(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        default Stream<Rule> getComponents() {
            return Stream.of(getComponent());
        }

        @Override
        default void flatten() {
            if (getComponent() instanceof ComposedRule
                    && ! getComponent().isGrammarField())
            {
                ((ComposedRule<?>) getComponent()).flatten();
            }
        }
    }

    /**
     * Allow to traverse a combined rule, that is to say
     * a rule made of several rules.
     *
     * This interface can be used only by subclasses of Rule.
     *
     * @author Philippe Poulard
     */
    interface CombinedRule extends TraversableRule, ComposedRule<List<Rule>> {

        @Override
        default void accept(Visitor visitor) {
            visitor.visit(this);
        }

        @Override
        default Stream<Rule> getComponents() {
            return getComponent().stream();
        }

    }

}
