package ml.alternet.parser.step6;

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
import ml.alternet.parser.util.ValueStack;
import ml.alternet.parser.visit.Dump;
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

    public static void main(String[] args) {
        System.out.println(Dump.tree(Calc.$, Math.Expression));
        System.out.println(Dump.tree(Math.$, Math.Expression));
        System.out.println(Dump.tree(Calc.$, Math.Argument));
        System.out.println(Dump.tree(Math.$, Math.Argument));

        System.out.println("================");
        Dump d = new Dump().withoutClass().withoutHash().setVisited(Calc.Product);
        Calc.SignedTerm.accept(d);
        System.out.println(d);

        System.out.println("================");
        d = new Dump().withoutClass().withoutHash().setVisited(Calc.Factor);
        Calc.SignedFactor.accept(d);
        System.out.println(d);

        System.out.println("================");
        d = new Dump().withoutClass().withoutHash().setVisited(Calc.SignedTerm).setVisited(Calc.Product);
        Calc.Sum.accept(d);
        System.out.println(d);
    }

}
