package ml.alternet.parser.ast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.handlers.TreeHandler;
import ml.alternet.scan.Scanner;

/**
 * Build an heterogeneous AST from a grammar, tokens mapper,
 * and rules mapper.
 *
 * @author Philippe Poulard
 *
 * @param <T> The type of the result value.
 */
public class ValueBuilder<T> extends TreeHandler<Object, T> implements Builder<T>, Mappers<Object> {

    Grammar grammar;
    Map<String, TokenMapper<Object>> tokenMapper = new HashMap<>();
    Map<String, RuleMapper<Object>> ruleMapper = new HashMap<>();

    /**
     * Create a node builder.
     * By default, the tokens mapper extract the value of the tokens
     * and the rules mapper does nothing.
     *
     * @param grammar The grammar used during parsing.
     */
    public ValueBuilder(Grammar grammar) {
        this.grammar = grammar;
    }

    @Override
    public Optional<T> parse(Scanner input, boolean matchAll) throws IOException {
        if (this.grammar.parse(input, this, matchAll)) {
            return Optional.of(this.get());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, TokenMapper<Object>> getTokenMapper() {
        return this.tokenMapper;
    }

    @Override
    public Map<String, RuleMapper<Object>> getRuleMapper() {
        return this.ruleMapper;
    }

}
