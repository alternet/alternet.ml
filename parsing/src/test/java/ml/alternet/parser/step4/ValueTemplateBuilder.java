package ml.alternet.parser.step4;

import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import ml.alternet.parser.ast.NodeBuilder;
import ml.alternet.parser.ast.RuleMapper;
import ml.alternet.parser.ast.TokenMapper;
import ml.alternet.parser.util.ValueStack;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.step4.StringExpression.*;

public class ValueTemplateBuilder extends NodeBuilder<StringExpression> {

    public ValueTemplateBuilder() {
        super(ValueTemplate.$);
        setTokenMapper(Tokens.class);
        setRuleMapper(Rules.class);
    }

    /**
     * Tokens to Expression mapper.
     *
     * @author Philippe Poulard
     */
    enum Tokens implements TokenMapper<StringExpression> {

        EXPRESSION {
            @Override
            public StringExpression transform(
                    ValueStack<Value<StringExpression>> stack,
                    TokenValue<?> token,
                    Deque<Value<StringExpression>> next) {
                NumericExpression num = token.getValue();
                return new CalcExpression(num);
            }
        },
        ESCAPE_LCB {
            @Override
            public StringExpression transform(
                    ValueStack<Value<StringExpression>> stack,
                    TokenValue<?> token,
                    Deque<Value<StringExpression>> next) {
                token.setValue("{");
                return null;
            }
        },
        ESCAPE_RCB {
            @Override
            public StringExpression transform(
                    ValueStack<Value<StringExpression>> stack,
                    TokenValue<?> token,
                    Deque<Value<StringExpression>> next) {
                token.setValue("}");
                return null;
            }
        };
    }

    /**
     * Rules to Expression mapper.
     *
     * @author Philippe Poulard
     */
    enum Rules implements RuleMapper<StringExpression> {

        ValueTemplate {
            @Override
            public StringExpression transform(
                    ValueStack<Value<StringExpression>> stack,
                    Rule rule,
                    Deque<Value<StringExpression>> args)
            {
                // ValueTemplate ::= ( Text EXPRESSION? )*
                List<StringExpression> expressions = args.stream()
                    .map(v -> v.getTarget())
                    .collect(Collectors.toList());
                return new ValueTemplateExpression(expressions);
            }
        }, Text {
            @Override
            public StringExpression transform(
                    ValueStack<Value<StringExpression>> stack,
                    Rule rule,
                    Deque<Value<StringExpression>> args)
            {
                String text = args.stream()
                    .map(v -> (String) ((StringValue) v.getSource()).getValue())
                    .collect(Collectors.joining());
                return new TextExpression(text);
            }
        };

    }

}
