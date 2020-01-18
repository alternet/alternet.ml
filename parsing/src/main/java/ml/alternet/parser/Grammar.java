package ml.alternet.parser;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ml.alternet.parser.visit.TraversableRule;
import ml.alternet.facet.Initializable;
import ml.alternet.facet.Presentable;
import ml.alternet.facet.Trackable;
import ml.alternet.facet.Unwrappable;
import ml.alternet.misc.CharRange;
import ml.alternet.misc.Thrower;
import ml.alternet.misc.Type;
import ml.alternet.parser.EventsHandler.NumberValue;
import ml.alternet.parser.EventsHandler.RuleEnd;
import ml.alternet.parser.EventsHandler.RuleStart;
import ml.alternet.parser.EventsHandler.StringValue;
import ml.alternet.parser.EventsHandler.TokenValue;
import ml.alternet.parser.ast.NodeBuilder;
import ml.alternet.parser.ast.ValueBuilder;
import ml.alternet.parser.handlers.DataHandler;
import ml.alternet.parser.handlers.TokensCollector;
import ml.alternet.parser.util.ComposedRule;
import ml.alternet.parser.util.Grammar$;
import ml.alternet.parser.util.Parser;
import ml.alternet.parser.util.Parser.Match;
import ml.alternet.scan.EnumValues;
import ml.alternet.scan.NumberConstraint;
import ml.alternet.scan.Scanner;
import ml.alternet.util.ByteCodeFactory;
import ml.alternet.util.NumberUtil;
import ml.alternet.util.StringBuilderUtil;

/**
 * Base interface for defining grammars.
 *
 * @see <a href="http://alternet.github.io/alternet-libs/parsing/parsing.html">http://alternet.github.io/alternet-libs/parsing/parsing.html</a>
 *
 * <h1>Skeleton</h1>
 *
 * <pre>import static ml.alternet.parser.Grammar.*;
 *import ml.alternet.parser.Grammar;
 *
 *&#x40;Skip(token="WS")
 *public interface NumbersGrammar extends Grammar {
 *
 *    &#x40;Fragment
 *    CharToken WS = isOneOf(" \t\n\r");
 *
 *    // DIGIT ::= [0-9]
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
 * A grammar is composed of rules and tokens, that may themselves be composed of rules and tokens ;
 * the rules and tokens that are fields in the grammar are called "grammar field" and have a name,
 * the others doesn't have a name.
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
     * @param car The actual Unicode codepoint.
     *
     * @return A char token.
     */
    static CharToken is(int car) {
        return new CharToken.Single(CharRange.is(car));
    }

    /**
     * Create a char token based on exclusion.
     *
     * <pre>// NOT_A_STAR ::= ! '*'
     *Token NOT_A_STAR = isNot('*');</pre>
     *
     * @param car The Unicode codepoint to exclude.
     *
     * @return A char token.
     */
    static CharToken isNot(int car) {
        return new CharToken.Single(CharRange.isNot(car));
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
        return new CharToken.Single(CharRange.isOneOf(string));
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
        return new CharToken.Single(CharRange.isNotOneOf(string));
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
     * Unlike other tokens, no token value is produced :
     * this definition just ensure that the next input
     * is not the one given ; this definition can be
     * consider only as a constraint to fulfill.
     *
     * @param string The string to exclude.
     *
     * @return A string token that won't supply any value
     *      if the condition succeeds.
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
    static EnumToken<? extends Enum> is(Class<? extends Enum> values) {
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
        return $any.except(chars);
    }

    /**
     * Turn a grammar to a token.
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
        return new CharToken.Single(CharRange.range(start, end));
    }

    /**
     * Create a number token.
     *
     * The token value will be available
     * as a number.
     *
     * @see NumberValue
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
     * @see NumberValue
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
    boolean parse(Scanner scanner, EventsHandler handler, Rule rule, boolean matchAll) throws IOException;

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
     *
     * Such annotated rule will have its
     * content merged with the content of
     * its nested rule.
     *
     * A fragment is by default not affected
     * by skipped characters when specified
     * at the grammar level, but will be
     * affected by a specific {@link Skip}
     * annotation on it.
     *
     * @author Philippe Poulard
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    @interface Fragment { }

    /**
     * The token value produced by the annotated token
     * won't be transmitted to the event handler while parsing.
     *
     * @author Philippe Poulard
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    @interface Drop { }

    /**
     * Indicates the characters to skip (typically, some
     * whitespaces) before and after parsing a token.
     *
     * When set on a grammar, the annotation is inherited
     * on every field that is not a {@link Fragment} and that doesn't
     * override this annotation. Anonymous subrules or subrules
     * from other grammars are not affected.
     *
     * @author Philippe Poulard
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE })
    @interface Skip {

        /**
         * The name of the token field that define the
         * characters to skip. May be {@code $empty}
         * to override the grammar {@code Skip}
         * annotation for disable skipping.
         *
         * The field that define the characters to
         * skip is defined in the same grammar of the
         * annotated field, except if the {@code grammar}
         * attribute is set.
         *
         * If the annotation is set on a grammar and
         * that the target token is in the same grammar,
         * the token is not affected by the {@code Skip}
         * annotation set on the grammar and must not
         * have it on itself.
         *
         * The target token may be arbitrary complex
         * as long as it is exposed as a token. None of
         * the subrules can be annotated with {@code Skip}.
         *
         * @return The name of the token field in the grammar.
         *
         * @see CharToken
         */
        String token();

        /**
         * Indicates the grammar that host the char token.
         * When missing, the default value <code>Grammar.class</code>
         * means that the grammar is the same grammar of
         * the annotated field.
         *
         * @return The grammar.
         */
        Class<? extends Grammar> grammar() default Grammar.class;

        /**
         * Skip the characters before the token.
         *
         * @return {@code true} by default.
         */
        boolean before() default true;

        /**
         * Skip the characters after the token.
         *
         * @return {@code true} by default.
         */
        boolean after() default true;

    }

    /**
     * Replace a rule/token field when extending
     * a grammar. The replacement is made on the
     * new grammar, the original grammar is unchanged.
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

        /**
         * The name of the field to replace.
         *
         * @return The field name, when missing the field to replace
         *      has the same name of the annotated field  */
        String field() default "";

        /**
         * Indicates the grammar that hold the field to replace.
         * The default value <code>Grammar.class</code>
         * means <b>unspecified</b>, and a lookup on all the
         * inherited grammars will be performed.
         *
         * @return The grammar.
         */
        Class<? extends Grammar> grammar() default Grammar.class;

        /**
         * When <code>true</code>, prevent substition of an existing
         * element by the annotated element
         *
         * @return <code>false</code> by default : substitution occurs.
         */
        boolean disable() default false;
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

        private static final AtomicInteger IDS = new AtomicInteger();

        int id = IDS.incrementAndGet(); // shared by rule clones

        String name;
        boolean fragment = true; // by default false for Token

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
         * or not, and therefore have to be processed by its enclosed rule.
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
         */
        @SuppressWarnings("unchecked")
        public final Match parse(Scanner scanner, Handler handler) {
            return this.parser.parse(this, scanner, handler);
        };

        /**
         * The active part of the rule when it is applied on the input to parse.
         */
        @SuppressWarnings("rawtypes")
        public Parser parser;

        /**
         * Return the identifier of this rule, should be used for
         * logically comparing 2 rules : rules may be cloned when
         * extending a grammar, but they will share the same ID.
         *
         * @return The identifier of this rule.
         */
        public int id() {
            return this.id;
        }

        /**
         * Compose this rule with a <b>choice</b> of other rules.
         *
         * @param alt The alternate rules.
         *
         * @return A new rule composed of this rule followed by all the given rules.
         */
        public Rule or(Rule... alt) {
            // don't flatten here : flattening may be set on UNNAMED rules,
            // after Grammar$.processFields() & processPlaceHolders()
            return new Choice(Stream.<Rule> concat(
                Stream.of(this),
                Stream.of(alt)
            ));
        }

        /**
         * Compose this rule with a <b>sequence</b> of other rules.
         *
         * @param sequence The next rules.
         *
         * @return A new rule composed of this rule followed by all the given rules.
         */
        public Rule seq(Rule... sequence) {
            // don't flatten here : flattening may be set on UNNAMED rules,
            // after Grammar$.processFields() & processPlaceHolders()
            return new Sequence(Stream.<Rule> concat(
                Stream.of(this),
                Stream.of(sequence)
            ));
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
        public AtLeast oneOrMore() {
            return new AtLeast(this, 1);
        }

        /**
         * Make this rule repeatable.
         *
         * @param min The mininum number of times that rule has to be repeated.
         * @return A repeatable rule.
         */
        public Rule atLeast(int min) {
            if (min == 0) {
                return new ZeroOrMore(this);
            } else {
                return new AtLeast(this, min);
            }
        }

        /**
         * Make this rule repeatable.
         *
         * @param max The maximum number of times that rule has to be repeated.
         * @return A repeatable rule.
         */
        public Rule atMost(int max) {
            if (max == 1) {
                return new Optional(this);
            } else {
                return new AtMost(this, max);
            }
        }

        /**
         * Make this rule repeatable.
         *
         * @param min The mininum number of times that rule has to be repeated.
         * @param max The maximum number of times that rule has to be repeated.
         * @return A repeatable rule.
         */
        public Rule bounds(int min, int max) {
            assert min <= max : "min must be <= max";
            assert min >= 0 : "min must be positive or nul";
            assert max >= 1 : "min must be positive";
            if (min == 1) {
                if (max == 1) {
                    return this;
                }
            } else if (max == 1 && min == 0) {
                return new Optional(this);
            }
            return new Bounds(this, min, max);
        }

        /**
         * Make this rule a token.
         *
         * @return This rule as a token.
         */
        public TypedToken<String> asToken() {
            return new TypedToken.String(this);
        }

        /**
         * Make this rule a token number.
         *
         * @return This rule as a token.
         */
        public TypedToken<java.lang.Number> asNumber() {
            return new TypedToken.Number(this);
        }

        /**
         * Make this rule a token number, the token value being of the type
         * expected.
         *
         * @param numberClass The expected number type.
         * @param <T> The type of the token value.
         *
         * @return This rule as a token.
         */
        public <T extends java.lang.Number> TypedToken<T> asNumber(Class<T> numberClass) {
            return new TypedToken.TypedNumber<T>(this, numberClass);
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
            return new TypedToken.Mapper<T>(this, mapper);
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
            return new TypedToken.String(this, sb -> mapper.apply(sb.toString(), this));
        }

        /**
         * Drop the tokens produced by this rule ;
         * grammar fields are modified, and other
         * rules will be wrapped.
         *
         * @return A drop element.
         *
         * @param <T> The type of the dropped rule.
         *
         * @see DropToken
         */
        @SuppressWarnings("unchecked")
        public <T extends Rule> T drop() {
            if (isGrammarField()) {
                @SuppressWarnings("rawtypes")
                Parser p = this.parser;
                this.parser = new Parser<Rule>() {
                    @Override
                    public Match parse(Rule rule, Scanner scanner, Handler handler) {
                        return p.parse(rule, scanner, Handler.NULL_HANDLER);
                    }
                };
                return (T) this;
            } else {
                return (T) new DropToken(this);
            }
        }

    }

    /**
     * A token not transmitted to the event handler while parsing.
     *
     * @see EventsHandler#receive(TokenValue)
     *
     * @author Philippe Poulard
     */
    class DropToken extends Token implements TraversableRule.SimpleRule, Unwrappable<Rule> {

        Rule rule;

        /**
         * A dropped rule doesn't forward its event to the handler
         * while parsing.
         *
         * @param rule The rule that will have its matched tokens dropped.
         */
        public DropToken(Rule rule) {
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
            if (! getComponent().parse(scanner, Handler.NULL_HANDLER).fail()) {
                return true;
            }
            return false;
        }

        @Override
        public Rule unwrap() {
            return this.rule;
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
    class Proxy extends Wrapper implements Unwrappable<Rule> {

        /**
         * Create a proxy rule from a rule.
         *
         * @param proxiedRule The proxied rule.
         */
        public Proxy(Rule proxiedRule) {
            super(proxiedRule);
            setParser();
            this.id = proxiedRule.id;
        }

        /**
         * Create a proxy rule that need further
         * configuration.
         */
        public Proxy() {
            setParser();
        }

        void setParser() {
            this.parser = (Parser<Proxy>) (proxy, scanner, handler) -> {
                handler.mark();

                handler.receive(new RuleStart(proxy, scanner));
                Match match = proxy.getComponent().parse(scanner, handler);
                handler.receive(new RuleEnd(proxy, scanner, ! match.fail()));

                handler.commit(! match.fail());
                return match;
            };
        }

        @Override
        public void setComponent(Rule rule) {
            super.setComponent(rule);
            this.id = rule.id;
        }

        @Override
        public Rule getComponent() {
            // ensure we can traverse a proxy name that is just a declaration
            // Rule R1 = $("R2");
            if (super.getComponent() == null) {
                // turn it to Rule R1 = is($("R2"));
                setComponent(new Proxy(this));
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
            setComponent(proxiedRule);
            return true;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return this.rule.toPrettyString(buf);
        }

        @Override
        public Rule unwrap() {
            return this.rule;
        }

        /**
         * A proxy defined by an initializer.
         *
         * @author Philippe Poulard
         */
        public static class Deferred extends Proxy implements Initializable<Rule> {

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
            public Rule init() {
                if (this.initializer != null) {
                    Rule r = this.initializer.get();
                    is(r);
                    return r;
                } else {
                    return null;
                }
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
    class Optional extends Repeatable implements Repeatable.Maximal, Repeatable.Minimal {

        /**
         * Create an optional rule
         *
         * @param optionalRule The rule to make optional.
         */
        public Optional(Rule optionalRule) {
            super(optionalRule);
            this.parser = (Parser<Optional>) (optional, scanner, handler) -> {
                handler.receive(new RuleStart(optional, scanner));
                Match match = optional.getComponent().parse(scanner, handler);
                handler.receive(new RuleEnd(optional, scanner, true));
                return match.asOptional();
            };
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return getComponent().toStringBuilder(buf).append('?');
        }

        @Override
        public int getMaximal() {
            return 1;
        }

        @Override
        public int getMinimal() {
            return 0;
        }

    }

    /**
     * Make a rule optional repeatable.
     *
     * @author Philippe Poulard
     */
    class ZeroOrMore extends Repeatable implements Repeatable.Minimal {

        /**
         * Create an optional repeatable rule
         *
         * @param repeatableRule The rule to make optional repeatable.
         */
        public ZeroOrMore(Rule repeatableRule) {
            super(repeatableRule);
            this.parser = (Parser<ZeroOrMore>) (zeroOrMore, scanner, handler) -> {
                // never fail, don't need to mark
                handler.receive(new RuleStart(zeroOrMore, scanner));
                Match ruleMatch = Match.EMPTY;
                if (scanner.hasNext()) {
                    do {
                        Match match = zeroOrMore.getComponent().parse(scanner, handler);
                        if (match.empty()) {
                            break;
                        } else {
                            ruleMatch = Match.SUCCESS;
                        }
                    } while (scanner.hasNext());
                }
                handler.receive(new RuleEnd(zeroOrMore, scanner, true));
                return ruleMatch;
            };
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return getComponent().toStringBuilder( buf).append('*');
        }

        @Override
        public int getMinimal() {
            return 0;
        }

    }

    /**
     * Make a rule repeatable.
     *
     * @author Philippe Poulard
     */
    class AtLeast extends Repeatable implements Repeatable.Minimal {

        int min;

        /**
         * Create a repeatable rule
         *
         * @param repeatable The rule to make repeatable.
         * @param min The minimal number of times to repeat.
         */
        public AtLeast(Rule repeatable, int min) {
            super(repeatable);
            this.min = min;
            this.parser = (Parser<AtLeast>) (atLeast, scanner, handler) -> {
                int count = 0;
                handler.mark();
                handler.receive(new RuleStart(atLeast, scanner));
                Match ruleMatch = scanner.hasNext()
                        ? atLeast.getComponent().parse(scanner, handler)
                        : Match.FAIL;
                if (! ruleMatch.empty() && scanner.hasNext()) {
                    do {
                        Match match = atLeast.getComponent().parse(scanner, handler);
                        if (match.empty()) {
                            break;
                        } else if (++count == this.min) {
                            ruleMatch = Match.SUCCESS;
                        }
                    } while (scanner.hasNext());
                }
                handler.receive(new RuleEnd(atLeast, scanner, ! ruleMatch.fail()));
                handler.commit(! ruleMatch.fail());
                return ruleMatch.asMandatory();
            };
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return getMinimal() == 1 ? getComponent().toStringBuilder( buf ).append('+')
                                     : getComponent().toStringBuilder( buf )
                                         .append('{').append(this.min).append(",}");
        }

        @Override
        public java.util.Optional<Rule> simplify() {
            if (! getComponent().isGrammarField() && getComponent() instanceof AtMost) {
                AtMost atMost = (AtMost) getComponent();
                return java.util.Optional.of(
                    atMost.getComponent().bounds(this.min, atMost.max)
                );
            }
            return java.util.Optional.empty();
        }

        @Override
        public int getMinimal() {
            return this.min;
        }

    }

    /**
     * Make a rule repeatable.
     *
     * @author Philippe Poulard
     */
    class AtMost extends Repeatable implements Repeatable.Maximal {

        int max;

        /**
         * Create a repeatable rule
         *
         * @param repeatable The rule to make repeatable.
         * @param max The maximal number of times to repeat.
         */
        public AtMost(Rule repeatable, int max) {
            super(repeatable);
            this.max = max;
            this.parser = (Parser<AtMost>) (atMost, scanner, handler) -> {
                int count = 0;
                // never fail, don't need to mark
                handler.receive(new RuleStart(atMost, scanner));
                Match ruleMatch = Match.EMPTY;
                if (scanner.hasNext()) {
                    do {
                        Match match = atMost.getComponent().parse(scanner, handler);
                        if (match.empty()) {
                            break;
                        } else {
                            ruleMatch = Match.SUCCESS;
                        }
                    } while (scanner.hasNext() && ++count <= this.max);
                }
                handler.receive(new RuleEnd(atMost, scanner, true));
                return ruleMatch;
            };
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return getComponent().toStringBuilder( buf ).append("{,").append(this.max).append('}');
        }

        @Override
        public java.util.Optional<Rule> simplify() {
            if (! getComponent().isGrammarField() && getComponent() instanceof AtLeast) {
                AtLeast atLeast = (AtLeast) getComponent();
                return java.util.Optional.of(
                    atLeast.getComponent().bounds(atLeast.min, this.max)
                );
            }
            return java.util.Optional.empty();
        }

        @Override
        public int getMaximal() {
            return this.max;
        }

    }

    /**
     * Base class for repeatable rules.
     *
     * @author Philippe Poulard
     */
    abstract class Repeatable extends Wrapper {

        /**
         * For repeatable rules that must be repeated a minimal number of times.
         *
         * @author Philippe Poulard
         */
        public interface Minimal {

            /**
             * Get the minimal number of repetition.
             *
             * @return An int greater than 2, or 0 (by default, a rule occurs 1).
             */
            int getMinimal();
        }

        /**
         * For repeatable rules that must be repeated a maximal number of times.
         *
         * @author Philippe Poulard
         */
        public interface Maximal {

            /**
             * Get the maximal number of repetition.
             *
             * @return An int greater than 2, or 0 (by default, a rule occurs 1).
             */
            int getMaximal();
        }

        /**
         * Allow to repeat a rule.
         *
         * @param rule The rule to repeat.
         */
        public Repeatable(Rule rule) {
            super(rule);
        }

        @Override
        public java.util.Optional<Rule> simplify() {
            return SIMPLIFY.apply(this);
        }

        /**
         * The simplify method for repeatable rules.
         */
        public static Function<Repeatable, java.util.Optional<Rule>> SIMPLIFY = rule1 -> {
            if (! rule1.isGrammarField()) { // don't simplify NAMED rules
                Rule nested = rule1.getComponent();
                if (nested instanceof SimpleRule && (nested instanceof Minimal || nested instanceof Maximal)) {
                    SimpleRule rule2 = (SimpleRule) nested;
                    nested = rule2.getComponent();
                    // so far we have rule1 and rule2 that can be merged,
                    //                               and the nested rule
                    boolean min1 = rule1 instanceof Minimal;
                    boolean max1 = rule1 instanceof Maximal;
                    boolean min2 = rule2 instanceof Minimal;
                    boolean max2 = rule2 instanceof Maximal;
                    boolean haveMin = min1 || min2;
                    boolean haveMax = max1 || max2;
                    int min = haveMin ? (min1 ? (min2 ? Math.min(((Minimal) rule1).getMinimal(), ((Minimal) rule2).getMinimal())
                                                      : ((Minimal) rule1).getMinimal())
                                              : ((Minimal) rule2).getMinimal())
                                      : 1;
                    int max = haveMax ? (max1 ? (max2 ? Math.max(((Maximal) rule1).getMaximal(), ((Minimal) rule2).getMinimal())
                                                      : ((Maximal) rule1).getMaximal())
                                              : ((Maximal) rule2).getMaximal())
                                      : 1;
                    Rule simplified = haveMin ? (haveMax ? nested.bounds(min, max)
                                                         : nested.atLeast(min))
                                              : (haveMax ? nested.atMost(max)
                                                         : nested);
                    return java.util.Optional.of(simplified);
                }
            }
            return java.util.Optional.empty();
        };
    }

    /**
     * Make a rule repeatable in bounds.
     *
     * @author Philippe Poulard
     */
    class Bounds extends Repeatable implements Repeatable.Minimal, Repeatable.Maximal {

        int min;
        int max;

        /**
         * Create an optional repeatable rule
         *
         * @param repeatableRule The rule to repeat in bounds.
         * @param min The minimum occurs.
         * @param max The maximum occurs.
         */
        public Bounds(Rule repeatableRule, int min, int max) {
            super(repeatableRule);
            this.min = min;
            this.max = max;
            this.parser = (Parser<Bounds>) (bounds, scanner, handler) -> {
                int count = 0;
                Match ruleMatch = Match.FAIL;
                if (this.min > 0) {
                    handler.mark();
                    ruleMatch = Match.EMPTY;
                } // else never fail, don't need to mark
                handler.receive(new RuleStart(bounds, scanner));
                if (scanner.hasNext()) {
                    do {
                        Match match = bounds.getComponent().parse(scanner, handler);
                        if (match.empty()) {
                            break;
                        } else if (++count == this.min) {
                            ruleMatch = Match.SUCCESS;
                        }
                    } while (scanner.hasNext() && count++ <= this.max);
                }
                handler.receive(new RuleEnd(bounds, scanner, true));
                if (this.min > 0) {
                    handler.commit(! ruleMatch.fail());
                    return ruleMatch.asMandatory();
                }
                return ruleMatch;
            };
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return getComponent().toStringBuilder( buf)
                .append('{').append(this.min).append(',').append(this.max).append('}');
        }

        @Override
        public int getMaximal() {
            return this.max;
        }

        @Override
        public int getMinimal() {
            return this.min;
        }

    }

    /**
     * A rule skeleton for combining rules.
     *
     * @author Philippe Poulard
     */
    abstract class Combine extends Rule implements TraversableRule.CombinedRule {

        /**
         * For a rule that can be replaced by its component if it has
         * just one, provided that it is not a grammar field.
         *
         * @see TraversableRule#isGrammarField()
         *
         * @author Philippe Poulard
         */
        public interface Simplifiable extends ComposedRule<List<Rule>>, TraversableRule {

            @Override
            default java.util.Optional<Rule> simplify() {
                return SIMPLIFY.apply(this);
            }

            /**
             * The default simplify method doesn't simplify named rules.
             *
             * @see TraversableRule#isGrammarField()
             */
            Function<Simplifiable, java.util.Optional<Rule>> SIMPLIFY = compRule -> {
                if (! compRule.isGrammarField() // don't simplify NAMED rules
                        && compRule.getComponent().size() == 1)
                {
                    return java.util.Optional.of(compRule.getComponent().get(0));
                } else {
                    return java.util.Optional.empty();
                }
            };

        }

        /**
         * For a rule that have components that may be flatten.
         *
         * @author Philippe Poulard
         */
        public interface Flattenable extends ComposedRule<List<Rule>> {

            @Override
            default void flatten() {
                FLATTEN.accept(this);
            }

            /**
             * The default flatten method.
             */
            Consumer<ComposedRule<List<Rule>>> FLATTEN = compRule -> {
                compRule.setComponent(
                    compRule.getComponents().flatMap(r -> {
                        if (r.isGrammarField() // don't flatten NAMED rules
                                || ! (compRule.getClass().isAssignableFrom(r.getClass())) )
                        {
                            return Stream.of(r);
                        }
                        if (r instanceof ComposedRule) {
                            ComposedRule<?> cr = (ComposedRule<?>) r;
                            cr.flatten();
                            r = cr.simplify().orElse(r);
                        }
                        return ((ComposedRule<?>) r).getComponents();
                    }).collect(toList())
                );
            };

        }

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
    class Choice extends Combine implements Combine.Simplifiable {

        /**
         * Create a choice rule.
         *
         * @param alt An alternate rule.
         */
        public Choice(Rule alt) {
            super(alt);
            setParser();
        }

        /**
         * Create a choice rule.
         *
         * @param rules The alternate rules.
         */
        public Choice(Stream<? extends Rule> rules) {
            super(rules);
            setParser();
        }

        void setParser() {
            this.parser = (Parser<Choice>) (choice, scanner, handler) -> {
                handler.mark();
                handler.receive(new RuleStart(choice, scanner));

                scanner.mark();
                for (Rule rule: choice.getComponent()) {
                    if (scanner.hasNext()) {
                        Match match = rule.parse(scanner, handler);
                        if (! match.empty()) {
                            scanner.consume();
                            handler.receive(new RuleEnd(choice, scanner, true));
                            handler.commit(true);
                            return match;
                        }
                    }
                }
                scanner.cancel();
                handler.receive(new RuleEnd(choice, scanner, false));
                handler.commit(false);
                return Match.FAIL;
            };
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            return getComponent().stream()
                .collect( StringBuilderUtil.collectorOf(
                    "( ", " | ", " )", buf,
                    rule -> rule.toStringBuilder(buf))
                );
        }

        @Override
        public void flatten() {
            // first, flatten and simplify everything within
            Flattenable.FLATTEN.accept(this);

            // then, merge consecutive CharTokens
            int size = getComponent().size();
            List<Rule> newRules = new ArrayList<>(size);
            CharToken[] current = new CharToken[1];
            getComponents().forEach(r -> {
                if (r instanceof CharToken) {
                    if (current[0] == null) {
                        current[0] = (CharToken) r;
                    } else {
                        // it doesn't matter that the result CharToken lost the label
                        // of their neighbor (or nested) CharToken that would have match :
                        // we can't map it to a custom type (if it was mapped to a custom
                        // type, it wouldn't be a CharToken).
                        current[0] = current[0].union((CharToken) r);
                    }
                } else {
                    if (current[0] != null) {
                        newRules.add(current[0]);
                        current[0] = null;
                    }
                    newRules.add(r);
                }
            });
            if (current[0] != null) {
                newRules.add(current[0]);
            }
            if (newRules.size() < size) {
                setComponent(newRules);
            }
        }

    }

    /**
     * A sequence rule.
     *
     * @author Philippe Poulard
     */
    class Sequence extends Combine implements Combine.Simplifiable, Combine.Flattenable {

        /**
         * Create a sequence rule.
         *
         * @param sequence The rule in the sequence.
         */
        public Sequence(Rule sequence) {
            super(sequence);
            setParser();
        }

        /**
         * Create a sequence rule.
         *
         * @param rules The rules in the sequence.
         */
        public Sequence(Stream<? extends Rule> rules) {
            super(rules);
            setParser();
        }

        void setParser() {
            this.parser = (Parser<Sequence>) (sequence, scanner, handler) -> {
                handler.mark();
                handler.receive(new RuleStart(sequence, scanner));
                scanner.mark();
                Match ruleMatch = Match.EMPTY;
                for (Rule rule: sequence.getComponent()) {
                    Match match = rule.parse(scanner, handler);
                    if (match.fail()) {
                        ruleMatch = Match.FAIL;
                        break;
                    } else if (! match.empty()) {
                        ruleMatch = Match.SUCCESS;
                    }
                }
                scanner.commit(! ruleMatch.fail());
                handler.receive(new RuleEnd(sequence, scanner, ! ruleMatch.fail()));
                handler.commit(! ruleMatch.fail());
                return ruleMatch;
            };
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
    abstract class Token extends Rule {

        /**
         * Unless annotated, a Token is not a fragment.
         */
        public Token() {
            this.fragment = false;
            this.parser = (Parser<Token>) (token, scanner, handler) -> {
                return Thrower.safeCall(() -> {
                    boolean alreadyMarked = this.parser instanceof Parser.Skip;
                    if (! alreadyMarked) {
                        scanner.mark();
                    }
                    boolean parsed = token.parse(scanner, handler, alreadyMarked);
                    if (! alreadyMarked) {
                        scanner.commit(parsed);
                    }
                    return parsed ? Match.SUCCESS : Match.FAIL;
                });
            };
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
    abstract class CharToken extends Token {

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
         * Combine this char token with the given Unicode codepoint.
         *
         * @param codepoint The Unicode codepoint.
         *
         * @return A char token made of this char token union the Unicode codepoint.
         */
        public CharToken union(int codepoint) {
            CharRange cr = this.charRange.union(CharRange.is(codepoint));
            return new CharToken.Single(cr);
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
            return new CharToken.Single(cr);
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
            return new CharToken.Single(cr);
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
            return new CharToken.Single(cr);
        }

        /**
         * Combine this char token with the given char tokens.
         *
         * @param charTokens The char ranges to combine, MUST BE
         *      CharToken.
         *
         * @return A char token made of this char token union the given char tokens.
         */
        public CharToken union(Token... charTokens) {
            return new Composed(
                    this.charRange,
                    Arrays.asList(charTokens).stream()
                        .map(t -> (Rule) t)
                        .collect(toList()),
                    CharRange::union // Composed by union
            );
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
            return new CharToken.Single(cr);
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
            return new CharToken.Single(cr);
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
            return new CharToken.Single(cr);
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
            return new CharToken.Single(cr);
        }

        /**
         * Remove from this char token the given char tokens.
         *
         * @param charTokens The char tokens to combine, MUST BE
         *      CharToken.
         *
         * @return A char token made of this char token minus the given char tokens.
         */
        public CharToken except(Token... charTokens) {
            return new Composed(
                    this.charRange,
                    Arrays.asList(charTokens).stream()
                        .map(t -> (Rule) t)
                        .collect(toList()),
                    CharRange::except // Composed by exclusion
            );
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

        /**
         * A token made of a single char range.
         *
         * @author Philippe Poulard
         */
        public static class Single extends CharToken implements TraversableRule.StandaloneRule {

            /**
             * Create a single char token.
             *
             * @param charRange The actual char range.
             */
            public Single(CharRange charRange) {
                super(charRange);
            }
        }

        /**
         * A token made of a several char ranges.
         *
         * @author Philippe Poulard
         */
        public static class Composed extends CharToken implements TraversableRule.CombinedRule, Combine.Flattenable {

            // we must keep the memory of the base char range and its components,
            // because they may be affected by substitutions ; in this case, the
            // simplify() function must be run again
            CharRange base;
            List<Rule> charTokens;
            BiFunction<CharRange, CharRange[], CharRange> composition;

            /**
             * Create composable char token.
             *
             * @param charRange The actual char range.
             * @param charTokens The others tokens, MUST be or wrap char tokens.
             * @param composition Indicates whether to compose by union or exclusion.
             *
             * @see CharRange#union(CharRange...)
             * @see CharRange#except(CharRange...)
             */
            public Composed(CharRange charRange, List<Rule> charTokens,
                    BiFunction<CharRange, CharRange[], CharRange> composition)
            {
                super(null);
                this.base = charRange;
                this.charTokens = charTokens;
                this.composition = composition;
                simplify(); // set super.charRange
            }

            @Override
            public List<Rule> getComponent() {
                return this.charTokens;
            }

            @Override
            public void setComponent(List<Rule> component) {
                // may happen if one component has been substituted
                this.charTokens = component;
                simplify(); // here it is again
            }

            @Override
            public java.util.Optional<Rule> simplify() {
                // merge the char ranges together
                this.charRange = this.composition.apply(
                        this.base,
                        this.charTokens.stream()
                            .map(c -> ((CharToken) c).charRange)
                            .toArray(l -> new CharRange[l])
                    );
                // in a grammar point of view, this didn't change the underlying rule
                return java.util.Optional.empty();
            }

        }

    }

    /**
     * A string token.
     *
     * @see Grammar#is(String)
     *
     * @author Philippe Poulard
     */
    class StringToken extends Token implements TraversableRule.StandaloneRule {

        String string;
        boolean equal;

        /**
         * Create a string token.
         *
         * @param string The string to match.
         * @param equal Indicates whether the given string has to match or must not.
         */
        public StringToken(String string, boolean equal) {
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
                    // the next string didn't match, if this token was to check exclusion
                    // it is the case, and NO TOKEN VALUE is produced here

                    // typical code if we would have to produce a string of the same length of the one to exclude :
                    // StringBuilder buf = new StringBuilder(string.length());
                    // scanner.nextString(new StringConstraint.ReadLength(string.length()), buf);
                    // handler.receive(new StringValue(this, buf.toString(), scanner));
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
     * A token made from an enum class or a list of string values ;
     * the order of the enum values doesn’t matter ; the longest value
     * if available will be read from the input.
     *
     * Internally, commons characters are grouped together to avoid testing
     * the same sequence several times while parsing.
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
    class EnumToken<T> extends Token implements TraversableRule.StandaloneRule {

        // hierarchy of enum string values by char
        EnumValues<T> values;

        @SuppressWarnings("unchecked")
        public <E extends Enum<E>> EnumToken(Class<E> values) {
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
     * An enum value token, just like a string, but with an enum value.
     *
     * @see Grammar#is(Enum)
     *
     * @author Philippe Poulard
     */
    class EnumValueToken extends Token implements TraversableRule.StandaloneRule {

        Enum<?> value;

        /**
         * Create a value token.
         *
         * @param value The actual value to match.
         */
        public EnumValueToken(Enum<?> value) {
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
    class Number extends Token implements TraversableRule.StandaloneRule {

        NumberConstraint constraint;

        /**
         * Create a number token based on a constraint.
         *
         * @param constraint The constraint to apply.
         */
        public Number(NumberConstraint constraint) {
            this.constraint = constraint;
        }

        /**
         * Create a number token.
         */
        public Number() { }

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
    class GrammarToken extends Token implements TraversableRule.StandaloneRule {

        Grammar grammar;
        Supplier<DataHandler<?>> dataHandlerSupplier;

        /**
         * Expose a grammar as a token.
         *
         * @param grammar The grammar instance.
         * @param dataHandlerSupplier Allow to supply the result parsing object.
         *
         * @see NodeBuilder
         * @see ValueBuilder
         */
        public GrammarToken(Grammar grammar, Supplier<DataHandler<?>> dataHandlerSupplier) {
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
                TokenValue<T> value;
                try {
                    value = newTokenValue(collector, scanner);
                    handler.receive(value);
                    return true;
                } catch (Exception e) {
                    // if the value can't be created, the rule didn't matched
                }
            }
            return false;
        }

        @Override
        public StringBuilder toPrettyString(StringBuilder buf) {
            buf.append('%');
            return this.rule.toStringBuilder(buf);
        }

        /**
         * A token that collect its subrules to a single string.
         *
         * @author Philippe Poulard
         */
        public static class String extends TypedToken<java.lang.String> {

            /**
             * This token is made of subrules.
             *
             * @param rule The rule that this token is made of.
             */
            public String(Rule rule) {
                this(rule, StringBuilder::toString);
            }

            /**
             * This token is made of subrules.
             *
             * @param rule The rule that this token is made of.
             * @param toString Post process to apply on the collected token
             *      to the string value.
             */
            public String(Rule rule, Function<StringBuilder, java.lang.String> toString) {
                super(rule);
                this.toString = toString;
            }

            Function<StringBuilder, java.lang.String> toString;

            @Override
            public TokenValue<java.lang.String> newTokenValue(TokensCollector<?> buf, Trackable trackable) {
                @SuppressWarnings("unchecked")
                StringBuilder sb = ((TokensCollector<StringBuilder>) buf).get();
                return new StringValue(this, this.toString.apply(sb), trackable);
            }

            @Override
            public TokensCollector<?> collector() {
                return TokensCollector.newStringBuilderHandler();
            }

        }

        /**
         * A token that collect its subrules to a number.
         *
         * @author Philippe Poulard
         */
        public static class Number extends TypedToken<java.lang.Number> {

            Class<? extends Number> numberClass;

            /**
             * This token is made of subrules.
             *
             * @param rule The rule that this token is made of.
             */
            public Number(Rule rule) {
                super(rule);
            }

            /**
             * This token is made of subrules.
             *
             * @param rule The rule that this token is made of.
             * @param numberClass The number class expected.
             */
            public Number(Rule rule, Class<? extends Number> numberClass) {
                this(rule);
                this.numberClass = numberClass;
            }

            @Override
            public TokenValue<java.lang.Number> newTokenValue(TokensCollector<?> buf, Trackable trackable) {
                @SuppressWarnings("unchecked")
                TokensCollector<LinkedList<TokenValue<?>>> coll = (TokensCollector<LinkedList<TokenValue<?>>>) buf;
                LinkedList<TokenValue<?>> values = coll.get();
                if (values.size() == 1) {
                    TokenValue<?> tv = values.get(0);
                    Object o = tv.getValue();
                    if (o instanceof java.lang.Number) {
                        return new NumberValue(this, (java.lang.Number) o, trackable);
                    }
                }
                java.lang.String s = values.stream().map(tv -> tv.toString())
                    .collect(Collectors.joining());
                java.lang.Number number = NumberUtil.parseNumber(s);
                return new NumberValue(this, number, trackable);
            }

            @Override
            public TokensCollector<?> collector() {
                return TokensCollector.newTokenValueHandler();
            }

        }

        /**
         * A token that collect its subrules to a number of a given type.
         *
         * @author Philippe Poulard
         */
        public static class TypedNumber<T extends java.lang.Number> extends TypedToken<T> {

            Class<T> numberClass;

            /**
             * This token is made of subrules.
             *
             * @param rule The rule that this token is made of.
             * @param numberClass The class number expected.
             */
            @SuppressWarnings("unchecked")
            public TypedNumber(Rule rule, Class<T> numberClass) {
                super(rule);
                this.numberClass = (Class<T>) Type.of(numberClass).box().forName().get();
            }

            @Override
            public TokenValue<T> newTokenValue(TokensCollector<?> buf, Trackable trackable) {
                @SuppressWarnings("unchecked")
                TokensCollector<LinkedList<TokenValue<?>>> coll = (TokensCollector<LinkedList<TokenValue<?>>>) buf;
                LinkedList<TokenValue<?>> values = coll.get();
                if (values.size() == 1) {
                    TokenValue<?> tv = values.get(0);
                    Object o = tv.getValue();
                    if (o instanceof java.lang.Number) {
                        T t = NumberUtil.as((java.lang.Number) o, numberClass);
                        return new TokenValue<T>(this, t, trackable);
                    }
                }
                java.lang.String s = values.stream().map(tv -> tv.toString())
                    .collect(Collectors.joining());
                @SuppressWarnings("unchecked")
                T number = (T) NumberUtil.parseNumber(s, true, this.numberClass);
                return new TokenValue<T>(this, number, trackable);
            }

            @Override
            public TokensCollector<?> collector() {
                return TokensCollector.newTokenValueHandler();
            }

        }

        /**
         * A token that collect its subrules to a list of items
         * that are mapped to a value.
         *
         * @author Philippe Poulard
         *
         * @see TokensCollector#newTokenValueHandler()
         */
        public static class Mapper<T> extends TypedToken<T> {

            Function<LinkedList<TokenValue<?>>, T> mapper;

            /**
             * Create a mapper typed token.
             *
             * @param rule The rule that this token is made of.
             * @param mapper The function that produce the final value
             *                          from the list of collected items.
             */
            public Mapper(Rule rule, Function<LinkedList<TokenValue<?>>, T> mapper) {
                super(rule);
                this.mapper = mapper;
            }

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
    static Proxy $(String rule) {
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
    CharToken $any = new CharToken.Single(CharRange.ANY);

    /**
     * The char token that matches nothing.
     */
    CharToken $empty = new CharToken.Single(CharRange.EMPTY) {

        { // because the names won't be set by Grammar$.init()
            this.name = "$empty";
            $any.name = "$any";
        }

        @Override
        public boolean parse(Scanner scanner, Handler handler, boolean alreadyMarked) throws IOException {
            return true;
        };

    };

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
     * DO NOT IMPLEMENT THIS METHOD IN YOUR GRAMMAR !
     *
     * @return A rule with all the tokens that are not fragments :
     *      the rule <code>(T1 | T2 | T3 ...)*</code>
     *
     * @see Fragment
     */
    Rule tokenizer(); // see Grammar$

    /**
     * Return the main rule of the grammar.
     *
     * DO NOT IMPLEMENT THIS METHOD IN YOUR GRAMMAR !
     *
     * @return The main rule if one has been set
     *      on the grammar or one of its interfaces.
     *
     * @see MainRule
     */
    java.util.Optional<Rule> mainRule(); // see Grammar$

    /**
     * Adopt a rule of another grammar in this grammar, that
     * is to say apply the relevant substitutions if any.
     *
     * The rule to adopt should be a field in a grammar that
     * is extended by this grammar.
     *
     * DO NOT IMPLEMENT THIS METHOD IN YOUR GRAMMAR !
     *
     * @param rule The rule to adopt, should be a rule of this
     *      grammar or an inherited grammar.
     * @return The same rule if this rule is already a field of
     *      this grammar, or a clone otherwise.
     */
    Rule adopt(Rule rule); // see Grammar$

}
