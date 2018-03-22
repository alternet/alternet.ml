package ml.alternet.parser.visit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.visit.TraversableRule.CombinedRule;
import ml.alternet.parser.visit.TraversableRule.SimpleRule;

/**
 * Traverse composed rules and apply a transformation
 * on nested rules.
 *
 * @author Philippe Poulard
 */
public abstract class Transform implements Visitor, Function<Rule, Rule> {

    /**
     * Contains and store the rules while traversing in order
     * to prevent infinite loop.
     */
    public Set<Rule> traversed;

    /**
     * Create a transform visitor.
     */
    public Transform() {
        this(new HashSet<>());
    }

    /**
     * Create a transform visitor.
     *
     * @param traversed Contains and store the rules
     *  while traversing in order to prevent infinite loop.
     */
    public Transform(Set<Rule> traversed) {
        this.traversed = traversed;
    }

    @Override
    public void visit(CombinedRule combinedRule) {
        if (traversed.add((Rule) combinedRule)) {
            List<Rule> rules = combinedRule.getComponents().map(this::apply)
                .collect(Collectors.toList());
            combinedRule.setComponent(rules);
            rules.stream().forEach(r -> r.accept(this));
        }
    }

    @Override
    public void visit(SimpleRule simpleRule) {
        if (traversed.add((Rule) simpleRule)) {
            Rule rule = apply(simpleRule.getComponent());
            simpleRule.setComponent(rule);
            rule.accept(this);
        }
    }

    /**
     * Apply a transformation on a nested rule.
     *
     * @param rule The rule to process.
     *
     * @return The result of the transformation,
     *      that will replace the nested rule.
     */
    @Override
    public abstract Rule apply(Rule rule);

}
