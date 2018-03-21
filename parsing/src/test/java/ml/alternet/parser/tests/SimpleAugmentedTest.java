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
 * Allow to test augmentation ; an augmentation takes an existing rule
 * and apply a mapper on it.
 *
 * Augmentation in the same grammar should work in the same way that
 * augmentation in a separate grammar.
 *
 * Raw test just for checking that the grammar is correct.
 */
public class SimpleAugmentedTest {

    public interface Raw extends Grammar {

        Token tok = Grammar.range('0', '9').union('-');

        @MainRule
        Rule rul = tok.oneOrMore();

        Raw $ = Grammar.$();

    }

    public interface Augmented extends Raw {

        @MainRule
        TypedToken<LocalDate> rul = Raw.rul
            .asToken((tokens)-> LocalDate.parse(
                    tokens.stream()
                        .map(t -> (String) t.getValue())
                        .collect(Collectors.joining())
                ));

        Augmented $ = Grammar.$();

    }

    public interface ExtRaw extends Raw {

        @MainRule
        Rule list = rul.seq( Grammar.is(',').seq(rul).zeroOrMore() );

        ExtRaw $ = Grammar.$();

    }

    public interface AugmentedWithRef extends ExtRaw {

        TypedToken<LocalDate> rul = ExtRaw.rul
            .asToken(tokens -> LocalDate.parse(
                tokens.stream()
                    .map(t -> (String) t.getValue())
                    .collect(Collectors.joining())
            ));

        @MainRule
        TypedToken<List<LocalDate>> list = ExtRaw.list
            .asToken(tokens -> tokens.stream()
                .filter(t -> t.getValue() instanceof LocalDate) // filter out ","
                .map(t -> (LocalDate) t.getValue())
                .collect(Collectors.toList())
            );

        AugmentedWithRef $ = Grammar.$();

    }

    public interface AugmentedInPlace extends Grammar {

        Token tok = Grammar.range('0', '9').union('-');

        @MainRule
        TypedToken<LocalDate> rul = tok.oneOrMore()
            .asToken((tokens)-> LocalDate.parse(
                tokens.stream()
                    .map(t -> (String) t.getValue())
                    .collect(Collectors.joining())
            ));

        AugmentedInPlace $ = Grammar.$();

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
    public void augmentedInPlaceGrammar_Should_supplyCustomObject() throws IOException {
        String input = "2018-01-16";
        NodeBuilder<LocalDate> parser = new NodeBuilder<>(AugmentedInPlace.$);
        Optional<LocalDate> result = parser.parse(input, true);

        assertThat(result).isNotEmpty();
        LocalDate date = result.get();
        assertThat(date).isEqualTo(LocalDate.parse(input));
    }

    @Test
    public void augmentedGrammar_Should_supplyCustomObject() throws IOException {
        String input = "2018-01-16";
        NodeBuilder<LocalDate> parser = new NodeBuilder<>(Augmented.$);
        Optional<LocalDate> result = parser.parse(input, true);

        assertThat(result).isNotEmpty();
        LocalDate date = result.get();
        assertThat(date).isEqualTo(LocalDate.parse(input));
    }

    @Test
    public void augmentedWithRefGrammar_Should_supplyCustomObject() throws IOException {
        String input = "2018-01-16";
        NodeBuilder<List<LocalDate>> parser = new NodeBuilder<>(AugmentedWithRef.$);
        Optional<List<LocalDate>> result = parser.parse(input, true);

        assertThat(result).isNotEmpty();
        LocalDate date = result.get().get(0);
        assertThat(date).isEqualTo(LocalDate.parse(input));
    }

}
