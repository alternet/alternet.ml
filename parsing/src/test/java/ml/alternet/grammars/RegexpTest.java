package ml.alternet.grammars;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.testng.annotations.Test;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.tests.GrammarTestBase;

public class RegexpTest extends GrammarTestBase {

    @Test
    public void simpleRegexp_Should_consumeCharacters() throws IOException {
//        Rule rule = Grammar.matches("Xc|Yc");
//
//        List<String> res = parseToAcc("Xc", Grammar.class, rule);
//        assertThat(res).contains("Token:Xc");

    }

}
