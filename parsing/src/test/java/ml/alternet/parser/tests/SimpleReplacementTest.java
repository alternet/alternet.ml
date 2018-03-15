package ml.alternet.parser.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.testng.annotations.Test;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.ast.NodeBuilder;
import ml.alternet.parser.handlers.TokensCollector;
import ml.alternet.parser.handlers.TreeHandler;
import ml.alternet.scan.Scanner;

import static ml.alternet.parser.Grammar.*;

/**
 * Allow to test replacement.
 *
 */
public class SimpleReplacementTest {

    public interface Raw extends Grammar {

        CharToken SEP = is('-');

        Token tok = Grammar.range('0', '9').union(SEP);

        @MainRule
        Rule rul = tok.oneOrMore();

        Raw $ = Grammar.$();

    }

    public interface ExtRaw extends Raw {

        CharToken SEP = is('/');

        ExtRaw $ = Grammar.$();

    }

    @Test
    public void rawGrammar_Should_supplyTokens() throws IOException {
        String input = "2018-01-16";
        TokensCollector<StringBuilder> handler = TokensCollector.newStringBuilderHandler();
        Raw.$.parse(Scanner.of(input), handler, true);

        String result = handler.get().toString();
        assertThat(result).isEqualTo(input);
    }

    @Test
    public void extRawGrammar_Should_supplyTokens() throws IOException {
        String input = "2018/01/16";
        TokensCollector<StringBuilder> handler = TokensCollector.newStringBuilderHandler();
        ExtRaw.$.parse(Scanner.of(input), handler, true);

        String result = handler.get().toString();
        assertThat(result).isEqualTo(input);
    }

}
