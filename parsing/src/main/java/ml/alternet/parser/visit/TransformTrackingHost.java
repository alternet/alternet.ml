package ml.alternet.parser.visit;

import java.util.List;
import java.util.stream.Collectors;

import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.visit.TraversableRule.CombinedRule;
import ml.alternet.parser.visit.TraversableRule.SimpleRule;

/**
 * Traverse composed rules and apply a transformation
 * on nested rules ; track the closest enclosed host
 * rule while traversing.
 *
 * @author Philippe Poulard
 */
public abstract class TransformTrackingHost extends Transform implements Visitor {

    // track the host rule while traversing
    Rule hostRule;

    /**
     * Create a visitor that recursively traverse the rules.
     *
     * @param hostRule The start host rule.
     *
     * @see TraversableRule#isGrammarField()
     */
    public TransformTrackingHost(Rule hostRule) {
        this.hostRule = hostRule;
    }

    /**
     * Get the host rule of the current rule, that is to say
     * the closest enclosed rule which is a grammar field.
     *
     * @return The closest enclosed rule or the current rule
     *      if it is a grammar field.
     *
     * @see TraversableRule#isGrammarField()
     */
    public Rule getHostRule() {
        return this.hostRule;
    }

    @Override
    public void visit(CombinedRule combinedRule) {
        if (traversed.add((Rule) combinedRule)) {
            Rule prevHost = hostRule;
            // is it a named rule, that is to say a field declared in a Grammar ?
            if (combinedRule.isGrammarField()) {
                // yes: get it; no: hostRule is unchanged
                hostRule = (Rule) combinedRule;
            }
            List<Rule> rules = combinedRule.getComponents().map(this::apply)
                .collect(Collectors.toList());
            combinedRule.setComponent(rules);
            rules.stream().forEach(r -> r.accept(this));
            hostRule = prevHost;
        }
    }

    @Override
    public void visit(SimpleRule simpleRule) {
        if (traversed.add((Rule) simpleRule)) {
            Rule prevHost = hostRule;
            // is it a named rule, that is to say a field declared in a Grammar ?
            if (simpleRule.isGrammarField()) {
                // yes: get it; no: hostRule is unchanged
                hostRule = (Rule) simpleRule;
            }
            Rule rule = apply(simpleRule.getComponent());
            simpleRule.setComponent(rule);
            rule.accept(this);
            hostRule = prevHost;
        }
    }

}
