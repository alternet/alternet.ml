package ml.alternet.parser.util;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

import ml.alternet.parser.Grammar.Optional;
import ml.alternet.parser.Grammar.Choice;
import ml.alternet.parser.Grammar.Rule;

/**
 * This interface is used by Rules in order
 * to traverse them.
 *
 * @author Philippe Poulard
 *
 * @see Rule
 */
public interface TraversableRule {

    /**
     * Traverse this rule and apply a transformer on each subrules,
     * that is to say recursively apply the function to the rules
     * that are composing this rule, otherwise do nothing ; if the
     * function change a rule, it must be replaced by the new one.
     * The changes can be applied to this rule or on a clone according
     * to a supplier.
     *
     * <h1>Implementation details</h1>
     *
     * A concrete rule has to implement this method, to allow
     * visiting its internals. The idea is to traverse a rule once,
     * by applying a specific task (performed by a <code>transformer</code>)
     * on its components rules, and then to recursively traverse
     * the result. If necessary, the transformation may be applied
     * on itself or a clone, according to the <code>targetRule</code>
     * supplier.
     * Sometimes rules are composed of just another rule,
     * such as {@link Optional}, or by several rules,
     * such as {@link Choice} ; you can examine the code of
     * the relevant classes, respectively {@link SimpleRule} and
     * {@link CombinedRule} that traverse a single nested rule or
     * a list of nested rules.
     *
     * @param hostRule The host rule in the hierarchy that is declared as a
     *      field in a grammar, that is to say that has a name.
     * @param traversed Each time a rule is traversed, it must be stored
     *      in this set to avoid traversing it again during recursion.
     * @param transformer <code>(hostRule, subRule) -&gt; transformedRule</code>
     *      The function that transform a subRule, must return that argument
     *      to keep it unchanged. The hostRule is the nearest enclosing rule
     *      that has a name (that is to say which is a grammar field).
     * @param targetRule <code>(thisRule) -&gt; sameRuleOrClone</code>
     *      Supply the rule on which the changes have to be
     *      applied : typically the rule itself or a clone of it. The
     *      argument of this function is this rule.
     * @return The rule on which the changes were performed, given by the
     *      <code>targetRule</code> (this rule or a clone of this rule),
     *      or this rule if no changed were performed. The resulting rule
     *      must be of the same type of the original.
     */
    Rule traverse(Rule hostRule, Set<Rule> traversed, BiFunction<Rule, Rule, Rule> transformer,
            Function<Rule, Rule> targetRule);

    /**
     * Indicates whether this rule is defined as a field
     * in a grammar.
     *
     * @return <code>true</code> if this rule has a name
     *      <code>false</code> otherwise.
     */
    boolean isGrammarField();

    /**
     * Just return this rule when traversing.
     *
     * This interface can be used only by subclasses of Rule.
     *
     * @author Philippe Poulard
     */
    interface SelfRule extends TraversableRule {

        @Override
        default Rule traverse(Rule hostRule, Set<Rule> traversed,
                BiFunction<Rule, Rule, Rule> transformer, Function<Rule, Rule> targetRule)
        {
            return (Rule) this;
        }

    }

    /**
     * Algorithm for traversing a single rule : can be used
     * for an out-of-the-box rule as well as for a wrapped rule.
     *
     * This interface can be used only by subclasses of Rule.
     *
     * It supply a default implementation of the
     * {@code traverse(Rule, Set, BiFunction, Function)}
     * method for such wrappers.
     *
     * @author Philippe Poulard
     */
    interface SimpleRule extends TraversableRule, ComposedRule<Rule> {

        @Override
        default Rule traverse(Rule hostRule, Set<Rule> traversed, BiFunction<Rule, Rule, Rule> transformer,
                Function<Rule, Rule> targetRule)
        {
            SimpleRule target = this;
            if (traversed.add((Rule) this)) {
                // is it a named rule, that is to say a field declared in a Grammar ?
                hostRule = isGrammarField() ? (Rule) this : hostRule; // yes: get it; no: hostRule is unchanged
                Rule thisRule = getComponent();
                Rule newRule = transformer.apply(hostRule, thisRule)
                    .traverse(hostRule, traversed, transformer, targetRule);
                if (thisRule != newRule) { // dirty
                    target = (SimpleRule) targetRule.apply((Rule) this);
                    target.setComponent(newRule);
                }
            } // else already traversed, then ignored
            return (Rule) target;
        }

        @Override
        default Stream<Rule> getComponents() {
            return Stream.of(getComponent());
        }

        @Override
        default void flatten() {
            if (getComponent() instanceof ComposedRule &&
                ! getComponent().isGrammarField())
            {
                ((ComposedRule<?>) getComponent()).flatten();
            }
        }
    }

    /**
     * Algorithm for traversing a combined rule, that is to say
     * a rule made of several rules.
     *
     * This interface can be used only by subclasses of Rule.
     *
     * It supply a default implementation of the
     * {@code traverse(Rule, Set, BiFunction, Function)}
     * method.
     *
     * @author Philippe Poulard
     */
    interface CombinedRule extends TraversableRule, ComposedRule<List<Rule>> {

        @Override
        default Rule traverse(Rule hostRule, Set<Rule> traversed,
                BiFunction<Rule, Rule, Rule> transformer, Function<Rule, Rule> targetRule)
        {
            CombinedRule target = this;
            if (traversed.add((Rule) this)) {
                // flag set when a change was made
                boolean[] dirty = new boolean[] { false };
                // is it a named rule, that is to say a field declared in a Grammar ?
                Rule host = isGrammarField() ? (Rule) this : hostRule; // yes: get it; no: hostRule is unchanged
                List<Rule> newRules = getComponent()
                    .stream()
                    .map(r -> {
                        Rule newRule = transformer.apply(host, r);
                        if (r == newRule) {
                            newRule = newRule.traverse(host, traversed, transformer, targetRule);
                        }
                        if (r != newRule) {
                            dirty[0] = true;
                        }
                        return newRule;
                    })
                    .collect(toList());
                if (dirty[0]) {
                    target = (CombinedRule) targetRule.apply((Rule) this);
                    target.setComponent(newRules);
                }
            } // else already traversed, then ignored
            return (Rule) target;
        }

        @Override
        default Stream<Rule> getComponents() {
            return getComponent().stream();
        }

    }

}