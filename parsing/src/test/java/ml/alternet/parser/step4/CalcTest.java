package ml.alternet.parser.step4;

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
        Calc g = Calc.$;
        assertThat(g).isInstanceOf(Calc.class);
        assertThat(g).isInstanceOf(Grammar.class);
        Calc g2 = Grammar.$(Calc.class);
        assertThat(g).isSameAs(g2);
    }

    @Test
    public void calcExpression_CanBe_evaluated() throws IOException {
        Map<String, Number> variables = new HashMap<>();
        variables.put("x", 1.0);
        variables.put("var_12", 10.0);

        Optional<NumericExpression> exp = new ExpressionBuilder().build("sin(x)*(1+var_12)", true);

        assertThat(exp).isNotEmpty();

        Number res = exp.get().eval(variables);
        Number expected = Math.sin(1.0)*(1+10.0);
        assertThat(res).isEqualTo(expected);
    }

    @DataProvider(name = "expressions")
    public static Object[][] createData() {
        Object[][] data = {
            {" 1 ", 1},
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
        Map<String, Number > variables = new HashMap<>();
        variables.put("x", 1.0);
        variables.put("var_12", 10.0);

        Optional<NumericExpression> exp = new ExpressionBuilder().build(expression, true);

        assertThat(exp).isNotEmpty();

        Number res = exp.get().eval(variables);
        assertThat(res.doubleValue()).isEqualTo(expected);
    }

}
