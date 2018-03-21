package ml.alternet.parser.visit;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.visit.TraversableRule.CombinedRule;
import ml.alternet.parser.visit.TraversableRule.StandaloneRule;
import ml.alternet.parser.visit.TraversableRule.SimpleRule;

/**
 * Dump a rule.
 *
 * @author Philippe Poulard
 */
public class Dump extends Traverse implements Visitor, Supplier<StringBuffer> {

    /**
     * Convenient method for dumping a rule with details
     * (rules class names and hash codes).
     *
     * @param rule The rule to dump.
     *
     * @return The tree-string representation of the internals
     *      of the rule.
     */
    public static String detailed(Rule rule) {
        Dump dumper = new Dump();
        rule.accept(dumper);
        return dumper.toString();
    }

    /**
     * Convenient method for dumping a grammar with details
     * (rules class names and hash codes).
     *
     * @param grammar The grammar to dump.
     *
     * @return The tree-string representation of the internals
     *      of the main rule if it exists, or "".
     */
    public static String detailed(Grammar grammar) {
        return grammar.mainRule()
                .map(Dump::detailed)
                .orElse("");
    }

    /**
     * Convenient method for dumping a rule with details
     * (rules class names and hash codes).
     *
     * This method use the grammar owner, typically when
     * a grammar extends another one, its rule can be
     * considered as the original rule (which is the case
     * without the grammar argument) or as an imported
     * rule in the extended grammar.
     *
     * @param grammar The owner grammar.
     * @param rule The rule to dump.
     *
     * @return The tree-string representation of the internals
     *      of the rule.
     */
    public static String detailed(Grammar grammar, Rule rule) {
        Dump dumper = new Dump();
        rule = grammar.adopt(rule);
        rule.accept(dumper);
        return dumper.toString();
    }

    /**
     * Convenient method for dumping a rule.
     *
     * This method use the grammar owner, typically when
     * a grammar extends another one, its rule can be
     * considered as the original rule (which is the case
     * without the grammar argument) or as an imported
     * rule in the extended grammar.
     *
     * @param grammar The grammar that owns the rule to dump.
     * @param rule The rule to dump.
     *
     * @return The tree-string representation of the internals
     *      of the rule.
     */
    public static String tree(Grammar grammar, Rule rule) {
        Dump dumper = new Dump().withoutClass().withoutHash();
        rule = grammar.adopt(rule);
        rule.accept(dumper);
        return dumper.toString();
    }

    /**
     * Convenient method for dumping a grammar.
     *
     * @param grammar The grammar to dump.
     *
     * @return The tree-string representation of the internals
     *      of the main rule if it exists, or "".
     */
    public static String tree(Grammar grammar) {
        return grammar.mainRule()
                .map(Dump::tree)
                .orElse("");
    }

    /**
     * Convenient method for dumping a rule.
     *
     * @param rule The rule to dump.
     *
     * @return The tree-string representation of the internals
     *      of the rule.
     */
    public static String tree(Rule rule) {
        Dump dumper = new Dump().withoutClass().withoutHash();
        rule.accept(dumper);
        return dumper.toString();
    }

    boolean withHash = true;
    boolean withClass = true;

    StringBuffer buf = new StringBuffer();

    // state
    String prefix = "";
    boolean isTail = true;
    boolean isTop = true; // allow to process the top element
    int size = 0; // state for list of rules
    int index = 0; // state for list of rules

    /**
     * Create a dump visitor.
     */
    public Dump() {
        this.depth = true;
    }

    /**
     * Return tree-string representation of the internals
     *      of the rule.
     *
     * @return The dump of a rule.
     */
    @Override
    public StringBuffer get() {
        return this.buf;
    }

    /**
     * Return tree-string representation of the internals
     *      of the rule.
     *
     * @return The dump of a rule.
     */
    @Override
    public String toString() {
        return this.buf.toString();
    }

    /**
     * Configure this dump :
     * by default, a hash is displayed in the dump ;
     * calling this method removes it.
     *
     * @return {@code this}, for chaining
     */
    public Dump withoutHash() {
        this.withHash = false;
        return this;
    }

    /**
     * Configure this dump :
     * by default, a class name is displayed in the dump ;
     * calling this method removes it.
     *
     * @return {@code this}, for chaining
     */
    public Dump withoutClass() {
        this.withClass = false;
        return this;
    }

    /**
     * Configure this dump by setting a set of rules
     * considered to be already visited.
     *
     * @param visited The already visited rules.
     *
     * @return {@code this}, for chaining
     */
    public Dump setVisited(Set<Rule> visited) {
        this.traversed.addAll(visited);
        return this;
    }

    /**
     * Configure this dump by setting a rule
     * considered to be already visited.
     *
     * @param visited The already visited rule.
     *
     * @return {@code this}, for chaining
     */
    public Dump setVisited(Rule visited) {
        this.traversed.add(visited);
        return this;
    }

    @Override
    public void visit(StandaloneRule selfRule) {
        dumpTop((Rule) selfRule);
        if (selfRule.isGrammarField()) {
            buf.append(" ━━━ ")
                .append(((Rule) selfRule).toPrettyString().toString());
        }
        buf.append('\n');
    }

    @Override
    public void visit(SimpleRule simpleRule) {
        boolean wasTop = dumpTop((Rule) simpleRule);
        buf.append('\n');
        // save state
        int prevSize = size;
        String prevPrefix = prefix;
        boolean prevTail = isTail;
        // set current state
        size = 1;
        prefix += wasTop ? "" : (isTail ? "    " : "┃   ");
        super.visit(simpleRule);
        // restore state
        prefix = prevPrefix;
        isTail = prevTail;
        size = prevSize;
    }

    @Override
    public void visit(CombinedRule combinedRule) {
        boolean wasTop = dumpTop((Rule) combinedRule);
        List<Rule> rules = combinedRule.getComponent();
        if (! rules.isEmpty()) {
            buf.append('\n');
        }
        // save state
        int prevSize = size;
        int prevIndex = index;
        String prevPrefix = prefix;
        boolean prevTail = isTail;
        // set current state
        size = rules.size();
        index = 0;
        prefix += wasTop ? "" : (isTail ? "    " : "┃   ");
        super.visit(combinedRule);
        // restore state
        prefix = prevPrefix;
        isTail = prevTail;
        index = prevIndex;
        size = prevSize;
    }

    boolean dumpTop(Rule rule) {
        if (isTop) {
            accept(rule);
            isTop = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void accept(Rule rule) {
        index++;
        isTail = index >= size;

        if (this.withHash) {
            appendHash(buf, rule).append(' ');
        }
        if (this.withClass) {
            String cl = rule.getClass().getName();
            if (cl.contains("Grammar$")) {
                cl = cl.substring(cl.indexOf("Grammar$") + 8);
            } else {
                cl = rule.getClass().getSimpleName();
            }
            int size = 18;
            if (cl.length() > size) {
                cl = cl.substring(0, size);
            }
            if (cl.length() < size) {
                cl = cl + new String(new char[size - cl.length()]).replace("\0", " ");
            }
            buf.append(cl).append(" ┊ ");
        }
        buf.append(prefix)
            .append(isTop ? "" : isTail ? "┗━━ " : "┣━━ ")
            .append(rule.isGrammarField() ? rule.getName() : rule.toPrettyString().toString());
    }

    static StringBuffer appendHash(StringBuffer buf, Rule rule) {
        buf.append('<');
        String id = Integer.toHexString(Objects.hashCode(rule));
        for (int i = id.length(); i < 8 ; i++) {
            buf.append('0');
        }
        buf.append(id).append('>');
        return buf;
   }

    /**
     * Convenient method for displaying the hash of a Rule.
     *
     * @param rule The actual rule
     * @return A hash, in hexa.
     */
    public static String getHash(Rule rule) {
        return appendHash(new StringBuffer(), rule).toString();
    }

}
