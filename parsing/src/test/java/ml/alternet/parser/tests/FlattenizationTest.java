package ml.alternet.parser.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static ml.alternet.parser.Grammar.*;

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
import ml.alternet.parser.visit.TraversableRule;
import ml.alternet.scan.Scanner;

/**
 * Allow to test flattenization.
 *
 * For example, a sequence of sequences is flatten to a single sequence.
 *
 * EXCEPT : if one item is a field, in that case it MUST be left as-is,
 * otherwise extensions won't be able to be applied.
 *
 * @see TraversableRule#isGrammarField()
 */
public class FlattenizationTest {

    public interface Base extends Grammar {

        Token sep = Grammar.is(':');

        Rule az = range('a', 'z').seq(sep);
        Rule AZ = range('A', 'Z').seq(sep);

        Rule flatSeq = $any.seq(
            range('a', 'z').seq(sep),
            range('A', 'Z').seq(sep)
        );

        Rule notFlatSeq = $any.seq(az, AZ);

        Base $ = $();

    }

    public interface Ext extends Base {

        Token sep = Grammar.is('/');

        Ext $ = $();

    }

    @Test
    public void flattenedRule_Should_workLikeNotFlattenedRule() throws IOException {
        String input = "&z/Z/";
        TokensCollector<StringBuilder> handler = TokensCollector.newStringBuilderHandler();
        Ext.$.parse(Scanner.of(input), handler, Ext.flatSeq, true);
        String result = handler.get().toString();
        assertThat(result).isEqualTo(input);

        handler = TokensCollector.newStringBuilderHandler();
        Ext.$.parse(Scanner.of(input), handler, Ext.notFlatSeq, true);
        result = handler.get().toString();
        assertThat(result).isEqualTo(input);

        input = "&z:Z:";
        handler = TokensCollector.newStringBuilderHandler();
        Base.$.parse(Scanner.of(input), handler, Base.flatSeq, true);
        result = handler.get().toString();
        assertThat(result).isEqualTo(input);

        handler = TokensCollector.newStringBuilderHandler();
        Base.$.parse(Scanner.of(input), handler, Base.notFlatSeq, true);
        result = handler.get().toString();
        assertThat(result).isEqualTo(input);
    }

    @Test
    public void namedRules_ShouldNot_beFlattened() throws IOException {
        List seq = ((Sequence) Base.flatSeq).getComponent();
        assertThat(seq).hasSize(5); // $any [a-z] : [A-Z] :
        seq = ((Sequence) Base.notFlatSeq).getComponent();
        assertThat(seq).hasSize(3); // $any az AZ
    }

}
