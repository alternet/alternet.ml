package ml.alternet.parser.visit;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.visit.TraversableRule.CombinedRule;
import ml.alternet.parser.visit.TraversableRule.SimpleRule;

/**
 * Traverse composed rules.
 *
 * @author Philippe Poulard
 */
public abstract class Traverse implements Visitor, Consumer<Rule> {

    // prevent infinite loop
    Set<Rule> traversed = new HashSet<>();
    boolean depth = false;

    /**
     * Configure this visitor :
     * by default, all nested rules are processed, and then
     * all nested rules are traversed ; when this method is
     * called the behaviour becomes slightly different : each
     * nested rule is processed then traversed, and so on.
     *
     * @return {@code this}, for chaining.
     */
    public Traverse inDepthBefore() {
        this.depth = true;
        return this;
    }

    @Override
    public void visit(CombinedRule combinedRule) {
        if (traversed.add((Rule) combinedRule)) {
            if (this.depth) {
                combinedRule.getComponents()
                    .peek(this::accept)
                    .forEach(r -> r.accept(this));
            } else {
                combinedRule.getComponents().forEach(this::accept);
                combinedRule.getComponents().forEach(r -> r.accept(this));
            }
        }
    }

    @Override
    public void visit(SimpleRule simpleRule) {
        if (traversed.add((Rule) simpleRule)) {
            accept(simpleRule.getComponent());
            simpleRule.getComponent().accept(this);
        }
    }

    /**
     * Process a nested rule.
     *
     * @param rule The rule to process.
     */
    @Override
    public abstract void accept(Rule rule);

}
