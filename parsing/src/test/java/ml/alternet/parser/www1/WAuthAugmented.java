package ml.alternet.parser.www1;

import ml.alternet.parser.Grammar;
import static ml.alternet.parser.Grammar.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;

import ml.alternet.parser.EventsHandler.StringValue;
import ml.alternet.parser.EventsHandler.TokenValue;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.joining;

/**
 * <pre># Challenge Basic
 * WWW-Authenticate: Basic realm="FooCorp"
 *
 * # Challenge OAuth 2.0 après l'envoi d'un token expiré
 * WWW-Authenticate: Bearer realm="FooCorp", error=invalid_token, error_description="The access token has expired"</pre>
 *
 * @author Philippe Poulard
 */
public interface WAuthAugmented extends WAuth {

    TypedToken<Parameter> Parameter = WAuth.Parameter
        .asToken(tokens ->
            new Parameter(
                    tokens.getFirst().getValue(), // TOKEN
                    tokens.getLast().getValue()   // ParameterValue
        ));

    // Parameters ::= Parameter (COMMA Parameter?)*
    TypedToken<List<Parameter>> Parameters = WAuth.Parameters
        .asToken(tokens ->
                tokens.stream()
                    .map(TokenValue::getValue)
                    .filter(p -> p instanceof Parameter)
                    .map(p -> (Parameter) p)
                    .collect(toList())
        );

    @MainRule
    TypedToken<Challenge> Challenge = WAuth.Challenge
        .asToken(tokens -> new Challenge(
                tokens.removeFirst().getValue(),
                tokens.removeFirst().getValue())
            );

    WAuthAugmented $ = $();

    class Parameter {

        public String name;
        public String value;

        public Parameter(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Parameter) {
                Parameter p = (Parameter) obj;
                return p.name.equals(this.name)
                        && p.value.equals(this.value);
            } else return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.value);
        }

        @Override
        public String toString() {
            return name + "=" + value;
        }

    }

    class Challenge {

        public String scheme;
        public List<Parameter> parameters;

        public Challenge(String scheme, List<Parameter> parameters) {
            this.scheme = scheme;
            this.parameters = parameters;
        }

    }

}
