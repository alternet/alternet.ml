package ml.alternet.parser.visit;

import java.util.logging.Logger;

import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.visit.TraversableRule.CombinedRule;
import ml.alternet.parser.visit.TraversableRule.SimpleRule;
import ml.alternet.parser.visit.TraversableRule.StandaloneRule;

/**
 * Log visited rules.
 *
 * @author Philippe Poulard
 */
public class LogVisitor implements Visitor {

    Logger log;
    Visitor visitor;

    /**
     * Wrap a visitor for logging.
     *
     * @param visitor The visitor to log.
     */
    public LogVisitor(Visitor visitor) {
        this.log = Logger.getLogger(visitor.getClass().getName());
        this.visitor = visitor;
    }

    /**
     * Wrap a visitor for logging.
     *
     * @param visitor The visitor to log.
     * @param logger The log receiver.
     */
    public LogVisitor(Visitor visitor, Logger logger) {
        this.log = logger;
        this.visitor = visitor;
    }

    void log(String msg, Rule rule) {
        this.log.info(() -> "Visiting " + msg + ' ' + Dump.getHash(rule) + rule.getName() + " -> " + rule.toPrettyString());
    }

    @Override
    public void visit(StandaloneRule selfRule) {
        log("standalone rule", (Rule) selfRule);
        this.visitor.visit(selfRule);
    }

    @Override
    public void visit(SimpleRule simpleRule) {
        log("simple rule", (Rule) simpleRule);
        this.visitor.visit(simpleRule);
    }

    @Override
    public void visit(CombinedRule combinedRule) {
        log("combined rule", (Rule) combinedRule);
        this.visitor.visit(combinedRule);
    }

}
