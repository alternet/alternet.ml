package ml.alternet.parser.step6;

import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import ml.alternet.parser.ast.NodeBuilder;
import ml.alternet.parser.ast.RuleMapper;
import ml.alternet.parser.ast.TokenMapper;
import ml.alternet.parser.handlers.ValueMapper.Value;
import ml.alternet.parser.step6.StringExpression.*;
import ml.alternet.parser.util.ValueStack;
import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.Grammar;
import ml.alternet.parser.Grammar.Rule;

public class ValueTemplateBuilder extends NodeBuilder<StringExpression> {

    public static ValueTemplateBuilder forCalcGrammar() {
        return new ValueTemplateBuilder(ValueTemplate.$);
    }

    public static ValueTemplateBuilder forMathGrammar() {
        return new ValueTemplateBuilder(MathValueTemplate.$);
    }

    public ValueTemplateBuilder(Grammar g) {
        super(g);
        setTokenMapper(Tokens.class);
        setRuleMapper(Rules.class);
    }

    /**
     * Tokens to Expression mapper.
     *
     * @author Philippe Poulard
     */
    enum Tokens implements TokenMapper<StringExpression> {

        EXPRESSION( (stack, token, next) -> {
            NumericExpression num = token.getValue();
            return new CalcExpression(num);
        }),
        ESCAPE_LCB( (stack, token, next) -> {
            token.setValue("{");
            return null;
        }),
        ESCAPE_RCB( (stack, token, next) -> {
            token.setValue("}");
            return null;
        });

        TokenMapper<StringExpression> mapper;

        Tokens(TokenMapper<StringExpression> mapper) {
            this.mapper = mapper;
        }

        @Override
        public StringExpression transform(
                ValueStack<Value<StringExpression>> stack,
                TokenValue<?> token,
                Deque<Value<StringExpression>> next)
        {
            return this.mapper.transform(stack, token, next);
        }
    }

    /**
     * Rules to Expression mapper.
     *
     * @author Philippe Poulard
     */
    enum Rules implements RuleMapper<StringExpression> {

        ValueTemplate( (stack, rule, args) -> {
            // ValueTemplate ::= ( Text EXPRESSION? )*
            List<StringExpression> expressions = args.stream()
                .map(v -> v.getTarget())
                .collect(Collectors.toList());
            return new ValueTemplateExpression(expressions);
        }),
        Text( (stack, rule, args) -> {
            String text = args.stream()
                .map(v -> (String) ((StringValue) v.getSource()).getValue())
                .collect(Collectors.joining());
            return new TextExpression(text);
        });

        RuleMapper<StringExpression> mapper;

        Rules(RuleMapper<StringExpression> mapper) {
            this.mapper = mapper;
        }

        @Override
        public StringExpression transform(
                ValueStack<Value<StringExpression>> stack,
                Rule rule,
                Deque<Value<StringExpression>> args)
        {
            return this.mapper.transform(stack, rule, args);
        }

    }

}
