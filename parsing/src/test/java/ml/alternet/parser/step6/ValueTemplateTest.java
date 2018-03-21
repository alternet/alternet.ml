package ml.alternet.parser.step6;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ValueTemplateTest {

    @DataProvider(name = "expressions")
    public static Object[][] createData() {
        Object[][] data = {
                {" { 1+1 } ", " 2 "},
                {"{ sin( x )* (1 + var_12) }", "" + (java.lang.Math.sin(1)*(1+10))},
                {"{ 1 + 20 * 300 }", "" + (1+20*300)},
                {"{ 1*20 + 300 }", "" + (1*20+300)},
                {"{( 1+20 )*300}", "" + ((1+20)*300)},
                {"{1* (20+300) }", "" + (1*(20+300))},
                {"{1+(20 * 300)}", "" + (1+(20*300))},
                {"{(1 * 20)+300}", "" + ((1*20)+300)},
                {" {{ A }} ", " { A } "},
                {" {{{ 1+1 }}} ", " {2} "},
                {" {{ { 1+1 } }} ", " { 2 } "},
                {" { 1+1 } * {4-1} > { x}", " 2 * 3 > 1"},
                {"", ""},
                {"}}{{", "}{"},
                {"{}{}{}", ""},
                {"a{}b{}c{}d", "abcd"}
        };
        return data;
    }

    @Test(dataProvider = "expressions")
    public void calcExpression_Can_compute(String expression, String expected) throws IOException {
        Map<String, Number> variables = new HashMap<>();
        variables.put("x", 1.0);
        variables.put("var_12", 10.0);

        Optional<StringExpression> exp = ValueTemplateBuilder.forCalcGrammar().build(expression, true);

        assertThat(exp).isNotEmpty();

        String res = exp.get().eval(variables);
        assertThat(res).isEqualTo(expected);
    }

}
