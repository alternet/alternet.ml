package ml.alternet.parser.www;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.Grammar.Rule;
import ml.alternet.parser.ast.NodeBuilder;
import ml.alternet.parser.ast.TokenMapper;
import ml.alternet.parser.handlers.TreeHandler;
import ml.alternet.parser.tests.BasicTest.BasicGrammar;
import ml.alternet.parser.www.WAuth.Challenge;
import org.assertj.core.api.Assertions.*;

public class TypedTokenTest {

    @Test
    public void wwwChallenge_Should_be_processed() throws IOException {
        String input = "Basic realm=\"FooCorp\"";
        NodeBuilder<Challenge> parser = new NodeBuilder<>(WAuth.$);
        Optional<Challenge> result = parser.build(input, true);

        assertThat(result).isNotEmpty();
        Challenge challenge = result.get();
        assertThat(challenge.scheme).isEqualTo("Basic");
        assertThat(challenge.parameters).containsExactly(new WAuth.Parameter("realm", "FooCorp"));
    }

    @Test
    public void wwwChallengeWithSeveralParameters_Should_be_processed() throws IOException {
        String input = "Basic realm=\"FooCorp\", error=invalid_token, error_description=\"The \\\"access token \\\" has expired\"";
        NodeBuilder<Challenge> parser = new NodeBuilder<>(WAuth.$);
        Optional<Challenge> result = parser.build(input, true);

        assertThat(result).isNotEmpty();
        Challenge challenge = result.get();
        assertThat(challenge.scheme).isEqualTo("Basic");
        assertThat(challenge.parameters).containsExactly(
                new WAuth.Parameter("realm", "FooCorp"),
                new WAuth.Parameter("error", "invalid_token"),
                new WAuth.Parameter("error_description", "The \"access token \" has expired")
        );
    }

}
