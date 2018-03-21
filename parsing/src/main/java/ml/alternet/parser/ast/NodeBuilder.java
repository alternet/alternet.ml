package ml.alternet.parser.ast;

import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.Grammar.Token;
import ml.alternet.parser.handlers.TreeHandler;
import ml.alternet.scan.Scanner;

/**
 * Build an homogeneous AST from a grammar, tokens mapper,
 * and rules mapper.
 *
 * <h1>Usage :</h1>
 * <pre>NodeBuilder nb = new NodeBuilder(grammar)
 *                             .setTokenMapper(tokenMapper)
 *                             .setRuleMapper(ruleMapper);</pre>
 *
 * @author Philippe Poulard
 *
 * @param <Node> The type of the target node.
 */
public class NodeBuilder<Node> extends TreeHandler<Node, Node> implements Builder<Node>, Mappers<Node> {

    Grammar grammar;
    Map<String, TokenMapper<Node>> tokenMapper = new HashMap<>();
    Map<String, RuleMapper<Node>> ruleMapper = new HashMap<>();

    /**
     * Create a node builder.
     * By default, the tokens mapper extract the value of the tokens
     * and the rules mapper does nothing.
     *
     * @param grammar The grammar used during parsing.
     */
    public NodeBuilder(Grammar grammar) {
        this.grammar = grammar;
    }

    @Override
    public Optional<Node> build(Scanner input, boolean matchAll) throws IOException {
        if (this.grammar.parse(input, this, matchAll)) {
            return Optional.of(this.get());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, TokenMapper<Node>> getTokenMapper() {
        return this.tokenMapper;
    }

    @Override
    public Map<String, RuleMapper<Node>> getRuleMapper() {
        return this.ruleMapper;
    }

}
