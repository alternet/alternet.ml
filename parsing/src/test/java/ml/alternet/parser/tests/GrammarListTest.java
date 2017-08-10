package ml.alternet.parser.tests;

import static ml.alternet.parser.Grammar.*;
import static org.assertj.core.api.Assertions.assertThat;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import ml.alternet.parser.Grammar;
import ml.alternet.parser.ast.NodeBuilder;
import ml.alternet.scan.Scanner;

public class GrammarListTest {

    enum Items {
        item, value, other;
    }

    public interface Seq1 extends Grammar {

        Token COMMA = is(',').skip();

        Rule B = is(Items.class);

        // [1]     A    ::=      B | A ',' B
        @MainRule
        Rule A = $(() -> B.or( $self.seq(COMMA, B))
                .asToken(tokens -> tokens.stream()
                        .map(tv -> (Items) tv.getValue())
                        .collect(toList())
                )
        );

        Seq1 $ = $();

    }

    public interface Seq2 extends Grammar {

        Rule B = is(Items.class);

        Token COMMA = is(',').skip();

        // [1]     A    ::=      B ( ',' B )*
        @MainRule
        Rule A = $(() -> B.seq( COMMA.seq(B).zeroOrMore())
            .asToken(tokens -> tokens.stream()
                    .map(tv -> (Items) tv.getValue())
                    .collect(toList())
            )
        );

        Seq2 $ = $();

    }

    @Test
    public void items_Should_beMatched2() throws IOException {
        NodeBuilder<List<Items>> nb = new NodeBuilder<>(Seq2.$);
        Seq2.$.parse(Scanner.of("value,other,item"), nb, true);
        List<Items> list = nb.get();
        assertThat(list).isEqualTo(Arrays.asList(Items.value, Items.other, Items.item));
    }

    @Test
    public void firstItem_Should_beMatchedBecauseOrConnectorIsLazy() throws IOException {
        NodeBuilder<List<Items>> nb = new NodeBuilder<>(Seq1.$);
        Seq1.$.parse(Scanner.of("value,other,item"), nb, false);
        List<Items> list = nb.get();
        assertThat(list).isEqualTo(Arrays.asList(Items.value));
    }

}
