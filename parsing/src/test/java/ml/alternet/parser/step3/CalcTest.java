package ml.alternet.parser.step3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import ml.alternet.parser.Grammar;

public class CalcTest {

    @Test
    public void calcGrammarSingleton_ShouldBe_generated() {
        CalcGrammar g = CalcGrammar.Calc;
        assertThat(g).isInstanceOf(CalcGrammar.class);
        assertThat(g).isInstanceOf(Grammar.class);
        CalcGrammar g2 = Grammar.$(CalcGrammar.class);
        assertThat(g).isSameAs(g2);
    }

    @Test(enabled=false)
    public void calcExpression_CanBe_evaluated() throws IOException {
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 1.0);
        variables.put("var_12", 10.0);

        Optional<Expression> exp = ExpressionBuilder.build("sin(x)*(1+var_12)");

        assertThat(exp).isNotEmpty();

        double res = exp.get().eval(variables);
        double expected = Math.sin(1.0)*(1+10.0);
        assertThat(res).isEqualTo(expected);
    }

    @DataProvider(name = "expressions")
    public static Object[][] createData() {
        Object[][] data = {
            {" 1 ", 1},
            {"+1", 1},
            {"-1", -1},
            {"sin( x )* (1 + var_12) ", Math.sin(1)*(1+10)},
            {"1 + 20 * 300", 1+20*300},
            {"1*20 + 300", 1*20+300},
            {"( 1+20 )*300", (1+20)*300},
            {"1* (20+300) ", 1*(20+300)},
            {"1+(20 * 300)", 1+(20*300)},
            {"(1 * 20)+300", (1*20)+300}
        };
        return data;
    }

    @Test(dataProvider = "expressions")
    public void calcExpression_Can_compute(String expression, double expected) throws IOException {
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 1.0);
        variables.put("var_12", 10.0);

        Optional<Expression> exp = ExpressionBuilder.build(expression);

        assertThat(exp).isNotEmpty();

        double res = exp.get().eval(variables);
        assertThat(res).isEqualTo(expected);
    }

}
