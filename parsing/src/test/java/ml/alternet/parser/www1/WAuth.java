package ml.alternet.parser.www1;

import ml.alternet.parser.Grammar;
import static ml.alternet.parser.Grammar.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;

import ml.alternet.parser.EventsHandler.StringValue;

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

    @Fragment Token SEPARATORS = isOneOf("()<>@,;:\\\"/[]?={} \t");
    @Fragment Token CTRLS = range(0, 31).union(127); // octets 0 - 31 and DEL (127)

    @WhitespacePolicy(preserve=true)
    @Fragment Token TOKEN_CHAR = isNot(SEPARATORS, CTRLS);

    @WhitespacePolicy
    Token TOKEN = TOKEN_CHAR.oneOrMore().asToken();

    @Fragment Token DOUBLE_QUOTE = is('"');
    @Fragment Token BACKSLASH = is('\\')
            .drop();

    @WhitespacePolicy(preserve=true)
    @Fragment Token QdText = isNot(DOUBLE_QUOTE);

    @WhitespacePolicy(preserve=true)
    @Fragment Token QuotedPair = BACKSLASH.seq($any).asToken();

    Token QuotedString = DOUBLE_QUOTE.drop().seq(
            QuotedPair.or(QdText).zeroOrMore(),
            DOUBLE_QUOTE.drop())
        .asToken();

    Token ParameterValue = TOKEN.or(QuotedString).asToken();

    @Fragment Token EQUAL = is('=');

    // Parameter ::= TOKEN     EQUAL  ParameterValue
    Rule Parameter = TOKEN.seq(EQUAL, ParameterValue);

    @WhitespacePolicy
    @Fragment Token COMMA = is(',');

    // Parameters ::= Parameter (COMMA Parameter?)*
    Rule Parameters = Parameter
            .seq(COMMA.seq(Parameter.optional()).zeroOrMore());

    // Challenge ::= TOKEN     Parameters
    @MainRule
    Rule Challenge = TOKEN.seq(Parameters);

    WAuth $ = $();

}
