package ml.alternet.parser.util;

import static ml.alternet.misc.Thrower.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ml.alternet.facet.Initializable;
import ml.alternet.misc.Thrower;
import ml.alternet.parser.Grammar;
import ml.alternet.util.ByteCodeFactory;
import ml.alternet.util.ClassUtil;
import ml.alternet.util.gen.ByteCodeSpec;

/**
 * Used internally to prepare every grammar :
 * perform initialization of the name fields,
 * some optimizations, and substitutions.
 *
 * @author Philippe Poulard
 */
public abstract class Grammar$ implements Grammar, Initializable {

    // this byte code factory creates an instance of a Grammar interface
    @ByteCodeSpec(parentClass = Grammar$.class, factoryPkg = "ml.alternet.parser.util",
            template = "/ml/alternet/parser/util/ByteCodeFactory$.java.template")
    static final ByteCodeFactory BYTECODE_FACTORY = ByteCodeFactory
        // the factory class exist after code generation
        .getInstance("ml.alternet.parser.util.ByteCodeFactory$");

    // allow to find the caller grammar interface
    private static class GrammarInterfaceFinder extends SecurityManager  {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Class<? extends Grammar> getGrammarInterface() {
            Class[] classes = getClassContext();
            // call stack :
            // [class ml.alternet.parser.util.Grammar$$GrammarInterfaceFinder,
            //  class ml.alternet.parser.util.Grammar$,
            //  interface ml.alternet.parser.Grammar,
            //  interface org.example.YourGrammar,
            // ...]
            return classes[3];
        };
    }
    private static final GrammarInterfaceFinder GIF = new GrammarInterfaceFinder();

    /**
     * Return the singleton instance of the given grammar.
     *
     * @param grammar The grammar must be an interface.
     *
     * @return An instance of the given interface,
     *      generated without methods implementation
     *      that would be specified in the interface.
     *
     * @param <T> The target grammar interface.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Grammar> T $(Class<? extends Grammar> grammar) {
        if (grammar.isInterface()) {
            try {
                Grammar$ g = (Grammar$) BYTECODE_FACTORY.getInstance(grammar);
                g.grammar = grammar;
                // before doing anything on the grammar,
                // ensure to prepare it before
                g.init();
                return (T) g;
            } catch (NoSuchFieldException | SecurityException
                    | InstantiationException | IllegalAccessException
                    | ClassNotFoundException | IllegalArgumentException e)
            {
                // Trouble shooting java.lang.IllegalAccessError:
                //        class a.b.c.D$E$ cannot access its superinterface a.b.c.D$E
                //        because a.b.c.D$E is not public
                return Thrower.doThrow(e);
            }
        } else {
            throw new IllegalArgumentException(grammar.getName() + " is not an interface.");
        }
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
    @SuppressWarnings("unchecked")
    public static <T> T $() {
        Class<? extends Grammar> g = GIF.getGrammarInterface();
        // check preconditions : all Rules must be non-null
        Field[] fields = g.getDeclaredFields();
        for (int i = fields.length - 1 ; i >= 0; i--) { // start from the end for fail fast
            Field f = fields[i];
            try {
                // if it remains a rule not initialized, return a Proxy()
                if (Rule.class.isAssignableFrom(f.getType()) && f.get(null) == null) {
                    return (T) new Grammar.Proxy();
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                Thrower.doThrow(e);
            }
        }
        // otherwise return the Grammar instance
        return $(g);
    }

    private Class<? extends Grammar> grammar; // the underlying grammar
    private Rule tokenizer; // once it has been computed, it is stored for later use
    private java.util.Optional<Grammar.Rule> mainRule; // same comment
    private final Map<Rule, Rule> substitutions = new HashMap<>(); // contain rules that replace another ones
    private Map<Rule, Rule> adopted = new HashMap<>(); // contain rules from another grammar
    private boolean init = false;

    @Override
    public <T> T init() {
        if (! this.init) {
            // preconditions will be checked by RuleField on the first field browse
            this.init = true;
            // apply initializations in this order
            // don't merge the loops in those methods !!!
            // typically : naming is an important step that must be
            //          performed BEFORE optimization and substitutions
            try {
                // set the name of rules that are fields,
                // process fragments, and process proxies
                processFields();
                // resolve placeholders $self and $("namedRule")
                processPlaceHolders();
                // flatten an simplify
                processOptimizations();
                // substitutions and extensions
                processSubstitutions();
                // ws
                processWhitespacePolicy();
            } catch (IllegalArgumentException | IllegalAccessException e) {
                return Thrower.doThrow(e);
            }
        }
        return null;
    }

    /**
     * Return the main rule of the grammar.
     *
     * @return The main rule if one has been set
     *      on the grammar or one of its interfaces.
     *      ({@code Grammar.MainRule})
     */
    @Override
    public java.util.Optional<Rule> mainRule() {
        if (this.mainRule == null) {
            // works on init() grammars only
            this.mainRule = fields()
                .filter(f -> f.getAnnotation(MainRule.class) != null)
                .map(f -> (Rule) safeCall(() -> f.get(null)))
                .map(this::adoption) // if it comes from an inherited grammar
                .findFirst();
        }
        return this.mainRule;
    }

    /**
     * Return the "tokenizer rule", that is to
     * say all the non-fragment tokens explicitly
     * specified by this grammar ({@code Grammar.Fragment}).
     *
     * @return The rule <code>(T1 | T2 | T3 ...)*</code>
     *
     * @see Grammar#tokenizer()
     */
    @Override
    public Rule tokenizer() {
        if (this.tokenizer == null) {
            // works on init() grammars only
            Stream<Token> tokens = fields()
                .filter(f ->   Token.class.isAssignableFrom(f.getType()) // keep tokens
                            && f.getAnnotation(Fragment.class) == null)  // that ARE NOT fragments
                .map(f -> (Token) safeCall(() -> f.get(null)))
                .map(this::adoption) // if it comes from an inherited grammar
                .map(t -> (Token) t);
            this.tokenizer = new Choice(tokens).zeroOrMore();
        }
        return this.tokenizer;
    }

    // =============== UTILITIES

    private void addSubstitution(Field from, Rule to) {
        try {                            // from is a static field
            Rule fromRule = (Rule) from.get(from.getDeclaringClass());
            this.substitutions.put(fromRule, to);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            Thrower.doThrow(e);
        }
    }

    /**
     * Get the whitespace policy from the annotation.
     *
     * @param whitespacePolicy The whitespace policy annotation,
     * @param defaultValue Supply the default value
     *      when <code>whitespacePolicy</code> is <code>null</code>.
     *
     * @return A function that filters out Unicode codepoints.
     */
    static java.util.Optional<Predicate<Integer>> getWhitespacePolicy(
        WhitespacePolicy whitespacePolicy,
        Supplier<java.util.Optional<Predicate<Integer>>> defaultValue)
    {
        if (whitespacePolicy == null) {
            return defaultValue.get();
        } else if (whitespacePolicy.preserve()) {
            return java.util.Optional.empty();
        } else {
            return java.util.Optional.of(
                safeCall(() -> whitespacePolicy.isWhitespace().newInstance())
            );
        }
    }

    private static class RuleField {

        Field field;
        Class<? extends Grammar> grammar;

        public RuleField(Class<? extends Grammar> grammar, Field field) {
            this.field = field;
            this.grammar = grammar;
        }

        Rule rule() {
            try {
                Rule rule = (Rule) field.get(null);
                if (rule == null) {
                    // case when Rule r = null;
                    //        or TheGrammar $ = $(); is not the last field
                    // -> abort
                    throw new NullPointerException("A Rule or Token is 'null' in "
                            + this.grammar + "\n or NOT initialized because the field " + field.getName()
                            + " is declared AFTER getting the instance of the grammar with $() :"
                            + "\n all Rule/Token fields MUST be declared before the Grammar field");
                } else {
                    return rule;
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                return Thrower.doThrow(e);
            }
        }

    }

    private Stream<RuleField> getRuleFields() {
        return Arrays.stream(this.grammar.getDeclaredFields())
            .map(field -> {
                if (Rule.class.isAssignableFrom(field.getType())) {
                    return new RuleField(this.grammar, field);
                }
                return null;
            })
            .filter(o -> o != null);
    }

    // the fields declared by this grammar and its interfaces
    private Stream<Field> fields() {
        Stream<Field> fields = ClassUtil.getClasses(this.grammar)
            .filter(g -> g != Grammar.class)
            .flatMap(c -> Arrays.asList(c.getDeclaredFields()).stream())
            .filter(f -> ! "$any".equals(f.getName()) && ! "$empty".equals(f.getName()));
        return fields;
    }

    // =============== INITIALIZERS

    private void processFields() throws IllegalArgumentException, IllegalAccessException {
        getRuleFields().forEach(rf -> {
            Rule rule = rf.rule();
            if (rule.isGrammarField()) {
                throw new IllegalArgumentException("The field \"" + rf.field.getName()
                        + "\" is a direct reference of an existing named rule."
                        + "\nPlease correct your grammar by defining your field as a proxy :"
                        + "\nRule " + rf.field.getName() + " = is(" + rule.getName() + ");");
            }
            // set the name of the rule with the name it has in the interface
            String name = rf.field.getName();
            rule.setName(name);
            if (rf.field.getAnnotation(Fragment.class) == null) {
                rule.setFragment(false);
            } else { // assume true by default
                rule.setFragment(true);
            }

            // set fields declaration that are proxies
            if (rule instanceof Proxy) {
                // lookup for a method that has the same name as the field
                // Proxy foo = proxy();
                Proxy proxy = ((Proxy) rule);
                try {
                    // static Rule foo() {
                    //     return SomeRule.zeroOrMore();
                    // }
                    Method method = this.grammar.getDeclaredMethod(name);
                    try {
                        // call the method
                        // Rule rule = foo();
                        Rule r = (Rule) method.invoke(proxy);
                        // set it to the proxy
                        proxy.is(r);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        Thrower.doThrow(e);
                    }
                } catch (NoSuchMethodException | SecurityException e) {
                    // lookup for a field that has the same name as the field prepend with $
                    try {
                        // Supplier<Rule> $foo = () -> SomeRule.zeroOrMore();
                        Field field = this.grammar.getDeclaredField('$' + name);
                        try {
                            // get the field and get the rule
                            @SuppressWarnings("unchecked")
                            Rule r = ((Supplier<Rule>) field.get(null)).get();
                            // set it to the proxy
                            proxy.is(r);
                        } catch (IllegalArgumentException | IllegalAccessException e1) {
                            Thrower.doThrow(e);
                        }
                    } catch (NoSuchFieldException | SecurityException e1) {
                        // ignore : the proxy is initialize by other mean
                    }
                }
            }
            if (rule instanceof Initializable) {
                ((Initializable) rule).init();
            }
        });
    }

    // resolve placeholders $self and $("namedRule")
    private void processPlaceHolders() {
        getRuleFields().forEach(rf -> {
            BiFunction<Rule, Rule, Rule> placeholderResolver = (hostRule, rule) -> {
                if (rule == Grammar.$self) {
                    rule = hostRule;
                } else if (rule instanceof Grammar.Proxy.Named) {
                    Grammar.Proxy.Named proxy = (Grammar.Proxy.Named) rule;
                    if (proxy.getProxyName() != null) {
                        try {
                            Field f = this.grammar.getDeclaredField(proxy.getProxyName());
                            rule = (Rule) f.get(this.grammar);
                        } catch (NoSuchFieldException | SecurityException
                                | IllegalArgumentException | IllegalAccessException e)
                        {
                            Thrower.doThrow(e);
                        }
                    }
                }
                return rule;
            };
            rf.rule().traverse(
                rf.rule(),
                new HashSet<Rule>(),
                placeholderResolver,
                r -> r  // apply the changes directly
            );
        });
    }

    // flatten sequences or choices of unnamed rules
    // flatten CharToken even on named tokens, but with rewrite capability on substitution
    // (it is useless to run optimizations after substitution because other new grammar
    //  extensions can occur far after)
    private void processOptimizations() {
        // flatten when possible
        getRuleFields().forEach(rf -> {
            Rule r = rf.rule();
            if (r instanceof ComposedRule) {
                ((ComposedRule<?>) r).flatten();
            }
        });
    }

    // when a grammar extends another grammar and extend or overload some fields
    private void processSubstitutions() throws IllegalArgumentException, IllegalAccessException {
        // 1) collect the substitutions
        getRuleFields().forEach(rf -> {
            // a substitution is either declared explicitly by @Replace
            // or because a field has the same name of a field in an inherited grammar
            Grammar.Replace replace = rf.field.getAnnotation(Grammar.Replace.class);
            if (replace == null) { // field override ?
                // find a rule with the same name in the inherited interfaces
                ClassUtil.getClasses(this.grammar)
                    .filter(i -> i != this.grammar && i != Grammar.class)
                    .flatMap(c -> Arrays.asList(c.getDeclaredFields()).stream())
                    .filter(f -> rf.rule().isGrammarField()
                            && f.getName().equals(rf.rule().getName())
                            && Rule.class.isAssignableFrom(f.getType()))
                    .findFirst()
                    .ifPresent(f -> addSubstitution(f, rf.rule()));
                    // else it is not a replacement... go on
            } else if (! replace.disable()) {
                // @Replace was set
                Stream<Class<?>> interfaces;
                if (replace.grammar() == Grammar.class) { // means : not specified
                    // find a rule with the given name in the inherited interfaces
                    interfaces = ClassUtil.getClasses(this.grammar)
                        .filter(i -> i != this.grammar && i != Grammar.class);
                } else {
                    // find a rule with the given name in the given interface
                    interfaces = Stream.of(replace.grammar());
                }
                Field source = interfaces
                    .flatMap(c -> Arrays.asList(c.getDeclaredFields()).stream())
                    .filter(f -> f.getName().equals(replace.field())
                            && Rule.class.isAssignableFrom(f.getType()))
                    .findFirst() // with @Replace, we MUST find one
                    .orElseThrow(() -> new NoSuchFieldError("Substitution not found "
                                    + replace + " in " + this.grammar.getName()));
                addSubstitution(source, rf.rule());
            } // else it is not a substitution
        });

        // 2) apply the substitutions to all fields
        if (! this.substitutions.isEmpty()) {
            Set<Rule> traversed = new HashSet<>();
            getRuleFields()
                .filter(rf -> rf.field.getAnnotation(MainRule.class) == null)
                .forEach(rf -> {
                    Rule rule = rf.rule();
                    Rule replace = rule.traverse(
                        rule,
                        traversed,
                        this.substitutionsResolver,
                        r -> r == rule
                            ? r // FIXME : just explain why ???
                            : (Rule) safeCall(r::clone) // keep intact the original rule
                    );
                    if (rule != replace) {
                        // set the new rule in the field
                        try {
                            Field modifField = Field.class.getDeclaredField("modifiers");
                            modifField.setAccessible(true);
                            final int modifiers = rf.field.getModifiers();
                            modifField.setInt(rf.field, modifiers & ~Modifier.FINAL);
                            rf.field.setAccessible(true);
                            rf.field.set(this.grammar, replace);
                            modifField.setInt(rf.field, modifiers);
                        } catch (IllegalArgumentException | IllegalAccessException
                                | NoSuchFieldException | SecurityException e)
                        {
                            Thrower.doThrow(e);
                        }
                    }
            });

            // apply the substitutions to the main rule
            // FIXME : explain WHY it can't be performed by the previous loop ???
            java.util.Optional<Rule> main = mainRule();
            main.ifPresent(mainRule -> {
                this.mainRule = java.util.Optional.of(
                    mainRule.traverse(
                        mainRule,
                        traversed,
                        this.substitutionsResolver,
                        r -> (Rule) safeCall(r::clone)) // keep intact the original rule
                );
            });
        }
    }

    private BiFunction<Rule, Rule, Rule> substitutionsResolver = (host, from) -> {
        if (host == from) {
            return from;
        }
        Rule to = this.substitutions.get(from);
        if (to == null
            || host == to) // prevent substitution on extension
                           // e.g.  Grammar g1 extends g2 {
                           //          Rule r = g2.r.asToken(...);
        {   // unchanged
            return from;
        } else {
            return to; // from -> to
        }
    };

    @SuppressWarnings("unchecked")
    private void processWhitespacePolicy() throws IllegalArgumentException, IllegalAccessException {
        java.util.Optional<Predicate<Integer>> globalWhitespacePolicy = getWhitespacePolicy(
                this.grammar.getAnnotation(Grammar.WhitespacePolicy.class),
                () -> java.util.Optional.empty()
//                () -> java.util.Optional.of(new JavaWhitespace()) // by default, ignore Java WS
        );
        // set whitespace policy on each field
        getRuleFields().forEach(rf -> {
            if (rf.rule() instanceof HasWhitespacePolicy) {
                HasWhitespacePolicy token = (HasWhitespacePolicy) rf.rule();
                java.util.Optional<Predicate<Integer>> whitespacePolicy = getWhitespacePolicy(
                        rf.field.getAnnotation(Grammar.WhitespacePolicy.class),
                        () -> globalWhitespacePolicy // by default, inherit the default of the grammar
                );
                token.setWhitespacePolicy(whitespacePolicy);
            }
        });
//        // inherit the whitespace policy
//        getRuleFields().forEach(rf -> {
//System.out.println(rf.field.getName() + " === ");
//            rf.rule.traverse(rf.rule,
//                new HashSet<Rule>(),
//                (hostRule, rule) -> {
//                    System.out.println(rf.field.getName() + " <<<<< " + rule);
//                    if (rule instanceof HasWhitespacePolicy) {
//                        HasWhitespacePolicy token = (HasWhitespacePolicy) rule;
//
//                        if (! token.getWhitespacePolicy().isPresent()) {
//                            // inherit
//
//
//                            java.util.Optional<Predicate<Integer>> whitespacePolicy = getWhitespacePolicy(
//                                    rf.field.getAnnotation(Grammar.WhitespacePolicy.class),
//                                    () -> globalWhitespacePolicy // by default, inherit the default of the grammar
//                            );
//                            token.setWhitespacePolicy(whitespacePolicy);
//                        }
//                    }
//                    return rule;
//                },
//                r -> r); // apply the changes directly
//        });
        // don't try to mix this loop with the one above
        HashSet<Rule> traversed = new HashSet<>();
        getRuleFields()
        .map(rf -> rf.rule())

// can't optimize on NAMED rules, it cause fail when replaced
//.filter(r -> false)


        .forEach(rule -> {
            // optimize for Choice : if all items have the same WSP, remove it and apply it on the choice
            // TODO : if we have a named rule, should we allow to change that on substitution ???
            rule.traverse(rule,
                traversed,
                (hostRule, r) -> {
                    if (r instanceof Choice) {
                        Choice choice = (Choice) r;
                        choice.setWhitespacePolicy(java.util.Optional.empty());
                        Rule first = choice.getComponent().get(0);
                        if (first instanceof HasWhitespacePolicy) {
                            java.util.Optional<Predicate<Integer>> wsp =
                                ((HasWhitespacePolicy) first).getWhitespacePolicy();
                            // get the first WSP that is the referent for comparing others
                            wsp.ifPresent(p -> {
                                // the predicate is defined by a class in @WhitespacePolicy
                                Class<Predicate<Integer>> isWhitespace =
                                    (Class<Predicate<Integer>>) p.getClass();
                                // all items have the same WSP ?
                                if (choice.getComponent().stream().allMatch(t -> {
                                        if (t instanceof HasWhitespacePolicy) {
                                            java.util.Optional<Predicate<Integer>> twsp =
                                                ((HasWhitespacePolicy) t).getWhitespacePolicy();
                                            if (twsp.isPresent()) {
                                                // compare class ; see @WhitespacePolicy
                                                return twsp.get().getClass().equals(isWhitespace);
                                            }
                                        }
                                        return false;
                                    }))
                                { // YES
                                    // => set the common WSP to the choice
                                    choice.setWhitespacePolicy(wsp);
                                    // => unset WSP of each items
                                    choice.setComponent(choice.getComponent().stream().map(cr -> {
                                        // we must create a clone if the rule has a name
                                        // because it may be reused elsewhere (with a different
                                        // inherited WSP)
                                        if (cr.isGrammarField()) {
                                            cr = (Rule) safeCall(cr::clone);
//                                            cr = cloneRule(cr);
                                        }
                                        ((HasWhitespacePolicy) cr)
                                              .setWhitespacePolicy(java.util.Optional.empty());
                                        return cr;
                                    }).collect(Collectors.toList()));
                                }
                            });
                        }
                    }
                    return r;
                },
                r -> r); // apply the changes directly
        });
    }

    /**
     * Adopt a rule of another grammar, that is to say apply the
     * relevant substitutions if any.
     *
     * @param rule The original rule, should be a rule of this
     *      grammar or an inherited grammar.
     * @return The same rule if no modifications were expected,
     *      a new rule otherwise.
     */
    public Rule adopt(Rule rule) {
        if (rule == this.tokenizer) { // || rule == this.mainRule.orElse(null)) {
            return rule;
        }
        if (rule.isGrammarField()) {
            try {
                getClass().getDeclaredField(rule.getName());
                return rule; // rule already in grammar
            } catch (NoSuchFieldException | SecurityException e) {
                return adoption(rule);
            }
        } else {
            throw new IllegalArgumentException("Unable to adopt a rule that is not a grammar field");
        }
    }

    private Rule adoption(Rule rule) {
        return this.adopted.computeIfAbsent(rule, rul ->
            rul.traverse(
                rul,
                new HashSet<>(),
                this.substitutionsResolver,
                r -> (Rule) safeCall(r::clone) // keep intact the original rule
            )
        );
    }

}
