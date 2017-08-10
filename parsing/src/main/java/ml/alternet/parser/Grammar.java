package ml.alternet.parser;

import static java.util.stream.Collectors.toCollection;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import ml.alternet.facet.Initializable;
import ml.alternet.facet.Presentable;
import ml.alternet.facet.Trackable;
import ml.alternet.misc.CharRange;
import ml.alternet.misc.TodoException;
import ml.alternet.parser.EventsHandler.NumberValue;
import ml.alternet.parser.EventsHandler.RuleEnd;
import ml.alternet.parser.EventsHandler.RuleStart;
import ml.alternet.parser.EventsHandler.StringValue;
import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.handlers.DataHandler;
import ml.alternet.parser.handlers.TokensCollector;
import ml.alternet.parser.util.Grammar$;
import ml.alternet.parser.util.HasWhitespacePolicy;
import ml.alternet.parser.util.TraversableRule;
import ml.alternet.scan.EnumValues;
import ml.alternet.scan.JavaWhitespace;
import ml.alternet.scan.NumberConstraint;
import ml.alternet.scan.Scanner;
import ml.alternet.scan.StringConstraint;
import ml.alternet.scan.StringScanner;
import ml.alternet.util.ByteCodeFactory;
import ml.alternet.util.NumberUtil;
import ml.alternet.util.StringBuilderUtil;

/**
 * Base interface for defining grammars.
 *
 * <h1>Skeleton</h1>
 *
 * <pre>import static ml.alternet.parser.Grammar.*;
 *import ml.alternet.parser.Grammar;
 *
 *&#x40;WhitespacePolicy
 *public interface NumbersGrammar extends Grammar {
 *
 *    // DIGIT ::= [0-9]
 *    &#x40;WhitespacePolicy(preserve=true)
 *    &#x40;Fragment
 *    Token DIGIT = range('0', '9').asNumber();
 *
 *    // NUMBER ::= DIGIT+
 *    Token NUMBER = DIGIT.oneOrMore()
 *            .asNumber();
 *
 *    // Value ::= NUMBER
 *    &#x40;MainRule
 *    &#x40;Fragment
 *    Rule Value = is(NUMBER);
 *
 *    // the instance to use for parsing an input
 *    NumbersGrammar $ = $(); // must be the last field
 *
 *}</pre>
 *
 * @author Philippe Poulard
 *
 * @see <a href="https://en.wikipedia.org/wiki/Parsing_expression_grammar">Parsing Expression Grammar (1)</a>
 * @see <a href="http://www.brynosaurus.com/pub/lang/peg.pdf">Parsing Expression Grammar (2)</a>
 */
public interface Grammar {

    /**
     * Create a char token.
     *
     * <pre>// RAISED ::= '^'
     *Token RAISED = is('^');</pre>
     *
     * @param car The actual Unicode character.
     *
     * @return A char token.
     */
    static CharToken is(int car) {
        return new CharToken(CharRange.is(car));
    }

    /**
     * Create a char token based on exclusion.
     *
     * <pre>// NOT_A_STAR ::= ! '*'
     *Token NOT_A_STAR = isnot('*');</pre>
     *
     * @param car The Unicode character to exclude.
     *
     * @return A char token.
     */
    static CharToken isNot(int car) {
        return new CharToken(CharRange.isNot(car));
    }

    /**
     * Create a char token, the char is one of the ones of a string.
     *
     * <pre>Token SEPARATORS = isOneOf("()&lt;&gt;@,;:\\\"/[]?={} \t");</pre>
     *
     * @param string The actual string
     *
     * @return A char token
     */
    static CharToken isOneOf(String string) {
        return new CharToken(CharRange.isOneOf(string));
    }

    /**
     * Create a char token, the char is none of the ones of a string.
     *
     * <pre>Token SEPARATORS = isNotOneOf("()&lt;&gt;@,;:\\\"/[]?={} \t");</pre>
     *
     * @param string The actual string
     *
     * @return A char token
     */
    static CharToken isNotOneOf(String string) {
        return new CharToken(CharRange.isNotOneOf(string));
    }

    /**
     * Create a string token.
     *
     * <pre>// SIN_FUNCTION ::= 'sin'
     *Token SIN_FUNCTION = is("sin");</pre>
     *
     * @param string The actual string
     *
     * @return A string token
     */
    static StringToken is(String string) {
        return new StringToken(string, true);
    }

    /**
     * Create a string token based on exclusion.
     *
     * <pre>// NOT_EVAL ::= ! 'eval'
     *Token NOT_EVAL = isNot("eval");</pre>
     *
     *The token value is a string of the same length. Uh ?
     *
     * @param string The string to exclude.
     *
     * @return A string token
     */
    static StringToken isNot(String string) {
        return new StringToken(string, false);
    }

    /**
     * Create a token made from a list of string values.
     *
     * @see EnumValues#from(String...)
     *
     * @param values The list of candidate values.
     *
     * @return An enum token.
     */
    static EnumToken<String> is(String... values) {
        return new EnumToken<>(values);
    }

    /**
     * Create a token made from a list of enum values.
     *
     * The token value will be available
     * as an enum value.
     *
     * @see EnumValues#from(Class)
     *
     * @param values Enum the candidate values.
     *
     * @return An enum token.
     */
    @SuppressWarnings("rawtypes")
    static EnumToken<Enum> is(Class<? extends Enum> values) {
        return new EnumToken<>(values);
    }

    /**
     * Create a token made from a value of an enum.
     *
     * The token value will be available
     * as this enum value.
     *
     * @see EnumValues#from(Class)
     *
     * @param value The enum value to match.
     *
     * @return An enum value token.
     */
    @SuppressWarnings("rawtypes")
    static EnumValueToken is(Enum value) {
        return new EnumValueToken(value);
    }

    /**
     * Create a proxy rule.
     *
     * <pre>// Expression ::= Sum
     *Rule sum = // ...
     *Rule Expression = is(Sum);</pre>
     *
     * @param rule The proxied rule.
     *
     * @return A proxy rule.
     */
    static Proxy is(Rule rule) {
        assert rule != null : "Rule MUST NOT be null";
        return new Proxy(rule);
    }

    /**
     * Create a token based on exclusion.
     *
     * <pre> // STAR ::= '*'
     * Token STAR = is ('*');
     * // NOT_A_STAR ::= ! STAR
     * Token NOT_A_STAR = isNot(STAR);</pre>
     *
     * @param chars The list of characters to exclude,
     *      MUST BE CharToken or MUST WRAP CharToken.
     *
     * @return A token.
     */
    static CharToken isNot(Token... chars) {
        return new CharToken(
            CharRange.ANY.except(
                Arrays.stream(chars)
                .map(c -> CharToken.unwrap(c).charRange)
                .toArray(l -> new CharRange[l])
            )
        );
    }

    /**
     * Turn a grammar in a token.
     *
     * When matching, the token value will be of the
     * type built by the data handler.
     *
     * @param grammar The other grammar.
     * @param dataHandler A data handler for the given grammar.
     *
     * @return The given grammar exposed as a token.
     */
    static Token is(Grammar grammar, Supplier<DataHandler<?>> dataHandler) {
        return new GrammarToken(grammar, dataHandler);
    }

    //    static Token matches(String string) {
//        return new RegexpGrammar.Expr(string);
//    }

    /**
     * Create a range rule.
     *
     * <pre>// DIGIT ::= [0-9]
     *&#x40;Fragment Token DIGIT = range('0', '9').asNumber();</pre>
     *
     * @param start The start of the range
     * @param end The end of the range
     *
     * @return A range rule.
     */
    static CharToken range(int start, int end) {
        return new CharToken(CharRange.range(start, end));
    }

    /**
     * Create a number token.
     *
     * The token value will be available
     * as a number.
     *
     * @see EventsHandler.NumberValue
     *
     * <pre>Token NUMBER = number();</pre>
     *
     * @return A number token.
     */
    static Number number() {
        return new Number();
    }

    /**
     * Create a number token.
     *
     * The token value will be available
     * as a number.
     *
     * @see EventsHandler.NumberValue
     *
     * <pre>Token NUMBER = number(NumberConstraint.BYTE_CONSTRAINT);</pre>
     * (in this example the token value will be a Byte).
     *
     * @param constraint A constraint on the number,
     *      such as limiting the number of total
     *      digits to read, forcing a number to be
     *      of a given type, etc.
     *
     * @return A number token.
     */
    static Number number(NumberConstraint constraint) {
        return new Number(constraint);
    }

    /**
     * Create a proxy rule, useful when a rule reference is expected while its
     * definition will be specified later.
     *
     * @param initializer The function that will initialize
     *      the underlying rule.
     *
     * @return A proxy rule.
     */
    static Proxy $(Supplier<Rule> initializer) {
        return new Proxy.Deferred(initializer);
    }

    /**
     * Parse an input with the main rule of this grammar.
     *
     * @param scanner The input.
     * @param handler The receiver.
     * @param matchAll <code>true</code> to indicates that if
     *      the input contains more characters at the end of
     *      the parsing, an error will be reported, <code>false</code>
     *      otherwise.
     *
     * @return <code>true</code> if the rule was matched,
     *          <code>false</code> otherwise.
     *
     * @throws IOException When the input cause an error.
     */
    default boolean parse(Scanner scanner, EventsHandler handler, boolean matchAll) throws IOException {
        java.util.Optional<Rule> rule = mainRule();
        if (rule.isPresent()) {
            return parse(scanner, handler, rule.get(), matchAll);
        } else {
            return false;
            // TODO : error, no rule found ???
        }
    };

    /**
     * Parse an input with a rule of this grammar.
     *
     * @param scanner The input.
     * @param handler The receiver.
     * @param rule The rule to use for parsing the input.
     * @param matchAll <code>true</code> to indicates that if
     *      the input contains more characters at the end of
     *      the parsing, an error will be reported, <code>false</code>
     *      otherwise.
     *
     * @return <code>true</code> if the rule was matched,
     *          <code>false</code> otherwise.
     *
     * @throws IOException When the input cause an error.
     */
    default boolean parse(Scanner scanner, EventsHandler handler, Rule rule, boolean matchAll) throws IOException {
        Handler h = handler.asHandler();
        // TODO : check if the rule belongs to this Grammar // TODO : Rule adopt(Rule rule) {}
        //      parse with a wrapper if necessary
        //      on which we have to perform init
        Match match = rule.parse(scanner, h);
        // TODO : notification that characters are available
        if (matchAll && scanner.hasNext()) {
//            handler.warning();
        }
        return ! match.fail();
    };

    /**
     * Mark the main rule of a grammar with this annotation.
     *
     * @author Philippe Poulard
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    @interface MainRule { }

    /**
     * Mark a token of a grammar that it is
     * a fragment of a rule token, or a rule
     * that is a fragment of a nested rule.
     * Such annotated token doesn't appear in
     * {@link Grammar#tokenizer()}.
     * Such annotated rule will have its
     * content merged with the content of
     * its nested rule.
     *
     * @author Philippe Poulard
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    @interface Fragment { }

    /**
     * Indicates how to process whitespaces during
     * parsing. When specified on a grammar, the annotation
     * applies on every token not annotated.
     * A non-annotated token of a non-annotated grammar
     * will preserve whitespaces.
     *
     * @author Philippe Poulard
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD })
    @interface WhitespacePolicy {
        /**
         * Indicates whether whitespaces have to be
         * preserved or not. An annotated item will
         * ignore them by default, but a specific token can
         * preserve them while its grammar is annotated
         * to ignore them.
         *
         * @return <code>true</code> to preserve
         * whitespaces, <code>false</code> otherwise.
         */
        boolean preserve() default false;

        /**
         * A predicate that tells whether a character is a
         * whitespace or not.
         *
         * @return The predicate by default is {@link JavaWhitespace}.
         */
        Class<? extends Predicate<Integer>> isWhitespace() default JavaWhitespace.class;
    }

    /**
     * Replace a rule/token field when extending
     * a grammar.
     *
     * A grammar may extend another grammar,
     * a field can override another field of
     * the extended grammar, but when the new
     * field definition has another name, this
     * annotation can indicates which field of
     * the extended grammar have to be replaced.
     *
     * Additionally, when several grammar interfaces
     * are used, the actual grammar with that field
     * must be specified too if the same field
     * is defined several times.
     *
     * @author Philippe Poulard
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    @interface Replace {
        /** @return The field name */
        String field();
        /** Indicates the grammar that hold the field.
         * The default value <code>Grammar.class</code>
         * means unspecified, and a lookup on all the
         * inherited grammars will be performed.
         *
         * @return The grammar.
         */
        Class<? extends Grammar> grammar() default Grammar.class;
    }

    /**
     * Hold the parsing status.
     *
     * @author Philippe Poulard
     */
    enum Match {
        /**
         * When a rule Fails, this Match is Empty
         */
        FAIL {
            @Override
            public boolean fail() {
                return true;
            }
            @Override
            public Match asOptional() {
                return EMPTY;
            }
        },
        /**
         * When a rule is a Success, this Match is NOT Empty
         */
        SUCCESS {
            @Override
            public boolean empty() {
                return false;
            }
        },
        /**
         * Not Failed but Empty is a useful status to avoid infinite
         * loops in repeatable rules, but that don't have to be reported
         * as a failure (since after the last hypothetic match we would
         * get a failure).
         */
        EMPTY {
            @Override
            public Match asMandatory() {
                return FAIL;
            }
        };

        /**
         * When a rule or token didn't match the input.
         *
         * @return <code>true</code> for FAIL,
         *      <code>false</code> for EMPTY or SUCCESS.
         */
        public boolean fail() {
            return false;
        };

        /**
         * An optional rule or token that didn't match the input
         * should be turned to EMPTY.
         *
         * @return <code>true</code> for FAIL or EMPTY,
         *      <code>false</code> for SUCCESS.
         */
        public boolean empty() {
            return true;
        }

        /**
         * Turn this match as an optional match,
         * that is to say an optional won't fail
         * but is empty, which allow repeatable
         * rules to avoid looping.
         *
         * @return SUCCES or EMPTY, never FAIL.
         */
        public Match asOptional() {
            return this;
        }

        /**
         * Turn this match as a mandatory match,
         * that is to say a mandatory is never
         * empty.
         *
         * @return SUCCES or FAIL, never EMPTY.
         */
        public Match asMandatory() {
            return this;
        }

    }

    /**
     * A grammar is composed of rules that
     * can be tokens or so-called rules.
     *
     * When parsing, a rule yield a {@link RuleStart}
     * event and a {@link RuleEnd} event.
     *
     * @see Token
     *
     * @author Philippe Poulard
     */
    abstract class Rule implements Cloneable, Presentable, TraversableRule {

        String name;
        boolean fragment = true;

        /**
         * When a grammar declares a rule or token,
         * it has a name which is the name of the field
         * in that grammar, otherwise it is an anonymous
         * rule or token.
         *
         * @return The proper name of the rule/token,
         *         or a name based on the class name.
         */
        public String getName() {
            if (this.name == null) {
                String derivedName = super.toString();
                try {
                    return derivedName.substring(derivedName.lastIndexOf('.') + 1);
                } catch (IndexOutOfBoundsException e) {
                    return derivedName;
                }
            } else {
                return this.name;
            }
        }

        /**
         * Used internally to set the name of this rule.
         *
         * @param name The grammar field name.
         */
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean isGrammarField() {
            return this.name != null;
        }

        /**
         * Indicates whether this rule was annotated with {@link Fragment}
         * or not, and therefore have to be processed by its nested rule.
         *
         * @return <code>true</code> if this rule/token is a fragment,
         *      <code>false</code> otherwise.
         */
        public boolean isFragment() {
            return this.fragment;
        }

        /**
         * Used internally to set whether this rule is a fragment
         * (annotated with {@link Fragment}).
         *
         * @param isFragment <code>true</code> if this rule/token is
         *      a fragment, <code>false</code> otherwise.
         */
        public void setFragment(boolean isFragment) {
            this.fragment = isFragment;
        }

        @Override
        public String toString() {
            return toStringBuilder(new StringBuilder()).toString();
        }

        /**
         * Append to the given buffer a representation of the rule, that will
         * be its name if it has one, or the pretty representation of this rule.
         *
         * @param buffer The buffer that receive the string representation.
         *
         * @return The same buffer, for chaining.
         */
        public StringBuilder toStringBuilder(StringBuilder buffer) {
            if (this.name == null) {
                return toPrettyString(buffer);
            } else {
                return buffer.append(getName());
            }
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        /**
         * Parse an input with this rule.
         *
         * @param scanner The input.
         * @param handler The receiver.
         *
         * @return Indicates whether this rule matched or not.
         *
         * @throws IOException When the input cause an error.
         */
        public abstract Match parse(Scanner scanner, Handler handler) throws IOException;

        /**
         * Compose this rule with a choice of other rules.
         *
         * @param alt The alternate rules.
         *
         * @return A new rule composed of this rule followed by all the given rules.
         */
        public Rule or(Rule... alt) {
            if (this instanceof Choice) {
                return new Choice(Stream.<Rule> concat(
                    ((Choice) this).getComponent().stream(),
                    Stream.of(alt)
                ));
            } else {
                return new Choice(Stream.<Rule> concat(
                    Stream.of(this),
                    Stream.of(alt)
                ));
            }
        }

        /**
         * Compose this rule with a sequence of other rules.
         *
         * @param sequence The next rules.
         *
         * @return A new rule composed of this rule followed by all the given rules.
         */
        public Rule seq(Rule... sequence) {
            if (this instanceof Sequence) {
                return new Sequence(Stream.<Rule> concat(
                    ((Sequence) this).getComponent().stream(),
                    Stream.of(sequence)
                ));
            } else {
                return new Sequence(Stream.<Rule> concat(
                    Stream.of(this),
                    Stream.of(sequence)
                ));
            }
        }

        /**
         * Make this rule optional.
         *
         * @return An optional rule.
         */
        public Optional optional() {
            return new Optional(this);
        }

        /**
         * Make this rule optional repeatable.
         *
         * @return An optional repeatable rule.
         */
        public ZeroOrMore zeroOrMore() {
            return new ZeroOrMore(this);
        }

        /**
         * Make this rule repeatable.
         *
         * @return A repeatable rule.
         */
        public OneOrMore oneOrMore() {
            return new OneOrMore(this);
        }

        /**
         * Define exceptions for this rule.
         *
         * @param except The list of rules that
         *      are exceptions to this rule.
         * @return An exclusion rule if necessary.
         */
        public Rule except(Rule... except) {
            if (except.length == 0) {
                return this;
            } else {
                return new Except(this, except);
            }
        }

        /**
         * Make this rule a token.
         *
         * @return This rule as a token.
         */
        public TypedToken<String> asToken() {
            return new TypedToken<String>(this) {
                @Override
                public TokenValue<String> newTokenValue(TokensCollector<?> buf, Trackable trackable) {
                    @SuppressWarnings("unchecked")
                    StringBuilder sb = ((TokensCollector<StringBuilder>) buf).get();
                    return new StringValue(this, sb.toString(), trackable);
                }
                @Override
                public TokensCollector<?> collector() {
                    return TokensCollector.newStringBuilderHandler();
                }
            };
        }

        /**
         * Make this rule a token number.
         *
         * @return This rule as a token.
         */
        public TypedToken<java.lang.Number> asNumber() {
            return new TypedToken<java.lang.Number>(this) {
                @Override
                public TokenValue<java.lang.Number> newTokenValue(TokensCollector<?> buf, Trackable trackable) {
                    @SuppressWarnings("unchecked")
                    StringBuilder sb = ((TokensCollector<StringBuilder>) buf).get();
                    java.lang.Number number = NumberUtil.parseNumber(sb.toString());
                    return new NumberValue(this, number, trackable);
                }
                @Override
                public TokensCollector<?> collector() {
                    return TokensCollector.newStringBuilderHandler();
                }
            };
        }

        /**
         * Make this rule a token.
         *
         * @param mapper A function that computes the token value
         *      from the tokens matched by the rule.
         *
         * @return This rule as a token.
         *
         * @param <T> The type of the target data.
         */
        public <T> TypedToken<T> asToken(Function<LinkedList<TokenValue<?>>, T> mapper) {
            return new TypedToken<T>(this) {
                @Override
                public TokenValue<T> newTokenValue(TokensCollector<?> buf, Trackable trackable) {
                    @SuppressWarnings("unchecked")
                    LinkedList<TokenValue<?>> tokens = ((TokensCollector<LinkedList<TokenValue<?>>>) buf).get();
                    T obj = mapper.apply(tokens);
                    return new TokenValue<>(this, obj, trackable);
                }
                @Override
                public TokensCollector<?> collector() {
                    return TokensCollector.newTokenValueHandler();
                }
            };
        }

        /**
         * Make this rule a token.
         *
         * @param mapper A function that computes the token value
         *      from the token matched by the rule :
         *      <code>(aValue, thisRule) -&gt; newValue</code>.
         *
         * @return This rule as a token.
         */
        public TypedToken<String> asToken(BiFunction<String, Rule, String> mapper) {
            return new TypedToken<String>(this) {
                @Override
                public TokenValue<String> newTokenValue(TokensCollector<?> buf, Trackable trackable) {
                    @SuppressWarnings("unchecked")
                    StringBuilder sb = ((TokensCollector<StringBuilder>) buf).get();
                    String s = mapper.apply(sb.toString(), this);
                    return new StringValue(this, s, trackable);
                }
                @Override
                public TokensCollector<?> collector() {
                    return TokensCollector.newStringBuilderHandler();
                }
            };
        }

        /**
         * Skip the tokens produced by this rule.
         *
         * @return A skip
         */
        public Skip skip() {
            return new Skip(this);
        }

    }

    /**
     * A token not transmitted to the event handler while parsing.
     *
     * @see EventsHandler#receive(TokenValue)
     *
     * @author Philippe Poulard
     */
    class Skip extends Token implements TraversableRule.SimpleRule {

        Rule rule;

        /**
         * A skipped rule doesn't forward its event to the handler
         * while parsing.
         *
         * @param rule The rule to skip.
         */
        public Skip(Rule rule) {
            this.rule = rule;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return this.rule.toPrettyString(buf);
        }

        @Override
        public void setComponent(Rule rule) {
            this.rule = rule;
        }

        @Override
        public Rule getComponent() {
            return this.rule;
        }

        @Override
        public boolean parse(Scanner scanner, Handler handler, boolean alreadyMarked) throws IOException {
            TokensCollector<?> collector = TokensCollector.newStringBuilderHandler();
            if (! getComponent().parse(scanner, collector).fail()) {
                return true;
            }
            return false;
        }

    }

    /**
     * A rule skeleton for composing rules.
     *
     * @author Philippe Poulard
     */
    abstract class Wrapper extends Rule implements TraversableRule.SimpleRule {

        /**
         * The wrapped rule.
         */
        protected Rule rule;

        /**
         * Wraps a rule.
         *
         * @param wrappedRule The rule to wraps.
         */
        public Wrapper(Rule wrappedRule) {
            this.rule = wrappedRule;
        }

        /**
         * An empty wrapper that will have a rule later.
         */
        public Wrapper() { }

        @Override
        public void setComponent(Rule rule) {
            this.rule = rule;
        }

        @Override
        public Rule getComponent() {
            return this.rule;
        }

    }

    /**
     * Allow to separate a rule declaration
     * and its assignment.
     *
     * @author Philippe Poulard
     */
    class Proxy extends Wrapper {

        public Proxy(Rule proxiedRule) {
            super(proxiedRule);
        }

        /**
         * Create a proxy rule that need further
         * configuration.
         */
        public Proxy() { }

        @Override
        public Rule getComponent() {
            // ensure we can traverse a proxy name that is just a declaration
            // Rule R1 = $("R2");
            if (super.getComponent() == null) {
                // turn it to Rule R1 = is($("R2"));
                super.rule = new Proxy(this);
            }
            return super.getComponent();
        }

        /**
         * Assign a rule to this proxy.
         *
         * @param proxiedRule The rule to assign.
         *
         * @return A dummy value.
         */
        public boolean is(Rule proxiedRule) {
            this.rule = proxiedRule;
            return true;
        }

        @Override
        public Match parse(Scanner scanner, Handler handler) throws IOException {
            handler.mark();

            handler.receive(new RuleStart(this, scanner));
            Match match = this.rule.parse(scanner, handler);
            handler.receive(new RuleEnd(this, scanner, ! match.fail()));

            handler.commit(! match.fail());
            return match;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return this.rule.toPrettyString(buf);
        }

        /**
         * A proxy defined by an initializer.
         *
         * @author Philippe Poulard
         */
        public static class Deferred extends Proxy implements Initializable {

            Supplier<Rule> initializer;

            /**
             * Create a deferred proxy.
             *
             * @param initializer The initializer that will
             *      supply the rule.
             */
            public Deferred(Supplier<Rule> initializer) {
                this.initializer = initializer;
            }

            @Override
            public <T> T init() {
                if (this.initializer != null) {
                    is(this.initializer.get());
                }
                return null;
            }

        }

        /**
         * A proxy defined by its name.
         *
         * @author Philippe Poulard
         */
        public static class Named extends Proxy {

            String ruleName; // "" stands for the rule $self

            /**
             * Declare a rule with a name.
             *
             * @param ruleName The name of the rule.
             */
            public Named(String ruleName) {
                this.ruleName = ruleName;
            }

            /**
             * Return the proxy name.
             *
             * @return The proxy name.
             */
            public String getProxyName() {
                return this.ruleName;
            }

            @Override
            public StringBuilder toPrettyString(StringBuilder buf) {
                if (this.rule == null) {
                    if ("".equals(this.ruleName)) {
                        return buf.append("$self");
                    } else {
                        return buf.append(this.ruleName);
                    }
                } else {
                    return super.toPrettyString(buf);
                }
            }

        }
    }

    /**
     * Make a rule optional.
     *
     * @author Philippe Poulard
     */
    class Optional extends Wrapper {

        /**
         * Create an optional rule
         *
         * @param optionalRule The rule to make optional.
         */
        Optional(Rule optionalRule) {
            super(optionalRule);
        }

        @Override
        public Match parse(Scanner scanner, Handler handler) throws IOException {
            handler.receive(new RuleStart(this, scanner));
            Match match = rule.parse(scanner, handler);
            handler.receive(new RuleEnd(this, scanner, true));
            return match.asOptional();
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return getComponent().toStringBuilder(buf).append('?');
        }

    }

    /**
     * Make a rule optional repeatable.
     *
     * @author Philippe Poulard
     */
    class ZeroOrMore extends Wrapper {

        /**
         * Create an optional repeatable rule
         *
         * @param repeatableRule The rule to make optional repeatable.
         */
        ZeroOrMore(Rule repeatableRule) {
            super(repeatableRule);
        }

        @Override
        public Match parse(Scanner scanner, Handler handler) throws IOException {
            // never fail, don't need to mark
            handler.receive(new RuleStart(this, scanner));
            Match ruleMatch = Match.EMPTY;
            if (scanner.hasNext()) {
                do {
                    Match match = this.rule.parse(scanner, handler);
                    if (match.empty()) {
                        break;
                    } else {
                        ruleMatch = Match.SUCCESS;
                    }
                } while (scanner.hasNext());
            }
            handler.receive(new RuleEnd(this, scanner, true));
            return ruleMatch;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return getComponent().toStringBuilder( buf).append('*');
        }

    }

    /**
     * Make a rule repeatable.
     *
     * @author Philippe Poulard
     */
    class OneOrMore extends Wrapper {

        /**
         * Create an repeatable rule
         *
         * @param repeatable The rule to make repeatable.
         */
        OneOrMore(Rule repeatable) {
            super(repeatable);
        }

        @Override
        public Match parse(Scanner scanner, Handler handler) throws IOException {
            handler.mark();
            handler.receive(new RuleStart(this, scanner));
            Match ruleMatch = scanner.hasNext() ? rule.parse(scanner, handler) : Match.FAIL;
            if (! ruleMatch.empty() && scanner.hasNext()) {
                do {
                    Match match = rule.parse(scanner, handler);
                    if (match.empty()) {
                        break;
                    } else {
                        ruleMatch = Match.SUCCESS;
                    }
                } while (scanner.hasNext());
            }
            handler.receive(new RuleEnd(this, scanner, ! ruleMatch.fail()));
            handler.commit(! ruleMatch.fail());
            return ruleMatch.asMandatory();
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return getComponent().toStringBuilder( buf ).append('+');
        }

    }

    /**
     * An exclusion rule.
     *
     * @author Philippe Poulard
     */
    class Except extends Combine {
        Rule r;

        Except(Rule r, Rule... except) {
            super(except[0]);
            this.r = r;
            int i = 0;
            for (Rule e: except) {
                if (i++ > 0) {
                    this.combine.add(e);
                }
            }
        }
        //        @Override
        //        public boolean parse(Scanner scanner, HandlerBuffer h) throws IOException {
        //            boolean b = true;
        //            // exceptions first : consider they are less than normal case
        //            scanner.mark(2048);
        //            for (Grammar g: combine) {
        //                if (scanner.hasNext() && g.parse(scanner, Handler.NULL_HANDLER)) {
        //                    b = false;
        //                    break;
        //                };
        //                scanner.cancel();
        //                scanner.mark(2048);
        //            }
        //            scanner.cancel();
        //            if (b) {
        //                h.mark();
        //                scanner.mark(2048);
        //                b = g.parse(scanner, h);
        //                if (b) {
        //                    scanner.consume();
        //                } else {
        //                    scanner.cancel();
        //                }
        //                return h.flush(b);
        //            } else {
        //                return false;
        //            }
        //        }
        @Override
        public Match parse(Scanner scanner, Handler handler) throws IOException {
            Match b = Match.SUCCESS;
            scanner.mark();
            handler.mark();
            handler.receive(new RuleStart(this, scanner));

            b = r.parse(scanner, handler);
            if (! b.fail()) {
//                int n = scanner.getPosition();
                scanner.cancel(); // rewind
                scanner.mark();
//                n = n - scanner.getPosition();
int n = 0; if (true) {throw new TodoException();} // TODO
                StringBuilder data = new StringBuilder(n);
                // capture data
                scanner.nextString(new StringConstraint.ReadLength(n), data);
                Scanner s = new StringScanner(data.toString());
                s.mark();
//                for (Rule g: combine) {
//                    if (s.hasNext() // before parse
//                            && g.parse(s, Handler.NULL_HANDLER.asHandler())
//                            && ! s.hasNext()) { // after parse we expect no remainder
//                        b = false;
//                        break;
//                    };
//                    s.cancel();
//                    s.mark();
//                }
//                s.cancel();
            }
//            scanner.commit(b);
//            handler.receive(new RuleEnd(this, scanner, b));
//            return handler.commit(b);
            return b;
        }

        @Override
        public Rule traverse(Rule hostRule, Set<Rule> traversed,
                BiFunction<Rule, Rule, Rule> transformer, Function<Rule, Rule> targetRule)
        {
            Except target = this;
            if (traversed.add(this)) {
//                hostRule = asHostRule(hostRule);
                target = (Except) super.traverse(hostRule, traversed, transformer, targetRule);
                Rule newRule = transformer.apply(hostRule, this.r)
                        .traverse(hostRule, traversed, transformer, targetRule);
                if (this.r != newRule) {
                    if (target == this) {
                        target = (Except) targetRule.apply(this);
                    }
                    target.r = newRule;
                }
            }
            return target;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            // TODO
            StringBuilder string = new StringBuilder("! (");
            r.toPrettyString(string);
            string.append(")");
            return string;
        }
    }

    /**
     * A rule skeleton for combining rules.
     *
     * @author Philippe Poulard
     */
    abstract class Combine extends Rule implements TraversableRule.CombinedRule {

        List<Rule> combine = new LinkedList<>();

        /**
         * Create a combining rule from a rule.
         *
         * @param rule The actual rule.
         */
        public Combine(Rule rule) {
            this.combine.add(rule);
        }

        /**
         * Combine a bunch of rules
         *
         * @param rules The actual rules.
         */
        public Combine(Stream<? extends Rule> rules) {
            rules.collect(toCollection(() -> this.combine));
        }

        /**
         * The list of combined rules.
         *
         * @return The rules.
         */
        @Override
        public List<Rule> getComponent() {
            return this.combine;
        }

        @Override
        public void setComponent(List<Rule> rules) {
            this.combine = rules;
        }

    }

    /**
     * A choice rule ; when parsing, the first rule
     * that is fulfilled is selected.
     *
     * @author Philippe Poulard
     */
    class Choice extends Combine implements HasWhitespacePolicy {

        java.util.Optional<Predicate<Integer>> whitespacePolicy = java.util.Optional.empty();

        @Override
        public void setWhitespacePolicy(java.util.Optional<Predicate<Integer>> whitespacePolicy) {
            this.whitespacePolicy = whitespacePolicy;
        }

        @Override
        public java.util.Optional<Predicate<Integer>> getWhitespacePolicy() {
            return this.whitespacePolicy;
        }

        /**
         * Create a choice rule.
         *
         * @param alt An alternate rule.
         */
        public Choice(Rule alt) {
            super(alt);
        }

        /**
         * Create a choice rule.
         *
         * @param rules The alternate rules.
         */
        public Choice(Stream<? extends Rule> rules) {
            super(rules);
        }

        @Override
        public Match parse(Scanner scanner, Handler handler) throws IOException {
            handler.mark();
            handler.receive(new RuleStart(this, scanner));
            applyWhitespacePolicyBefore(scanner);

            scanner.mark();
            for (Rule rule: combine) {
                if (scanner.hasNext()) {
                    Match match = rule.parse(scanner, handler);
                    if (! match.empty()) {
                        applyWhitespacePolicyAfter(scanner);
                        scanner.consume();
                        handler.receive(new RuleEnd(this, scanner, true));
                        handler.commit(true);
                        return match;
                    }
                }
            }
            scanner.cancel();
            handler.receive(new RuleEnd(this, scanner, false));
            handler.commit(false);
            return Match.FAIL;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return getComponent().stream()
                .collect( StringBuilderUtil.collectorOf(
                    "( ", " | ", " )", buf,
                    rule -> rule.toStringBuilder(buf))
                );
        }

    }

    /**
     * A sequence rule.
     *
     * @author Philippe Poulard
     */
    class Sequence extends Combine {

        /**
         * Create a sequence rule.
         *
         * @param sequence The rule in the sequence.
         */
        Sequence(Rule sequence) {
            super(sequence);
        }

        /**
         * Create a sequence rule.
         *
         * @param rules The rules in the sequence.
         */
        Sequence(Stream<? extends Rule> rules) {
            super(rules);
        }

        @Override
        public Match parse(Scanner scanner, Handler handler) throws IOException {
            handler.mark();
            handler.receive(new RuleStart(this, scanner));
            scanner.mark();
            Match ruleMatch = Match.EMPTY;
            for (Rule r: combine) {
                Match match = r.parse(scanner, handler);
                if (match.fail()) {
                    ruleMatch = Match.FAIL;
                    break;
                } else if (! match.empty()) {
                    ruleMatch = Match.SUCCESS;
                }
            }
            scanner.commit(! ruleMatch.fail());
            handler.receive(new RuleEnd(this, scanner, ! ruleMatch.fail()));
            handler.commit(! ruleMatch.fail());
            return ruleMatch;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return getComponent().stream()
                .collect( StringBuilderUtil.collectorOf(
                    "( ", " ", " )", buf,
                    rule -> rule.toStringBuilder(buf))
                );
        }

    }

    /**
     * When parsing, a token yield a {@link TokenValue}.
     *
     * @author Philippe Poulard
     */
    abstract class Token extends Rule implements HasWhitespacePolicy {

        public Token() {
            this.fragment = false;
        }

        java.util.Optional<Predicate<Integer>> whitespacePolicy = java.util.Optional.empty();

        @Override
        public void setWhitespacePolicy(java.util.Optional<Predicate<Integer>> whitespacePolicy) {
            this.whitespacePolicy = whitespacePolicy;
        }

        @Override
        public java.util.Optional<Predicate<Integer>>  getWhitespacePolicy() {
            return this.whitespacePolicy;
        }

        @Override
        public final Match parse(Scanner scanner, Handler handler) throws IOException {
            boolean alreadyMarked = applyWhitespacePolicyBefore(scanner);
            boolean parsed = parse(scanner, handler, alreadyMarked);
            if (alreadyMarked) {
                scanner.commit(parsed);
            }
            if (parsed) {
                applyWhitespacePolicyAfter(scanner);
            }
            return parsed ? Match.SUCCESS : Match.FAIL;
        }

        /**
         * Parse the input with this token.
         *
         * @param scanner Hold the input
         * @param handler Receive the parse event(s)
         * @param alreadyMarked Indicates whether the input was already marked
         *      (may occur when applying the whitespace policy)
         *
         * @return <code>true</code> if this token was matched,
         *          <code>false</code> otherwise.
         *
         * @throws IOException When an I/O exception occur.
         */
        public abstract boolean parse(Scanner scanner, Handler handler, boolean alreadyMarked) throws IOException;

        /**
         * Return the underlying char token if any ; useful for
         * combining it.
         *
         * @return This token if it is a char token,
         *      or the wrapped token if it is a char token.
         *
         * @throws IllegalArgumentException It this token is
         *      not a char token or doesn't wrap a char token.
         *
         * @see CharToken#union(CharRange...)
         * @see CharToken#union(Token...)
         * @see CharToken#except(CharRange...)
         * @see CharToken#except(Token...)
         * @see TraversableRule.SimpleRule
         * @see CharToken#unwrap(Token)
         */
        public CharToken unwrap() {
            return CharToken.unwrap(this);
        }

        @Override
        public Rule traverse(Rule hostRule, Set<Rule> traversed,
                BiFunction<Rule, Rule, Rule> transformer, Function<Rule, Rule> targetRule)
        {
            return this;
        }

    }

    /**
     * A character token, based on compositions of character ranges.
     *
     * @see Grammar#is(int)
     * @see Grammar#isOneOf(String)
     * @see Grammar#range(int, int)
     * @see Grammar#isNot(int)
     * @see Grammar#isNot(Token...)
     * @see Grammar#isNotOneOf(String)
     *
     * @author Philippe Poulard
     */
    class CharToken extends Token {

        CharRange charRange;

        /**
         * Create a char token based on a char range.
         *
         * @param charRange The underlying char range.
         */
        public CharToken(CharRange charRange) {
            this.charRange = charRange;
        }

        /**
         * Return the char token wrapped in the given token, if any.
         *
         * @param token The token to examine, may be or may wrap a char token.
         *
         * @return The char token if any.
         *
         * @see TraversableRule.SimpleRule
         */
        public static java.util.Optional<CharToken> unwrapSafely(Token token) {
            while (! (token instanceof CharToken) && token instanceof TraversableRule.SimpleRule) {
                token = (Token) ((TraversableRule.SimpleRule) token).getComponent();
            }
            if (token instanceof CharToken) {
                return java.util.Optional.of((CharToken) token);
            } else {
                return java.util.Optional.empty();
            }
        }

        /**
         * Return the char token wrapped in the given token, if any.
         *
         * @param token The token to examine, must be or must wrap a char token.
         *
         * @return The char token if any.
         *
         * @throws IllegalArgumentException If the token is not a char token and
         *      doesn't wrap a char token.
         *
         * @see TraversableRule.SimpleRule
         */
        public static CharToken unwrap(Token token) {
            return unwrapSafely(token).orElseThrow(
                () -> new IllegalArgumentException("The token argument MUST BE CharToken"
                    + " or MUST WRAP CharToken, but " + token + " is not ; it is "
                    + token.getClass()));
        };

        /**
         * Combine this char token with the given Unicode codepoint.
         *
         * @param codepoint The Unicode codepoint.
         *
         * @return A char token made of this char token union the Unicode codepoint.
         */
        public CharToken union(int codepoint) {
            CharRange cr = this.charRange.union(CharRange.is(codepoint));
            return cr == this.charRange ? this : new CharToken(cr);
        }

        /**
         * Combine this char token with the given range.
         *
         * @param startCodepoint The start Unicode codepoint, included.
         * @param endCodepoint The end Unicode codepoint, included.
         *
         * @return A char token made of this char token union the given range.
         */
        public CharToken union(int startCodepoint, int endCodepoint) {
            CharRange cr = this.charRange.union(CharRange.range(startCodepoint, endCodepoint));
            return cr == this.charRange ? this : new CharToken(cr);
        }

        /**
         * Combine this char token with the given characters.
         *
         * @param characters The characters to add.
         *
         * @return A char token made of this char token union all the characters given.
         */
        public CharToken union(CharSequence characters) {
            CharRange cr = this.charRange.union(CharRange.isOneOf(characters));
            return cr == this.charRange ? this : new CharToken(cr);
        }

        /**
         * Combine this char token with the given char ranges.
         *
         * @param charRanges The char ranges to add.
         *
         * @return A char token made of this char token union the given char ranges.
         */
        public CharToken union(CharRange... charRanges) {
            CharRange cr = this.charRange.union(charRanges);
            return cr == this.charRange ? this : new CharToken(cr);
        }

        /**
         * Combine this char token with the given char tokens.
         *
         * @param charTokens The char ranges to combine, must be
         *      CharToken or wrap a CharToken.
         *
         * @return A char token made of this char token union the given char tokens.
         */
        public CharToken union(Token... charTokens) {
            CharRange cr = this.charRange.union(
                Arrays.stream(charTokens)
                    .map(c -> CharToken.unwrap(c).charRange)
                    .toArray(l -> new CharRange[l]));
            return cr == this.charRange ? this : new CharToken(cr);
        }

        @Override
        public Rule or(Rule... alt) {
            if (Arrays.stream(alt).filter(r -> ! (r instanceof CharToken)).findFirst().isPresent()) {
                // if we find one that is not a char token => process it normally
                return super.or(alt);
            } else {
                // all are char token => merge with union
                CharToken[] ct = Arrays.stream(alt).map(r -> (Token) r).toArray(l -> new CharToken[l]);
                return union(ct);
            }
        }

        /**
         * Remove from this char token the given Unicode codepoint.
         *
         * @param codepoint The Unicode codepoint to remove.
         *
         * @return A char token made of this char token minus the Unicode codepoint.
         */
        public CharToken except(int codepoint) {
            CharRange cr = this.charRange.except(CharRange.is(codepoint));
            return cr == this.charRange ? this : new CharToken(cr);
        }

        /**
         * Remove from this char token the given range.
         *
         * @param startCodepoint The start Unicode codepoint of the range to remove, included.
         * @param endCodepoint The end Unicode codepoint of the range to remove, included.
         *
         * @return A char token made of this char token minus the given range.
         */
        public CharToken except(int startCodepoint, int endCodepoint) {
            CharRange cr = this.charRange.except(CharRange.range(startCodepoint, endCodepoint));
            return cr == this.charRange ? this : new CharToken(cr);
        }

        /**
         * Remove from this char token the given characters.
         *
         * @param characters The characters to remove.
         *
         * @return A char token made of this char token minus all the characters given.
         */
        public CharToken except(CharSequence characters) {
            CharRange cr = this.charRange.except(CharRange.isOneOf(characters));
            return cr == this.charRange ? this : new CharToken(cr);
        }

        /**
         * Remove from this char token the given char ranges.
         *
         * @param charRanges The char ranges to remove.
         *
         * @return A char token made of this char token minus the char ranges given.
         */
        public CharToken except(CharRange... charRanges) {
            CharRange cr = this.charRange.except(charRanges);
            return cr == this.charRange ? this : new CharToken(cr);
        }

        /**
         * Remove from this char token the given char tokens.
         *
         * @param charTokens The char tokens to combine, must be
         *      CharToken or wrap a CharToken.
         *
         * @return A char token made of this char token minus the given char tokens.
         */
        public CharToken except(Token... charTokens) {
            CharRange cr = this.charRange.except(
                Arrays.stream(charTokens)
                    .map(c -> CharToken.unwrap(c).charRange)
                    .toArray(l -> new CharRange[l]));
            return cr == this.charRange ? this : new CharToken(cr);
        }

        @Override
        public boolean parse(Scanner scanner, Handler handler, boolean alreadyMarked) throws IOException {
            java.util.Optional<String> car = scanner.nextChar(this.charRange)
                    .map(c -> new String(Character.toChars(c)));
            if (car.isPresent()) {
                handler.receive(new StringValue(this, car.get(), scanner));
                return true;
            } else {
                return false;
            }
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return this.charRange.toPrettyString(buf);
        }
    }

    /**
     * A string token.
     *
     * @see Grammar#is(String)
     *
     * @author Philippe Poulard
     */
    class StringToken extends Token {

        String string;
        boolean equal;

        StringToken(String string, boolean equal) {
            this.string = string;
            this.equal = equal;
        }

        @Override
        public boolean parse(Scanner scanner, Handler handler, boolean alreadyMarked) throws IOException {
            if (! (equal ^ scanner.hasNextString(string, true))) {
                // match
                if (equal) {
                    handler.receive(new StringValue(this, string, scanner));
                } else {
                    StringBuilder buf = new StringBuilder(string.length());
                    scanner.nextString(new StringConstraint.ReadLength(string.length()), buf);
                    handler.receive(new StringValue(this, buf.toString(), scanner));
                }
                return true;
            }
            return false;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            if (! this.equal) {
                buf.append("!");
            }
            return buf.append('\'').append(this.string).append('\'');
        }

    }

    /**
     * A token made from an enum class or a list of string values.
     *
     * @see Grammar#is(Class)
     * @see Grammar#is(String...)
     *
     * @author Philippe Poulard
     *
     * @param <T> The type of values : <code>String</code> or an enum class.
     *
     * @see EnumValues
     */
    class EnumToken<T> extends Token {

        // hierarchy of enum string values by char
        EnumValues<T> values;

        @SuppressWarnings("unchecked")
        public EnumToken(@SuppressWarnings("rawtypes") Class<? extends Enum> values) {
            this.values = (EnumValues<T>) EnumValues.from(values);
        }

        @SuppressWarnings("unchecked")
        public EnumToken(String... values) {
            this.values = (EnumValues<T>) EnumValues.from(values);
        }

        @Override
        public boolean parse(Scanner scanner, Handler handler, boolean alreadyMarked) throws IOException {
            java.util.Optional<T> value = this.values.nextValue(scanner);
            value.ifPresent(
                e -> handler.receive(new TokenValue<>(this, e, scanner))
            );
            return value.isPresent();
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return this.values.toPrettyString(buf);
        }

    }

    /**
     * An enum value token.
     *
     * @see Grammar#is(Enum)
     *
     * @author Philippe Poulard
     */
    class EnumValueToken extends Token {

        Enum<?> value;

        EnumValueToken(Enum<?> value) {
            this.value = value;
        }

        @Override
        public boolean parse(Scanner scanner, Handler handler, boolean alreadyMarked) throws IOException {
            if (scanner.hasNextString(value.name(), true)) {
                // match
                handler.receive(new TokenValue<>(this, value, scanner));
                return true;
            }
            return false;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return buf.append('\'').append(this.value.toString()).append('\'');
        }
    }

    /**
     * A number token.
     *
     * @see Grammar#number()
     *
     * @author Philippe Poulard
     */
    class Number extends Token {

        NumberConstraint constraint;

        Number(NumberConstraint constraint) {
            this.constraint = constraint;
        }

        Number() { }

        @Override
        public boolean parse(Scanner scanner, Handler handler, boolean alreadyMarked) throws IOException {
            if (! alreadyMarked) {
                scanner.mark();
            }
            java.lang.Number number = this.constraint == null
                  ?  scanner.nextNumber()
                  : scanner.nextNumber(this.constraint);
            if (number == null || number instanceof BigInteger || number instanceof BigDecimal) {
                if (! alreadyMarked) {
                    scanner.cancel();
                }
                return false;
            } else {
                if (! alreadyMarked) {
                    scanner.consume();
                }
                handler.receive(new NumberValue(this, number, scanner));
                return true;
            }
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            if (this.constraint != null && this.constraint.getNumberType() != null) {
                return buf.append('#')
                    .append(this.constraint.getNumberType().getName());
            } else {
                return buf.append("#NUMBER");
            }
        }

    }

    /**
     * A token based on a grammar.
     *
     * @author Philippe Poulard
     */
    class GrammarToken extends Token {

        Grammar grammar;
        Supplier<DataHandler<?>> dataHandlerSupplier;

        GrammarToken(Grammar grammar, Supplier<DataHandler<?>> dataHandlerSupplier) {
            this.grammar = grammar;
            this.dataHandlerSupplier = dataHandlerSupplier;
        }

        @Override
        public boolean parse(Scanner scanner, Handler handler, boolean alreadyMarked) throws IOException {
            DataHandler<?> dataHandler = this.dataHandlerSupplier.get();
            boolean parsed = this.grammar.parse(scanner, dataHandler, false);
            if (parsed) {
                Object value = dataHandler.get();
                TokenValue<?> tokenValue = new TokenValue<>(this, value, scanner);
                handler.receive(tokenValue);
            }
            return parsed;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return buf.append('#').append(this.grammar.getClass().getInterfaces()[0].getName());
        }

    }

    /**
     * A token based on rules.
     *
     * @author Philippe Poulard
     *
     * @param <T> The type of the token value.
     */
    abstract class TypedToken<T> extends Token implements TraversableRule.SimpleRule {

        Rule rule;

        /**
         *
         * @param rule The rule from which this token is built.
         */
        public TypedToken(Rule rule) {
            this.rule = rule;
        }

        @Override
        public Rule getComponent() {
            return this.rule;
        }

        @Override
        public void setComponent(Rule rule) {
            this.rule = rule;
        }

        /**
         * Create a token value.
         *
         * @param buf Contains the tokens collected by the rule.
         * @param trackable Allow to determine the position of the data in the input.
         *
         * @return The token value.
         */
        public abstract TokenValue<T> newTokenValue(TokensCollector<?> buf, Trackable trackable);

        /**
         * Create the handler that collect all tokens within.
         *
         * @return The handler for collecting values.
         */
        public abstract TokensCollector<?> collector();

        @Override
        public boolean parse(Scanner scanner, Handler handler, boolean alreadyMarked) throws IOException {
            TokensCollector<?> collector = collector();
            if (! this.rule.parse(scanner, collector).fail() && ! collector.isEmpty()) {
                handler.receive(newTokenValue(collector, scanner));
                return true;
            } else {
                return false;
            }
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return this.rule.toStringBuilder(buf);
        }

    }

    /**
     * Return a reference to the given rule.
     *
     * @param rule The name of the referred rule,
     *  MUST NOT be empty.
     *
     * @return A proxy rule.
     */
    static Rule $(String rule) {
        assert rule.length() > 0;
        return new Proxy.Named(rule);
    }

    /**
     * <code>$self</code> is a reference to
     * the current rule definition.
     *
     * Used when a rule definition refers to itself :
     *
     * <pre>// Argument ::= FUNCTION Argument | Value | '(' Expression ')'
     *&#x40;Fragment Rule Argument = ( FUNCTION.seq($self) ).or(Value).or( LBRACKET.seq( Expression, RBRACKET ) );</pre>
     */
    Rule $self = new Proxy.Named("");

    /**
     * The char token for any Unicode codepoint.
     */
    CharToken $any = new CharToken(CharRange.ANY);

    /**
     * The char token that matches nothing.
     */
    CharToken $empty = new CharToken(CharRange.EMPTY);

    /**
     * Return the singleton instance of the given grammar.
     *
     * @param grammar The grammar must be an interface.
     * @param <T> The target grammar interface.
     *
     * @return An instance of the given interface,
     *      generated without methods implementation
     *      that would be specified in the interface.
     *
     * @see ByteCodeFactory#getInstance(Class)
     */
    static <T extends Grammar> T $(Class<? extends Grammar> grammar) {
        return Grammar$.$(grammar);
    }

    /**
     * Create either the singleton instance of the current Grammar
     * or a new proxy Rule, according to how this method is invoked.
     *
     * <p>In the following sample code, the method <code>$()</code>
     * is called twice, once on a rule field, then on the grammar
     * singleton field :</p>
     *
     * <pre>import static ml.alternet.parser.Grammar.*;
     *import ml.alternet.parser.Grammar;
     *
     *public interface Foo extends Grammar {
     *
     *    // create a proxy rule that will be initialized later
     *    Rule MyRule = $();
     *
     *    // create the grammar instance to use for parsing an input
     *    Foo $ = $(); // must be the last field
     *
     *}</pre>
     *
     * <p>In both cases, the caller of this method must be the
     * actual grammar interface, that is to say you MUST NOT
     * invoke that method outside for initializing a Rule or
     * the Grammar instance elsewhere than in your grammar
     * interface.</p>
     *
     * <h1>Grammar field</h1>
     *
     * <p><code>$()</code> return the singleton instance of the
     * current grammar.</p>
     *
     * <p>NOTE : this method MUST BE called after all fields of
     * the interface have been properly initialized ; typically
     * this method is used to set the LAST field of the grammar
     * by getting its instance.</p>
     *
     * <h1>Rule field</h1>
     *
     * <p><code>$()</code> return a new proxy rule, useful when a
     * rule reference is expected while its definition will be
     * specified later. It is useful when a rule is made of other
     * rules/tokens that are defined later in the grammar.</p>
     *
     * <p>NOTE : all Rule fields in a grammar MUST NOT be null.</p>
     *
     * <p>When such proxy rule has been declared as a field in a
     * grammar, it have to be initialized later in the grammar :</p>
     * <ul>
     *  <li>Either by declaring a STATIC method with the same name as
     *      the field :
     *      <pre>    static Rule MyRule() {
     *        return SomeRule.seq(OtherRule);
     *    }</pre></li>
     *  <li>Or by declaring a supplier field with the same name as
     *      the field prepend with "$" :
     *      <pre>    Supplier&lt;Rule&gt; $MyRule = () -&gt; SomeRule.seq(OtherRule);</pre></li>
     *  <li>Or by declaring a dummy field that perform the initialization :
     *      <pre>    boolean b1 = ((Proxy) MyRule).is(
     *        SomeRule.seq(OtherRule)
     *    );</pre></li>
     * </ul>
     *
     * @param <T> The target grammar interface OR a Rule.
     *
     * @return For the grammar field, return an instance of
     *      the given interface, generated without methods
     *      implementation that would be specified in the
     *      interface ; for a rule field, return a new proxy
     *      rule.
     *
     * @see ByteCodeFactory#getInstance(Class)
     *
     * @throws ClassCastException If the grammar interface defines
     *      a rule field left <code>null</code>.
     */
    static <T> T $() {
        return Grammar$.$();
    }

    /**
     * Return the "tokenizer rule", that is to
     * say all the non-fragment tokens explicitly
     * specified by this grammar, in their natural
     * order.
     *
     * @return A rule with all the tokens that are not fragments :
     *      the rule <code>(T1 | T2 | T3 ...)*</code>
     *
     * @see Fragment
     */
    Rule tokenizer();

    /**
     * Return the main rule of the grammar.
     *
     * @return The main rule if one has been set
     *      on the grammar or one of its interfaces.
     *
     * @see MainRule
     */
    java.util.Optional<Rule> mainRule();

}
