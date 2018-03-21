package ml.alternet.parser.step6;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ml.alternet.parser.ast.Expression;

/**
 * The user data model is a tree of expressions.
 *
 * @author Philippe Poulard
 */
public interface StringExpression extends Expression<String, Map<String,Number>> {

    /**
     * A calc expression embeds a numeric expression.
     *
     * @see Calc
     * @see NumericExpression
     *
     * @author Philippe Poulard
     */
    class CalcExpression implements StringExpression {

        NumericExpression expression;

        public CalcExpression(NumericExpression expression) {
            this.expression = expression;
        }

        @Override
        public String eval(Map<String, Number> context) {
            Number n = this.expression.eval(context);
            // remove trailing .0
            if (n.longValue() == n.doubleValue()) {
                return "" + n.longValue();
            } else {
                return "" + n;
            }
        }

        @Override
        public String toString() {
            return '{' + expression.toString() + '}';
        }

    }

    class TextExpression implements StringExpression {

        String text;

        public TextExpression(String text) {
            this.text = text;
        }

        @Override
        public String eval(Map<String, Number> context) {
            return this.text;
        }

    }

    class ValueTemplateExpression implements StringExpression {

        List<StringExpression> expressions;

        public ValueTemplateExpression(List<StringExpression> expressions) {
            this.expressions = expressions;
        }

        @Override
        public String eval(Map<String, Number> context) {
            return this.expressions.stream()
                .map(e -> e.eval(context))
                .collect(Collectors.joining());
        }

    }

}
