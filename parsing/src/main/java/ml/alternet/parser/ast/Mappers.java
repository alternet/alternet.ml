package ml.alternet.parser.ast;

import java.util.Deque;
import java.util.Map;
import java.util.stream.Stream;

import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.handlers.ValueMapper;
import ml.alternet.parser.handlers.ValueMapper.Value;
import ml.alternet.parser.util.ValueStack.Stackable;

/**
 * Hold all the mappings.
 *
 * Unlike with {@code ValueMapper} the context of the transformation
 * is enlarged to the stack plus the current token and its next values,
 * or the current rule and its arguments.
 *
 * @author Philippe Poulard
 */
public interface Mappers<T> extends Stackable<Value<T>>, ValueMapper<T> {

    @Override
    default Value<T> tokenToValue(TokenValue<?> token, Deque<Value<T>> next) {
        T expr = getTokenMapper().getOrDefault(
                token.getRule().getName(),
                (s, t, n) -> null)
            .transform(getStack(), token, next);
        if (expr == null) {
            // no transformations was made
            return new Value<T>().setSource(token);
        } else {
            // we have it
            return new Value<T>().setTarget(expr);
        }
    }

    @Override
    default Value<T> ruleToValue(Rule rule, Deque<Value<T>> args) {
        T expr = getRuleMapper().getOrDefault(
                rule.getName(),
                (s, r, a) -> null)
            .transform(getStack(), rule, args);
        if (expr == null) {
            return null; // discard
        } else {
            return new Value<T>().setTarget(expr);
        }
    }

    /**
     * Return the tokens mapper.
     *
     * @return The tokens mapper.
     */
    Map<String, TokenMapper<T>> getTokenMapper();

    /**
     * Return the rules mapper.
     *
     * @return The rules mapper.
     */
    Map<String, RuleMapper<T>> getRuleMapper();

    /**
     * Set the token mapper to this mappers.
     *
     * @param tokenMapper The token mapper.
     *
     * @return This, for chaining.
     *
     * @param <B> This mapper must be a builder.
     */
    @SuppressWarnings({ "unchecked" })
    default <B extends Mappers<T> & Builder<?>> B setTokenMapper(Class<? extends Enum<? extends TokenMapper<T>>> tokenMapper) {
        getTokenMapper().clear();
        Stream.of(tokenMapper.getEnumConstants())
            .forEach(e -> getTokenMapper().put(e.name(), (TokenMapper<T>) e));
        return (B) this;
    }

    /**
     * Set the token mapper to this mappers.
     *
     * @param tokenMapper The token mapper.
     *
     * @return This, for chaining.
     *
     * @param <B> This mapper must be a builder.
     */
    @SuppressWarnings("unchecked")
    default <B extends Mappers<T> & Builder<?>> B setTokenMapper(Map<String, TokenMapper<T>> tokenMapper) {
        getTokenMapper().clear();
        getTokenMapper().putAll(tokenMapper);
        return (B) this;
    }

    /**
     * Set the rule mapper to this mappers.
     *
     * @param ruleMapper The rule mapper.
     *
     * @return This, for chaining.
     *
     * @param <B> This mapper must be a builder.
     */
    @SuppressWarnings("unchecked")
    default <B extends Mappers<T> & Builder<?>> B setRuleMapper(Map<String, RuleMapper<T>> ruleMapper) {
        getRuleMapper().clear();
        getRuleMapper().putAll(ruleMapper);
        return (B) this;
    }

    /**
     * Set the rule mapper to this mappers.
     *
     * @param ruleMapper The rule mapper.
     *
     * @return This, for chaining.
     *
     * @param <B> This mapper must be a builder.
     */
    @SuppressWarnings({ "unchecked" })
    default <B extends Mappers<T> & Builder<?>> B setRuleMapper(Class<? extends Enum<? extends RuleMapper<T>>> ruleMapper) {
        getRuleMapper().clear();
        Stream.of(ruleMapper.getEnumConstants())
            .forEach(e -> getRuleMapper().put(e.name(), (RuleMapper<T>) e));
        return (B) this;
    }

}
