package ml.alternet.parser.step5;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.ast.RuleMapper;
import ml.alternet.parser.ast.TokenMapper;
import ml.alternet.parser.handlers.ValueMapper.Value;
import ml.alternet.parser.step4.ExpressionBuilder;
import ml.alternet.parser.step4.NumericExpression;
import ml.alternet.parser.step4.Calc;
import ml.alternet.parser.step4.Calc.Multiplicative;
import ml.alternet.parser.step4.NumericExpression.Product;
import ml.alternet.parser.step4.NumericExpression.Term;
import ml.alternet.parser.step5.Math.MathMultiplicative;
import ml.alternet.parser.util.ValueStack;
import ml.alternet.util.EnumUtil;

public class MathTest {

    @Test
    public void calcGrammarSingleton_ShouldBe_generated() {
        Math g = Math.$;
        assertThat(g).isInstanceOf(Math.class);
        assertThat(g).isInstanceOf(Grammar.class);
        Math g2 = Grammar.$(Math.class);
        assertThat(g).isSameAs(g2);
    }

    @Test
    public void calcExpression_CanBe_evaluated() throws IOException {
        Map<String, Number> variables = new HashMap<>();
        variables.put("X", 1.0);
        variables.put("VAR_12", 10.0);

        Optional<NumericExpression> exp = new MathExpressionBuilder().parse("asin(X)×(1+VAR_12)", true);

        assertThat(exp).isNotEmpty();

        Number res = exp.get().eval(variables);
        Number expected = java.lang.Math.asin(1.0)*(1+10.0);
        assertThat(res).isEqualTo(expected);
    }

    @DataProvider(name = "expressions")
    public static Object[][] createData() {
        Object[][] data = {
            {" 1 ", 1},
            {"+1", 1},
            {"-1", -1},
            {"asin( X )× (1 + VAR_12) ", java.lang.Math.asin(1)*(1+10)},
            {"1 + 20 × 300", 1+20*300},
            {"1×20 + 300", 1*20+300},
            {"( 1+20 )×300", (1+20)*300},
            {"1× (20+300) ", 1*(20+300)},
            {"1+(20 × 300)", 1+(20*300)},
            {"(1 × 20)+300", (1*20)+300}
        };
        return data;
    }

    @Test(dataProvider = "expressions")
    public void calcExpression_Can_compute(String expression, double expected) throws IOException {
        Map<String, Number > variables = new HashMap<>();
        variables.put("X", 1.0);
        variables.put("VAR_12", 10.0);

        Optional<NumericExpression> exp = new MathExpressionBuilder().parse(expression, true);

        assertThat(exp).isNotEmpty();

        Number res = exp.get().eval(variables);
        assertThat(res.doubleValue()).isEqualTo(expected);
    }

}
