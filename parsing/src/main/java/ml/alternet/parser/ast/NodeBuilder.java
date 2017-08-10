package ml.alternet.parser.ast;

import java.io.IOException;
import java.io.Reader;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.Grammar.Token;
import ml.alternet.parser.handlers.TreeHandler;
import ml.alternet.scan.Scanner;

/**
 * Builds an AST from a grammar, tokens mapper,
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
public class NodeBuilder<Node> extends TreeHandler<Node> {

    Grammar grammar;
    Function<Token, TokenMapper<Node>> tokenMapper;
    Function<Rule, RuleMapper<Node>> ruleMapper;

    /**
     * Create a node builder.
     * By default, the tokens mapper extract the value of the tokens
     * and the rules mapper does nothing.
     *
     * @param grammar The grammar used during parsing.
     */
    public NodeBuilder(Grammar grammar) {
        this.grammar = grammar;
        this.tokenMapper = aToken -> (stack, token, next) -> token.getValue();
        this.ruleMapper = aRule -> (stack, rule, args) -> null;
    }

    /**
     * Set the token mapper to this node builder.
     *
     * @param tokenMapper The token mapper.
     *
     * @return This, for chaining.
     */
    public NodeBuilder<Node> setTokenMapper(Function<Token, TokenMapper<Node>> tokenMapper) {
        this.tokenMapper = tokenMapper;
        return this;
    }

    /**
     * Set the token mapper to this node builder.
     *
     * @param tokenMapper The token mapper.
     *
     * @return This, for chaining.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public NodeBuilder<Node> setTokenMapper(Class<? extends Enum<? extends TokenMapper<Node>>> tokenMapper) {
        this.tokenMapper = (token) -> {
            String tokenName = token.getName(); // e.g. "FUNCTION"
            // find it with the same name
            try {
                return (TokenMapper<Node>) Enum.valueOf((Class<? extends Enum>) tokenMapper, tokenName);
            } catch (IllegalArgumentException e) {
                return (stack, tokN, next) -> null;
            }
        };
        return this;
    }

    /**
     * Set the token mapper to this node builder.
     *
     * @param tokenMapper The token mapper.
     *
     * @return This, for chaining.
     */
    public NodeBuilder<Node> setTokenMapper(Map<Token , TokenMapper<Node>> tokenMapper) {
        this.tokenMapper = (token) -> tokenMapper.getOrDefault(token, (stack, tokN, next) -> null);
        return this;
    }

    /**
     * Set the token mapper to this node builder.
     *
     * @param tokenMapper The token mapper.
     *
     * @return This, for chaining.
     */
    public NodeBuilder<Node> setTokenNameMapper(Map<String, TokenMapper<Node>> tokenMapper) {
        this.tokenMapper = (token) -> tokenMapper.getOrDefault(token.getName(), (stack, tokN, next) -> null);
        return this;
    }

    /**
     * Set the rule mapper to this node builder.
     *
     * @param ruleMapper The rule mapper.
     *
     * @return This, for chaining.
     */
    public NodeBuilder<Node> setRuleMapper(Function<Rule , RuleMapper<Node>> ruleMapper) {
        this.ruleMapper = ruleMapper;
        return this;
    }

    /**
     * Set the rule mapper to this node builder.
     *
     * @param ruleMapper The rule mapper.
     *
     * @return This, for chaining.
     */
    public NodeBuilder<Node> setRuleMapper(Map<Rule , RuleMapper<Node>> ruleMapper) {
        this.ruleMapper = (rule) -> ruleMapper.getOrDefault(rule, (stack, token, next) -> null);
        return this;
    }

    /**
     * Set the rule mapper to this node builder.
     *
     * @param ruleMapper The rule mapper.
     *
     * @return This, for chaining.
     */
    public NodeBuilder<Node> setRuleNameMapper(Map<String, RuleMapper<Node>> ruleMapper) {
        this.ruleMapper = (rule) -> ruleMapper.getOrDefault(rule.getName(), (stack, token, next) -> null);
        return this;
    }

    /**
     * Set the rule mapper to this node builder.
     *
     * @param ruleMapper The rule mapper.
     *
     * @return This, for chaining.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public NodeBuilder<Node> setRuleMapper(Class<? extends Enum<? extends RuleMapper<Node>>> ruleMapper) {
        this.ruleMapper = (rule) -> {
            String ruleName = rule.getName(); // e.g. "Product"
            // find it with the same name
            return (RuleMapper<Node>) Enum.valueOf((Class<? extends Enum>) ruleMapper, ruleName);
        };
        return this;
    }

    @Override
    public Value<Node> tokenToNode(TokenValue<?> token, Deque<Value<Node>> next) {
        Node expr = this.tokenMapper.apply(token.getRule())
                .transform(this.stack, token, next);
        if (expr == null) {
            // no transformations was made
            return new Value<Node>().setSource(token);
        } else {
            // we have it
            return new Value<Node>().setTarget(expr);
        }
    }

    @Override
    public Value<Node> ruleToNode(Rule rule, Deque<Value<Node>> args) {
        Node expr = this.ruleMapper.apply(rule)
            .transform(this.stack, rule, args);
        if (expr == null) {
            return null; // discard
        } else {
            return new Value<Node>().setTarget(expr);
        }
    }

    /**
     * Entry point for building an expression.
     *
     * @param input An expression that follows the grammar, e.g. "sin(x)*(1+var_12)"
     * @param matchAll <code>true</code> to indicates that if
     *      the input contains more characters at the end of
     *      the parsing, an error will be reported, <code>false</code>
     *      otherwise.
     *
     * @return An evaluable expression.
     *
     * @throws IOException When an I/O error occurs.
     */
    public Optional<Node> build(String input, boolean matchAll) throws IOException {
        return build(Scanner.of(input), matchAll);
    }

    /**
     * Entry point for building an expression.
     *
     * @param input An expression that follows the grammar, e.g. "sin(x)*(1+var_12)"
     * @param matchAll <code>true</code> to indicates that if
     *      the input contains more characters at the end of
     *      the parsing, an error will be reported, <code>false</code>
     *      otherwise.
     *
     * @return An evaluable expression.
     *
     * @throws IOException When an I/O error occurs.
     */
    public Optional<Node> build(Reader input, boolean matchAll) throws IOException {
        return build(Scanner.of(input), matchAll);
    }

    /**
     * Entry point for building an expression.
     *
     * @param input An expression that follows the grammar, e.g. "sin(x)*(1+var_12)"
     * @param matchAll <code>true</code> to indicates that if
     *      the input contains more characters at the end of
     *      the parsing, an error will be reported, <code>false</code>
     *      otherwise.
     *
     * @return An evaluable expression.
     *
     * @throws IOException When an I/O error occurs.
     */
    public Optional<Node> build(Scanner input, boolean matchAll) throws IOException {
        if (this.grammar.parse(input, this, matchAll)) {
            return Optional.of(this.get());
        } else {
            return Optional.empty();
        }
    }

}
