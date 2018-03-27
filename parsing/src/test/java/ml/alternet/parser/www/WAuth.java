package ml.alternet.parser.www;

import ml.alternet.parser.Grammar;
import static ml.alternet.parser.Grammar.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;

import ml.alternet.parser.EventsHandler.StringValue;
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
public interface WAuth extends Grammar {

/*
    # from RFC-2617 (HTTP Basic and Digest authentication)

    challenge      = auth-scheme 1*SP 1#auth-param
    auth-scheme    = token
    auth-param     = token "=" ( token | quoted-string )

    # from RFC2616 (HTTP/1.1)

    token          = 1*<any CHAR except CTLs or separators>
    separators     = "(" | ")" | "<" | ">" | "@"
                   | "," | ";" | ":" | "\" | <">
                   | "/" | "[" | "]" | "?" | "="
                   | "{" | "}" | SP | HT
    quoted-string  = ( <"> *(qdtext | quoted-pair ) <"> )
    qdtext         = <any TEXT except <">>
    quoted-pair    = "\" CHAR
*/

    @Fragment
    CharToken SP = is(' ');

    @Fragment
    Token SEPARATORS = isOneOf("()<>@,;:\\\"/[]?={} \t");
    @Fragment
    Token CTRLS = range(0, 31).union(127); // octets 0 - 31 and DEL (127)

    @Fragment
    Token TOKEN_CHAR = isNot(SEPARATORS, CTRLS);

    @Skip(token="SP")
    Token TOKEN = TOKEN_CHAR.oneOrMore().asToken();

    @Fragment
    Token DOUBLE_QUOTE = is('"');

    @Fragment
    @Drop
    Token BACKSLASH = is('\\');

    @Fragment
    Token QdText = isNot(DOUBLE_QUOTE);

    @Fragment
    Token QuotedPair = BACKSLASH.seq($any).asToken();
//        .asToken(tokens -> {
//            tokens.removeFirst(); // skip BACKSLASH
//            return tokens.getFirst().getValue();
//        });

    Token QuotedString = DOUBLE_QUOTE.drop().seq(
            QuotedPair.or(QdText).zeroOrMore(),
            DOUBLE_QUOTE.drop())
        .asToken();
//        .asToken(tokens -> {
//            tokens.removeFirst(); // skip DOUBLE_QUOTE before...
//            tokens.removeLast();  // ...and after
//            return tokens.stream()
//                .map(v -> (String) v.getValue())
//                .collect(joining());
//        });

    Token ParameterValue = TOKEN.or(QuotedString).asToken();

    @Fragment
    Token EQUAL = is('=');

    TypedToken<Parameter> Parameter = TOKEN.seq(EQUAL, ParameterValue)
        .asToken(tokens ->
            new Parameter(
                    tokens.getFirst().getValue(), // TOKEN
                    tokens.getLast().getValue()   // ParameterValue
        ));

    @Skip(token="SP")
    @Fragment Token COMMA = is(',');

    // Parameters ::= Parameter (COMMA Parameter?)*
    TypedToken<List<Parameter>> Parameters = Parameter.seq(COMMA.seq(Parameter.optional()).zeroOrMore())
        .asToken(tokens ->
                tokens.stream()
                    // drop ","
                    .filter(t -> t.getRule().id() != COMMA.id())
                    // extract the value as a Parameter
                    .map(t -> (Parameter) t.getValue())
                    .collect(toList())
        );

    @MainRule
    TypedToken<Challenge> Challenge = TOKEN.seq(Parameters)
        .asToken(tokens -> new Challenge(
                tokens.removeFirst().getValue(),
                tokens.removeFirst().getValue())
            );

    WAuth $ = $();

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
