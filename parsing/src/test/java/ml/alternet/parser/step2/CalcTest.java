package ml.alternet.parser.step2;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 1.0);
        variables.put("var_12", 10.0);

        Optional<Expression> exp = ExpressionBuilder.build("sin(x)*(1+var_12)");

        assertThat(exp).isNotEmpty();

        double res = exp.get().eval(variables);
        double expected = Math.sin(1.0)*(1+10.0);
        assertThat(res).isEqualTo(expected);
    }

}
