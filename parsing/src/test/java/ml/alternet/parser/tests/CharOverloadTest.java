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

/**
 * Allow to test overload on CharTokens.
 *
 * Combined CharTokens are optimized by merging ; this test ensure that
 * an overload can be taken into account.
 */
public class CharOverloadTest {

    public interface Base extends Grammar {

        CharToken sep = Grammar.is('-');

        Token tok = Grammar.range('0', '9').union(sep);

        @MainRule
        Rule rul = tok.oneOrMore();

        Base $ = Grammar.$();

    }

    public interface Ext extends Base {

        Token sep = Grammar.is('_');

        Ext $ = Grammar.$();

    }

    @Test
    public void charOptimization_ShouldNot_affectCharOverload() throws IOException {
        // first pass : Ext has not yet been initialized
        String input = "2018-01-16";
        TokensCollector<StringBuilder> handler = TokensCollector.newStringBuilderHandler();
        Base.$.parse(Scanner.of(input), handler, true);
        String result = handler.get().toString();
        assertThat(result).isEqualTo(input);

        // 2nd pass : check Ext with its changes
        input = "2018_01_16";
        handler = TokensCollector.newStringBuilderHandler();
        Ext.$.parse(Scanner.of(input), handler, true);
        result = handler.get().toString();
        assertThat(result).isEqualTo(input);

        // 3rd pass : ensure that Ext didn't affect Base
        input = "2018-01-16";
        handler = TokensCollector.newStringBuilderHandler();
        Base.$.parse(Scanner.of(input), handler, true);
        result = handler.get().toString();
        assertThat(result).isEqualTo(input);

    }

    public interface RawBase extends Grammar {

        Token sep = Grammar.is('-');

        Token tok = Grammar.range('0', '9').or(Grammar.is(sep)).asToken();

        @MainRule
        Rule rul = tok.oneOrMore();

        RawBase $ = Grammar.$();

    }

    public interface ExtRaw extends RawBase {

        Token sep = Grammar.is('_');

        ExtRaw $ = Grammar.$();

    }

    @Test
    public void charOptimization_ShouldBe_blocked() throws IOException {
        // first pass : Ext has not yet been initialized
        String input = "2018-01-16";
        TokensCollector<StringBuilder> handler = TokensCollector.newStringBuilderHandler();
        RawBase.$.parse(Scanner.of(input), handler, true);
        String result = handler.get().toString();
        assertThat(result).isEqualTo(input);

        // 2nd pass : check Ext with its changes
        input = "2018_01_16";
        handler = TokensCollector.newStringBuilderHandler();
        ExtRaw.$.parse(Scanner.of(input), handler, true);
        result = handler.get().toString();
        assertThat(result).isEqualTo(input);

        // 3rd pass : ensure that Ext didn't affect Base
        input = "2018-01-16";
        handler = TokensCollector.newStringBuilderHandler();
        RawBase.$.parse(Scanner.of(input), handler, true);
        result = handler.get().toString();
        assertThat(result).isEqualTo(input);

    }

}
