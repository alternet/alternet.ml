package ml.alternet.parser.visit;

import ml.alternet.parser.visit.TraversableRule.CombinedRule;
import ml.alternet.parser.visit.TraversableRule.StandaloneRule;
import ml.alternet.parser.visit.TraversableRule.SimpleRule;

/**
 * The rules visitor.
 *
 * The visitor hold its state during traversing and is responsible
 * of avoiding infinite loop during recursion.
 *
 * @author Philippe Poulard
 */
public interface Visitor {

    /**
     * Visit a tail rule (typically, a token atom).
     *
     * By default, do nothing.
     *
     * @param selfRule A rule that is not a composed rule.
     */
    default void visit(StandaloneRule selfRule) { }

    /**
     * Visit a rule composed of a single nested rule.
     *
     * @param simpleRule The rule to visit.
     */
    void visit(SimpleRule simpleRule);

    /**
     * Visit a rule composed of several nested rules.
     *
     * @param combinedRule The rule to visit.
     */
    void visit(CombinedRule combinedRule);

}
