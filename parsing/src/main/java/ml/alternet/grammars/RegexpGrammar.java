package ml.alternet.grammars;

import static ml.alternet.parser.Grammar.is;

import java.io.IOException;

import ml.alternet.misc.TodoException;

import static ml.alternet.parser.Grammar.$;

import ml.alternet.parser.EventsHandler;
import ml.alternet.parser.Grammar;
import ml.alternet.scan.Scanner;

/**
 * TODO
 *
 * @author Philippe Poulard
 *
 * @see <a href="http://www.unicode.org/reports/tr18/">Unicode TR 18</a>
 */
public interface RegexpGrammar extends Grammar {

    @Override
    default boolean parse(Scanner scanner, EventsHandler handler, Rule rule, boolean matchAll) throws IOException {
        throw new TodoException("GRAMMAR NOT YET IMPLEMENTED");
    }

    Proxy SimpleExp = $();

    Token OPTIONAL = is('?');
    Token ZERO_OR_MORE = is('*');
    Token ONE_OR_MORE = is('+');
    Token ALTERATION = is('|');
    Token ANY_CHAR = is('.');
    Token LEFT_BRACE = is('(');
    Token RIGHT_BRACE = is(')');

    @MainRule
    Rule Exp = SimpleExp.seq(ALTERATION.seq(SimpleExp).zeroOrMore());

    Proxy ElementaryExp = $();

    boolean b1 = SimpleExp.is( ElementaryExp.oneOrMore() );

    // mandatory groups are not processed as optional groups
    // (optional groups must not fire characters as one goes along
    // rather they have to ensure that the group does match)
    Proxy ZeroOrMoreGroup = $();
    Proxy OptionalGroup = $();
    Proxy OneOrMoreGroup = $();
    Proxy MandatoryGroup = $();
    Rule Group = OptionalGroup.or(ZeroOrMoreGroup, OneOrMoreGroup);

    boolean b2 =  OptionalGroup.  is( LEFT_BRACE.seq(Exp, RIGHT_BRACE, OPTIONAL) ) // optional
                ^ ZeroOrMoreGroup.is( LEFT_BRACE.seq(Exp, RIGHT_BRACE, ZERO_OR_MORE) ) // optional
                ^ OneOrMoreGroup. is( LEFT_BRACE.seq(Exp, RIGHT_BRACE, ONE_OR_MORE ) ) // can fire characters
                ^ MandatoryGroup .is( LEFT_BRACE.seq(Exp, RIGHT_BRACE) ); // can fire characters

    Token RANGE = is('-');
    Proxy Chr = $();
    Rule Range = Chr.seq(RANGE, Chr);

    Rule SetItem = Range.or(Chr);
    Rule SetItems = SetItem.zeroOrMore();

/*
    <RE>    ::=     <union> | <simple-RE>
    <union>     ::= <RE> "|" <simple-RE>
    <simple-RE>     ::=     <concatenation> | <basic-RE>
    <concatenation>     ::= <simple-RE> <basic-RE>
    <basic-RE>  ::= <star> | <plus> | <elementary-RE>
    <star>  ::= <elementary-RE> "*"
    <plus>  ::= <elementary-RE> "+"
    <elementary-RE>     ::= <group> | <any> | <eos> | <char> | <set>
    <group>     ::=     "(" <RE> ")"
    <any>   ::=     "."
    <eos>   ::=     "$"
    <char>  ::=     any non metacharacter | "\" metacharacter
    <set>   ::=     <positive-set> | <negative-set>
    <positive-set>  ::=     "[" <set-items> "]"
    <negative-set>  ::=     "[^" <set-items> "]"
    <set-items>     ::=     <set-item> | <set-item> <set-items>
    <set-items>     ::=     <range> | <char>
    <range>     ::=     <char> "-" <char>
*/
 // * is not zeroOrMore because it is greedy (each repetition have to be marked and backtrack occurs for the last-1 after a fail and so on)

//    static class Expr extends Token {
//        String string;
//        NFA nfa;
//        public Expr(String string) {
//            this.string = string;
//            this.nfa = new NFAFactory().createNFA(string);
//        }
//
//        @Override
//        public boolean parse(Scanner scanner, HandlerBuffer handler) throws IOException {
//            // TODO : once the regexp doesn't match anymore, it success if it is at the end, and fails if more regexp have to match
//            scanner.mark();
//            String input = scanner.getRemainderString();
//            if (this.nfa.test(input)) {
//                scanner.consume();
//                handler.emit(this, input);
//                return true;
//            } else {
//                scanner.cancel();
//                return false;
//            }
//        }
//    }

}
