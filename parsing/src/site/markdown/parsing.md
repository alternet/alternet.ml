# Grammars and Parsers

<div class="scroller"></div>
<div class="nopub">
<a href="http://alternet.ml/alternet-libs/parsing/parsing.html">
Published version of this page available HERE</a></div>

**Alternet Parsing** is a nice [Parsing Expression Grammar](https://en.wikipedia.org/wiki/Parsing_expression_grammar) framework that includes an Abstract Syntax Tree builder.

1. [Overview](#overview)
    1. [Features](#features)
1. [Grammars](#grammars)
    1. [The grammar skeleton](#skeleton)
    1. [Tokens](#tokens)
        1. [Enum tokens](#enumTokens)
        1. [Fragment tokens and composed tokens](#fragments)
    1. [Rules](#rules)
        1. [Repeating](#repeating)
        1. [Self rule and deferred rules](#self)
        1. [Proxy rules](#proxy)
        1. [Direct reference](#directRef)
        1. [Handling whitespaces](#whitespaces)
        1. [Extending grammars and overriding rules](#extending)
1. [Grammar with custom token types](#customTypes)
    1. [The target custom classes](#targetClasses)
    1. [Dropping tokens](#drop)
    1. [Mapping tokens](#mapping)
    1. [Separating the raw grammar and the augmented grammar](#augmented)
1. [Parsing](#parsing)
    1. [Parsing an input](#input)
        1. [The “tokenizer” rule](#tokenizer)
        1. [The remainder](#remainder)
    1. [Handlers](#handlers)
    1. [Target data model](#dataModel)
    1. [AST builder](#ast)
        1. [Node builder](#nodeBuilder)
        1. [AST mappers](#mappers)
        1. [Token mappers](#tokenMappers)
        1. [Rule mappers](#ruleMappers)
    1. [A grammar as a token](#grammarToken)
    1. [Extending the mappings](#extendingMapping)
1. [Additional examples](#examples)
1. [Troubleshooting](#troubleshooting)
    1. [Dump](#dump)
    1. [Common issues](#issues)

<a name="overview"></a>

## Overview

<a name="features"></a>

### Features

Some tools are already existing for designing grammars.

**Alternet Parsing** takes the bet that you don't want to learn a new DSL (such as with the well-known ANTLR tool), and therefore allow you to write your grammar in pure Java code.

A grammar in **Alternet Parsing** is just an interface, which allow to avoid pollute the code with Java modifiers (namely "`public static final`") ; the other benefits with interfaces is that you can have multiple inheritence, which allow you to extend by composition new grammars. Overriding a rule is as simple as you expect.

A grammar in **Alternet Parsing** (almost) follows the way rules are written in grammar-formal languages, you are not lost by the order rules are written.

There are 3 main citizens in Alternet Parsing : the [`Grammar`](apidocs/ml/alternet/parser/Grammar.html),
the [`Rule`](apidocs/ml/alternet/parser/Grammar.Rule.html), and the [`Token`](apidocs/ml/alternet/parser/Grammar.Token.html) (a token is also a rule).

Alternet Parsing comes with out-of-the-box convenient features such as :

* no boilerplate code
* easy parsing of enum values and ranges of characters (combination by union and exclusion)
* rules written in Java are following the natural writing of (most) formal grammar languages
* easy extension, combining facilities, and overriding
* can scan strings or streams of characters
* whitespace handling policy set by annotation on a grammar and overridable on any token
* clean separation of grammar and parser
* nice token to custom object mappings facilities
* out-of-the-box abstract syntax tree builder
* etc...

<div style="columns: 2">
<div>
<h3 style="margin: 0">Maven import</h3>
<pre class="prettyprint"><![CDATA[
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-parsing</artifactId>
    <version>1.0</version>
</dependency>]]>
</pre>
</div>
<div style="break-before: column">
<h3>JavaDoc API documentation</h3>
<ul><li><a href="apidocs/index.html">Alternet Parsing</a></li></ul>
<p>Other Alternet APIs :</p>
<ul><li><a href="../apidocs/index.html">Alternet Libs</a></li></ul>
</div>
</div>

<a name="grammars"></a>

## Grammars

In this tutorial, we are writing a grammar that allow to parse a simple mathematical expression like this :

<div class="source"><pre class="prettyprint">
sin( x ) * (1 + var_12)
</pre></div>

The formal grammar definition looks like this, and we intend to write it in pure Java as close as possible to the original :

<div class="source"><pre class="prettyprint">
[01] LBRACKET       ::= '('
[02] RBRACKET       ::= ')'
[03] FUNCTION       ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt'
[04] ADDITIVE       ::= '+' | '-'
[05] MULTIPLICATIVE ::= '*' | '/'
[06] UNDERSCORE     ::= '_'
[07] DIGIT          ::= [0-9]
[08] NUMBER         ::= DIGIT+
[09] UPPERCASE      ::= [A-Z]
[10] LOWERCASE      ::= [a-z]
[11] VARIABLE       ::= (LOWERCASE | UPPERCASE) (LOWERCASE | UPPERCASE | DIGIT | UNDERSCORE)*

[12] Expression     ::= Sum
[13] Value          ::= NUMBER | VARIABLE
[14] Argument       ::= FUNCTION Argument | Value | '(' Expression ')'
[15] Factor         ::= Argument ('^' SignedFactor)?
[16] Product        ::= Factor (MULTIPLICATIVE SignedFactor)*
[17] SignedTerm     ::= ADDITIVE? Product
[18] SignedFactor   ::= ADDITIVE? Factor
[19] Sum            ::= SignedTerm (ADDITIVE Product)*
</pre></div>

Next, we will use our grammar for parsing an expression to our target object tree that can evaluate it.

<a name="skeleton"></a>

### The grammar skeleton

Let's start with the skeleton of our class. As mentioned earlier, a grammar in Alternet Parsing MUST BE an `interface` ; and it has to extend the [`Grammar`](apidocs/ml/alternet/parser/Grammar.html) interface :

```java
package org.example.grammar;

import static ml.alternet.parser.Grammar.*;

public interface Calc extends Grammar {

    // Rules and Tokens will be defined here

    Calc $ = $(); // it MUST BE the last field of the grammar

}
```

The static import contains all the material useful for building your grammar. If you use an IDE you will have it available with autocompletion.

As a requirement, the last field of your grammar (actually the only one so far : `$`) **must** hold an instance of your grammar (generated by [`$()`](apidocs/ml/alternet/parser/Grammar.html#Z:Z:D--)), which will allow to parse the input text (we will talk later about that).

<div class="alert alert-warning" role="alert">
<ul>
<li>The Grammar field will serve to parse the input</li>
<li>All the Rules and Tokens MUST BE declared BEFORE the Grammar field.</li>
</ul>
</div>

<a name="tokens"></a>

### Tokens

First, we just need to enumerate the tokens that are part of the grammar. A [`Token`](apidocs/ml/alternet/parser/Grammar.Token.html) is just a character, or a sequence of characters, that are (the more often) terminal values of the grammar, and to which we give a name. Therefore, if you look at the mathematical expression, we can identify the left bracket as being the character '`(`' ; in our grammar, we simply add a new field for the left bracket, and a second one for the right bracket :

```java
    //    LBRACKET   ::=  '('
    Token LBRACKET = is(  '('  ); // this is how we turn that definition to the Java syntax
    //    RBRACKET   ::=  ')'
    Token RBRACKET = is(  ')'  );
```

You may read it as you write it : the token `LBRACKET` is the character '`(`'.

Since you are defining those fields in a Java `interface`, no need to specify `public static final` on them.

<div class="alert alert-info" role="alert">
<p>Supplementary Unicode characters (whose code points are greater than U+FFFF) can also be defined as tokens :</p>
<div class="source"><pre class="prettyprint linenums">
   Token UNICODE_CHAR_CODE = is(0x1F60E);
   Token UNICODE_CHAR = is("😎".codePointAt(0));

   Token UNICODE_STRING = isOneOf("😎☕🍀");
</pre></div>
</div>

A token can be defined with one of the numerous static methods of [`Grammar`](apidocs/ml/alternet/parser/Grammar.html).

Similarly, we could define a token for the `sin` function that appears in the mathematical expression :

<div class="source"><pre class="prettyprint">
    Token SIN_FUNCTION = is("sin");
</pre></div>

<a name="enumTokens"></a>

#### Enum tokens

We could enumerate the other functions that our parser have to support, but since we are writing our grammar in Java,
we can do better. Instead, we are designing an enum class, and create a token based on its values :

```java
    // FUNCTION ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt'
    enum Function {
        sin, cos, exp, ln, sqrt;
    }
    Token FUNCTION = is(Function.class);
```

Again, you may read it as you write it : the token `FUNCTION` is given by the enum `Function.class`.

Sometimes, a token can't be written as an enum value, because Java names have naming constraints. This is the case in our grammar, we want to write an enum class for "`+`" and "`-`" but they are invalid Java names :

```java
    // ADDITIVE ::= '+' | '-'
    enum Additive {
        +, -; // ⛔ you can't write that in Java
    }
    Token ADDITIVE = is(Additive.class);
```

We have a tool for getting rid of that limitations. First, add the following import :

```java
import static ml.alternet.util.EnumUtil.replace;
```

...and use it in the constructor like this in order to replace the token value by the one supplied :

```java
    // ADDITIVE ::= '+' | '-'
    enum Additive {
        PLUS("+"), MINUS("-");
        Additive(String str) {
            replace(this, s -> str);
        }
    }
    Token ADDITIVE = is(Additive.class);
```

The <code>PLUS</code> value will be replaced by the <code>+</code> value, 
and the <code>MINUS</code> value will be replaced by the <code>-</code> value.
The idea is to have in enum types tokens that are exactly those expected
in the input text. If the input to parse contains "`+`" the token value get
will be `Additive.PLUS`.

Repeat for the multiplicative token :

```java
    // MULTIPLICATIVE ::= '*' | '/'
    enum Multiplicative {
        MULT("*"), DIV("/");
        Multiplicative(String str) {
            replace(this, s -> str);
        }
    }
    Token MULTIPLICATIVE = is(Multiplicative.class);
```

Sometimes, the replacement is generic. Let's consider the XPath grammar : if you look at the [XPath specification](https://www.w3.org/TR/xpath/#axes), you will find 13 axis that contain a "`-`" in their names, which is an invalid character in Java names. Instead, we will write the values with "`_`" and replace them in the constructor like this :

```java
    // # From the XPath specification
    // [6] AxisName   ::=   'ancestor' | 'ancestor-or-self' | 'attribute'
    //                    | 'child' | 'descendant' | 'descendant-or-self'
    //                    | 'following' | 'following-sibling' | 'namespace'
    //                    | 'parent' | 'preceding' | 'preceding-sibling' | 'self'
    public enum Axis {
            ancestor, ancestor_or_self, attribute,
            child, descendant, descendant_or_self,
            following, following_sibling, namespace,
            parent, preceding, preceding_sibling, self;
        Axis() {
            // replace the "_" in the name by a "-"
            replace(this, s -> s.replace('_', '-'));
        }
    }
    Token AxisName = is(Axis.class);
```

When parsing, the longest value if available will be read from the input. That is to say
if the input contains "`ancestor-or-self`" the token value get will be `Axis.ancestor_or_self` and not jut `Axis.ancestor`. It means that the order of the enum values doesn't matter in the enum class. Internally, the tokenizer is smart enough to group commons characters together to avoid testing the same sequence several times.

<a name="fragments"></a>

#### Fragment tokens and composed tokens

Let's go back to our grammar.

The production of variable names (`var_12` in our example) is made of "`_`", digits, and lowercase or uppercase characters. We can define the expected tokens like this :

```java
    //    UNDERSCORE  ::= '_';
    @Fragment
    Token UNDERSCORE = is('_');

    //    UPPERCASE     ::= [A-Z]
    @Fragment
    Token UPPERCASE = range('A', 'Z');

    //    LOWERCASE     ::= [a-z]
    @Fragment
    Token LOWERCASE = range('a', 'z');

    //    DIGIT     ::= [0-9]
    @Fragment
    Token DIGIT = range('0', '9')
            .asNumber();

    //    VARIABLE ::= (LOWERCASE  |  UPPERCASE) (LOWERCASE | UPPERCASE | DIGIT | UNDERSCORE)*
    Token VARIABLE = ( (LOWERCASE).or(UPPERCASE) ).seq(
            ( (LOWERCASE).or(UPPERCASE).or(DIGIT).or(UNDERSCORE) ).zeroOrMore() )
            .asToken();
```

* The production of `Token DIGIT` ends with [`.asNumber();`](apidocs/ml/alternet/parser/Grammar.Rule.html#asNumber--), that is a convenient method to get number values instead of raw strings during parsing.
* The production of `Token VARIABLE` ends with [`.asToken();`](apidocs/ml/alternet/parser/Grammar.Rule.html#asToken--). In fact, we have written our first `Rule` but we want to turn the entire rule in a simple token. We will examine rules in detail in the next section.
* In fact, the `Token VARIABLE` is made of smaller tokens, that are marked as `@Fragment`.

[`@Fragment`](apidocs/ml/alternet/parser/Grammar.Fragment.html) ? A token is not necessary the smallest component of a grammar, but rather the smallest *useful* component of a grammar. In fact, we have convenient `@Fragment`s tokens that are defined here because they may be used elsewhere. But the real useful part is to have a `VARIABLE` produced by the parser, we don't care that that variable name is made of a mix of uppercase, lowercase, digits, and underscore characters (our grammar ensure that it will be the case), we just want a variable name. If we omit the `@Fragment` annotation, each individual token will be produced by the parser and may mask the production of a `VARIABLE` token. However, when entering a token composed of other tokens, the components will be considered as fragments. Setting a `@Fragment` annotation indicates that the target token is not eligible for selection.

We can reuse the previously fragments defined elsewhere by using [`.asToken()`](apidocs/ml/alternet/parser/Grammar.Rule.html#asToken--) if we want a string token, or [`.asNumber()`](apidocs/ml/alternet/parser/Grammar.Rule.html#asNumber--) if we want a number. A token annotated as fragment will be discarded, except if it is used in a rule exposed itself as a token with `.asToken()` or `.asNumber()` : such rule will aggregate the tokens. Be aware that for a rule made of non-fragment tokens, the matched characters will be reported twice !

```java
    // NUMBER  ::= DIGIT+
    Token NUMBER = DIGIT.oneOrMore()
            .asNumber();
```

All non-fragments tokens can be get in the so-called "tokenizer rule" by the 
method [`Calc.$.tokenizer()`](apidocs/ml/alternet/parser/Grammar.html#tokenizer--) (more on this later).

As an alternative, you can combine character tokens ([`CharToken`](apidocs/ml/alternet/parser/Grammar.CharToken.html)) directly :

```java
    //    VARIABLE ::= (LOWERCASE | UPPERCASE) (LOWERCASE | UPPERCASE | DIGIT | UNDERSCORE)*
    Token VARIABLE = range('a', 'z').union('A', 'Z').seq(
            range('a', 'z').union('A', 'Z').union(DIGIT).union('_').zeroOrMore() )
            .asToken();
```

[`CharToken`](apidocs/ml/alternet/parser/Grammar.CharToken.html) contains all the material to define
and combine by inclusion `union()` or exclusion `except()` other ranges of characters.

<a name="rules"></a>

### Rules

Now that we are able to split our input into tokens defined in our grammar, we can specify the [`Rule`s](apidocs/ml/alternet/parser/Grammar.Rule.html) that tell how the input is structured.

A rule can be made of other rules and tokens. They are wired together by a connector that can be the alternative connector ("|" character in most formal grammar languages)  or the sequential connector (a space in most grammars). In java, we have counterparts methods ([`or()`](apidocs/ml/alternet/parser/Grammar.Rule.html#or-ml.alternet.parser.Grammar.Rule...-) and [`seq()`](apidocs/ml/alternet/parser/Grammar.Rule.html#seq-ml.alternet.parser.Grammar.Rule...-)) for that.

```java
    //   Value ::=  NUMBER  |  VARIABLE
    Rule Value   = (NUMBER).or(VARIABLE);
```

We can combine several tokens ; say that we defined the tokens T1, T2, etc :

```java
    // MyRule ::=  T1  |  T2  |  T3  |  T4  |  T5
    Rule MyRule = (T1).or(T2).or(T3).or(T4).or(T5);
```

or more consicely :

```java
    //   MyRule ::= T1 |  T2 | T3 | T4 | T5
    Rule MyRule   = T1.or(T2,  T3,  T4,  T5);
```

The `or()` method is lazy : while parsing, the first rule that will match the input will fulfill the production rule.

<div class="alert alert-error" role="alert">
As a consequence, grammars expressed with :
<div class="source"><pre class="prettyprint">
[1]     Items    ::=      ITEM | Items ',' ITEM
</pre></div>
must be rewrite to :
<div class="source"><pre class="prettyprint">
[1]     Items    ::=      ITEM ( ',' ITEM )*
</pre></div>
</div>

Similarly, for a sequence we write :

```java
    //   MyRule ::=  T1      T2      T3      T4      T5
    Rule MyRule   = (T1).seq(T2).seq(T3).seq(T4).seq(T5);
```

or more consicely :

```java
    //   MyRule ::= T1     T2  T3  T4  T5
    Rule MyRule   = T1.seq(T2, T3, T4, T5);
```

The thing to notice when writing grammars in *Alternet Parsing*, is that you write rules in the same order where they appear in the formal grammar language.

<a name="repeating"></a>

#### Repeating

Rule parts can be combined together, and combined with operators such as * ? or +, that have their counterpart Java methods [`zeroOrMore()`](apidocs/ml/alternet/parser/Grammar.Rule.html#zeroOrMore--), [`optional()`](apidocs/ml/alternet/parser/Grammar.Rule.html#optional--), [`oneOrMore()`](apidocs/ml/alternet/parser/Grammar.Rule.html#oneOrMore--):

```java
    // RuleA ::= T1 *
    Rule RuleA = T1.zeroOrMore();
    // RuleB ::= T1 ?
    Rule RuleB = T1.optional();
    // RuleC ::= T1 +
    Rule RuleC = T1.oneOrMore();

    // RuleD ::= T1 ? | T2 +
    Rule RuleD = T1.optional().or( T2.oneOrMore() );
    // RuleE ::= (T1 ? | T2) +
    Rule RuleE = T1.optional().or(T2).oneOrMore();
```

Notice how we wrote the 2 last rules. Remember that we are expressing rules in the Java language, and that the "`.`" (dot) operator in Java applies a method on the previously given object. Therefore, when we write `.or( T2.oneOrMore() )` the `oneOrMore()` method applies on `T2` only, and when we write `.or(T2).oneOrMore()` this time the `oneOrMore()` method applies to the result of the `or()` connector to which `T2` was connected, that is to say an optional `T1`.

Additional operators are available : [`atLeast()`](apidocs/ml/alternet/parser/Grammar.Rule.html#atLeast-int-), [`atMost()`](apidocs/ml/alternet/parser/Grammar.Rule.html#atMost-int-) and [`bounds()`](apidocs/ml/alternet/parser/Grammar.Rule.html#bounds-int-int-) :

```java
    // RuleX ::= T1{3,}
    Rule RuleX = T1.atLeast(3);
    // RuleY ::= T1{,12}
    Rule RuleY = T1.atMost(12);
    // RuleZ ::= T1{3,12}
    Rule RuleZ = T1.bounds(3, 12);
```

Now we are able to write such rules, and when necessary to turn rules in tokens :

```java
    //    VARIABLE ::= (LOWERCASE | UPPERCASE) (LOWERCASE | UPPERCASE | DIGIT | UNDERSCORE)*
    Token VARIABLE = LOWERCASE.or(UPPERCASE).seq(
            LOWERCASE.or(UPPERCASE, DIGIT, UNDERSCORE).zeroOrMore() )
            .asToken();
```

<a name="self"></a>

#### Self rule and deferred rules

Let's go back to our grammar.

We have all the material to write rules like this :

```java
    // Expression ::= Sum
    Rule Expression = is(Sum); // ⛔ you can't write that in Java because Sum is not yet defined

    //   Argument ::= FUNCTION     Argument  |   Value   |   LBRACKET      Expression  RBRACKET
    Rule Argument =   FUNCTION.seq(Argument).or( Value ).or( LBRACKET.seq( Expression, RBRACKET ) );
                                 // ⛔ you can't write that either
```

Unfortunately, this writing *fails*, because we are in a Java program, and we can't define a field (actually
`Argument`) by being a composition of itself. You can't either define a field (actually `Expression`) being 
composed of other rules that are not yet defined. Actually `Sum` has not yet been defined.
We could define it before, but a `Sum` will be made sooner or later of `Expression`s (indirectly).

<div class="alert alert-info" role="alert">
It's not rare to see grammars defining rules that are referring each others or rules that are referring themselves.
</div>

Note that the following fix also *fail* because the value of the `Sum` field, although correctly referred is
not yet assigned and a `null` value would be passed :

```java
    // Expression ::=      Sum
    Rule Expression = is(Calc.Sum); // ⛔ in fact we have null
```

To fix this, we introduce `$("Sum")` and `$("Argument")` that are placeholders for rules not yet defined 
or defined later in our Java program :

```java
    // Expression ::=    Sum
    Rule Expression = $("Sum");
    //   Argument ::= FUNCTION        Argument  |     Value   |   LBRACKET      Expression  RBRACKET
    Rule Argument =   FUNCTION.seq($("Argument")).or( Value ).or( LBRACKET.seq( Expression, RBRACKET ) );
```

The built-in token [`$self`](apidocs/ml/alternet/parser/Grammar.html#Z:Z:Dself) can be used instead of `$("Argument")` within the
definition of the field `Argument` :

```java
    // Expression ::=    Sum
    Rule Expression = $("Sum");
    //   Argument ::= FUNCTION    Argument  |   Value   |   LBRACKET      Expression  RBRACKET
    Rule Argument =   FUNCTION.seq( $self ).or( Value ).or( LBRACKET.seq( Expression, RBRACKET ) );
```

`$("Sum")` stands for a reference to the following rule declaration :

```java
    //   Sum ::= SignedTerm    (ADDITIVE     Product)*
    Rule Sum =   SignedTerm.seq(ADDITIVE.seq(Product).zeroOrMore());
```

(at this time we assume that `SignedTerm` and `Product` have been too already defined, either directly or with a proxy, you know the recipe).

<a name="proxy"></a>

#### Proxy rules

If you are in trouble by writing `$("Sum")` (which reduce the ease of reading), you can instead define it previously by being a [`Proxy`](apidocs/ml/alternet/parser/Grammar.Proxy.html) rule:

```java
    Proxy Sum = $(); // we expect a definition later
    // Expression ::=    Sum
    Rule Expression = is(Sum); // but we can use it here
    //   Argument ::= FUNCTION    Argument  |   Value   |   LBRACKET      Expression  RBRACKET
    Rule Argument =   FUNCTION.seq( $self ).or( Value ).or( LBRACKET.seq( Expression, RBRACKET ) );
```

...and later in the grammar, you supply its definition when appropriate. You can write it in 3 different flavors, plus the possibility to write it inline :

* The former writing consist on mimicking a static block :

```java
    boolean b1 = 
    // Sum ::= SignedTerm (ADDITIVE Product)*
    Sum.is(
        SignedTerm.seq(ADDITIVE.seq(Product).zeroOrMore())
    );
```

Why do we have a boolean ? In fact we just want to set a value to `Sum`, but since it has
already been defined before, this writing is just a convenient way with Java to supply its value ; since an interface can't have static blocks, we are creating a dummy field `b1`.

* The second writing consist on declaring a `static` method that has the same name of the field, actually `Sum()`, that return the actual rule.

```java
    // Sum ::= SignedTerm (ADDITIVE Product)*
    static Rule Sum() {
        return SignedTerm.seq(ADDITIVE.seq(Product).zeroOrMore());
    }
```

* The latter writing consist on declaring a property that has the same name of the field prepend with $, actually `$Sum`, this property being a **supplier** of the expected rule.

```java
    // Sum ::= SignedTerm (ADDITIVE Product)*
    Supplier<Rule> $Sum = () -> SignedTerm.seq(ADDITIVE.seq(Product).zeroOrMore());
```

* Alternatively, you can also supply the definition *inline*, at the place the rule field is declared. In that case, each field not yet defined in the grammar has to be taken from the grammar class ; in our example the fields `SignedTerm` and `Product` have not yet been defined, and we must refer them as class members : `Calc.SignedTerm` and `Calc.Product`.

Unlike previously, we don't get null values because the supplier is a deferred method that will set the rule definition after all fields initialization :

```java
    // Sum ::= SignedTerm (ADDITIVE Product)*
    Rule Sum = $(() -> 
               Calc.SignedTerm.seq(ADDITIVE.seq(Calc.Product).zeroOrMore())
    );
    // Expression ::=    Sum
    Rule Expression = is(Sum);
    //   Argument ::= FUNCTION    Argument  |   Value   |   LBRACKET      Expression  RBRACKET
    Rule Argument =   FUNCTION.seq( $self ).or( Value ).or( LBRACKET.seq( Expression, RBRACKET ) );
```

The advantage of this writing is that the rule is defined in place, but at the cost of extra syntax.

Choose your style of writing : `$()` with its inline or deferred assignment is interchangeable with `$("foo")` with a normal definition.

Now you should be able to write yourself the remaining rules. The complete code of `Calc` grammar is available on Github. [`Calc`](https://github.com/alternet/alternet.ml/blob/master/parsing/src/test/java/ml/alternet/parser/step1/Calc.java)

<a name="directRef"></a>

#### Direct reference

Now that we are able to write

```java
    // Expression ::=    Sum
    Rule Expression = is(Sum);
```

you might wonder why we didn't write it like that :

```java
    // Expression ::=    Sum
    Rule Expression = Sum; // ⛔ you will have an error
```

The engine won't let you write that and reject such grammar. Rules must hold a specific value, not identical values because if one rule was annotated, it would affect both fields. Therefore it is forbidden.

<a name="whitespaces"></a>

#### Handling whitespaces

In fact, we would like to parse inputs like this :

```
sin (x) * ( 1 + var_12 )
```

So simple in Alternet Parsing ;) with [`@WhitespacePolicy`](apidocs/ml/alternet/parser/Grammar.WhitespacePolicy.html)

By default, whitespaces are left as-is, but if you want to ignore them, simply set this annotation to your grammar :

```java
@WhitespacePolicy(preserve=false, isWhitespace=ml.alternet.scan.JavaWhitespace.class)
public interface Calc extends Grammar {

    // tokens and rules definition here

    Calc $ = $();
}
```

In fact, the values above are the default, therefore, you can simply write :

```java
@WhitespacePolicy
public interface Calc extends Grammar {

    // tokens and rules definition here

    Calc $ = $();
}
```

Another kind of whitespaces are pre-defined, it is [`ml.alternet.scan.XMLWhitespace.class`](apidocs/ml/alternet/scan/XMLWhitespace.html) but you can write your own class, it just has to implement `Predicate<Character>`.

But wait, setting the `@WhitespacePolicy` annotation on the grammar affect every token. It's nice for the `NUMBER` token, but not appropriate for what it is made of, the `DIGIT` tokens. In the grammar, we have to change accordingly on its declaration :

```java
    // DIGIT ::= [0-9]
    @WhitespacePolicy(preserve=true)
    @Fragment Token DIGIT = range('0', '9').asNumber();

    // NUMBER ::= DIGIT+
    Token NUMBER = DIGIT.oneOrMore()
            .asNumber();
```

Now, whereas `NUMBER` inherit the whitespace policy defined by the grammar, `DIGIT` override it with its own requirements. In our grammar we are setting `@WhitespacePolicy(preserve=true)` on other fragments tokens, actually `UPPERCASE`, `LOWERCASE`, and `UNDERSCORE`.

<a name="extending"></a>

#### Extending grammars and overriding rules

It is possible to extend a grammar by adding new rules and tokens, but also
to redefine some of them.

Imagine that we want to write a grammar that is almost the same as the `Calc` grammar,
but with different rules :

* having more built-in functions
* change the tokens `*` and `/` by `×` and `÷`
* disallow lowercase characters in variable names
* surround function arguments with parentheses

Here are the changes :

<div class="source"><pre class="prettyprint">
[03] FUNCTION       ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt' | 'asin' | 'acos'
[05] MULTIPLICATIVE ::= '×' | '÷'
[11] VARIABLE       ::= UPPERCASE (UPPERCASE | DIGIT | UNDERSCORE)*

[14] Argument       ::= FUNCTION LBRACKET Argument RBRACKET | Value | LBRACKET Expression RBRACKET
</pre></div>

To achieve this, the new grammar has just to extend the previous one :

```java
public interface Math extends Calc { // 👈 look here, we extend Calc

    // new rules and tokens here

    Math $ = $();

}
```

Then it has to contain its new definitions.

If the new field have the same name as an existing field in the other grammar, it is replacing it :

```java
    // MULTIPLICATIVE ::= '×' | '÷'
    enum MathMultiplicative {
        MULT("×"), DIV("÷");
        MathMultiplicative(String str) {
            replace(MathMultiplicative.class, this, s -> str);
        }
    }
    // same name than in Calc grammar -> automatic replacement
    Token MULTIPLICATIVE = is(MathMultiplicative.class);

    //   Argument ::= FUNCTION      LBRACKET  Argument RBRACKET   |   Value   |   LBRACKET      Expression  RBRACKET
    // same name than in Calc grammar -> automatic replacement
    Rule Argument =   FUNCTION.seq( LBRACKET, $self,   RBRACKET ).or( Value ).or( LBRACKET.seq( Expression, RBRACKET ) );
```

If the new field has another name, we have to specify with the
[`@Replace`](apidocs/ml/alternet/parser/Grammar.Replace.html) annotation which one it is replacing. Below, everywhere `VARIABLE` appears in the `Calc` grammar, it will be replaced by `UPPERCASE_VARIABLE` in the `Math` grammar (and of course stay unchanged in the `Calc` grammar) :

```java
    // VARIABLE ::= [A-Z] ([A-Z] | DIGIT | '_')*
    @Replace(field="VARIABLE") // because the field below has another name
    Token UPPERCASE_VARIABLE = UPPERCASE.seq(
            UPPERCASE.or(DIGIT, UNDERSCORE).zeroOrMore() )
            .asToken();
```

Sometimes, you have to extend several grammars that may have fields with the same name ;
in this case, you also have to specify the appropriate grammar in the annotation :

```java
    // FUNCTION ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt' | 'asin' | 'acos'
    enum MathFunction {
        sin, cos, exp, ln, sqrt, asin, acos;
    }
    @Replace(grammar=Calc.class, field="FUNCTION")    // replace Calc.FUNCTION
    Token ADVANCED_FUNCTION = is(MathFunction.class);
```

A nice helper tool also allows to extend enums :

```java
    // FUNCTION ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt' | 'asin' | 'acos'
    enum MathFunction {
        asin, acos;
        static {
            EnumUtil.extend(Calc.Function.class); // other values are imported
        }
    }
    @Replace(grammar=Calc.class, field="FUNCTION")    // replace Calc.FUNCTION
    Token ADVANCED_FUNCTION = is(MathFunction.class);
```

You don't need to rewrite the rules where those new definitions are used, they will be replaced in the new grammar. Of course, the original grammar will stay unchanged.

Here is a parsing example with each grammar :

```java
    Calc.$.parse(Scanner.of("sin( x ) * ( 1 + var_12 )"), handler);
    Math.$.parse(Scanner.of("asin( X ) × ( 1 + VAR_12 )"), handler);
```

(more about parsing later)

Sometimes, you may tend to use tokens directly in rules without defining **named** tokens in the grammar. Using named tokens allow global replacements without needing to rewrite each rule that use them. In our grammar, since we have names for the parenthesis, it is very easy to design a new grammar that just change the parenthesis with, say, square brackets :

```java
public interface CalcSquare extends Calc { // 👈 look here, we extend Calc

    Token LBRACKET = is( '[' );
    Token RBRACKET = is( ']' );

    CalcSquare $ = $();

}
```

Substitutions will occur everywhere `LBRACKET` and `RBRACKET` are referred in the new grammar. Now we can parse `sin[ x ] * [1 + var_12]` with the `ClacSquare` grammar.

In the next section we will learn how to build a custom data model.

<a name="customTypes"></a>

## Grammar with custom token types

A **token value** represents the input characters that are parsed. We have seen before that a token value may have various types :

* **a single character** : this is the default behaviour. E.g. `"("`
* **a string** : this is also the default behaviour when a sequence of characters is matched. E.g. `"var_12"`
* **a number** : when the token is defined with [`.asNumber()`](apidocs/ml/alternet/parser/Grammar.Rule.html#asNumber--). E.g. `123.45` (it is also possible to specify the type of the number)
* **an enum value** : when the token is defined with an enum class. E.g. `Axis.ancestor_or_self`

It is also possible to specify in the grammar :

* characters that we want to skip. E.g. `"\\"` where the first `\` stand for an escape character and the second `\` for the data.
* characters rendered as a custom object. E.g. our custom class `Challenge` defined here after.

Let's show how.

In this example, we intend to parse the WWW-Authenticate header sent by an 
HTTP server in a response 401 : « Unauthorized », that indicates how the client
can authenticate :

<div class="source"><pre class="prettyprint">
# Challenge Basic
WWW-Authenticate: Basic realm="FooCorp"
</pre></div>
 
<div class="source"><pre class="prettyprint">
# Challenge OAuth 2.0 après l'envoi d'un token expiré
WWW-Authenticate: Bearer realm="FooCorp", error=invalid_token, error_description="The \"access token\" has expired"
</pre></div>

We will design our `WAuth` grammar and our custom result objects (for the part after "WWW-Authenticate:"), say the `Challenge` class for the global result and the `Parameter` class for each {name, value} pair.

<a name="targetClasses"></a>

### The target custom classes

Our custom classes, first, are very simple ; notice they are agnostic
regarding our future grammar, they are just POJOs :

<div style="columns: 2">
<div>
<pre class="prettyprint linenums"><![CDATA[
public class Parameter {

    public String name;  // e.g. "realm"
    public String value; // e.g. "FooCorp"

    public Parameter(String name, String value) {
        this.name = name;
        this.value = value;
    }
}]]></pre>
</div>
<div style="break-before: column">
<pre class="prettyprint linenums"><![CDATA[
public class Challenge {

    public String scheme; // e.g. "Basic"
    public List<Parameter> parameters;

    public Challenge(String scheme, List<Parameter> parameters) {
        this.scheme = scheme;
        this.parameters = parameters;
    }

}]]></pre>
</div>
</div>

### The WAuth grammar

Now, the grammar (using the "Augmented BNF" syntax, see §2.1 in [RFC-2616](https://www.ietf.org/rfc/rfc2616.txt)) :

<div class="source"><pre class="prettyprint">
&#35; from RFC-2617 (HTTP Basic and Digest authentication)

challenge      = auth-scheme 1*SP 1#auth-param
auth-scheme    = token
auth-param     = token "=" ( token | quoted-string )

&#35; from RFC-2616 (HTTP/1.1)

token          = 1*&lt;any CHAR except CTLs or separators&gt;
separators     = "(" | ")" | "&lt;" | "&gt;" | "@"
               | "," | ";" | ":" | "\" | &lt;"&gt;
               | "/" | "[" | "]" | "?" | "="
               | "{" | "}" | SP | HT
quoted-string  = ( &lt;"&gt; *(qdtext | quoted-pair ) &lt;"&gt; )
qdtext         = &lt;any TEXT except &lt;"&gt;&gt;
quoted-pair    = "\" CHAR
</pre></div>

<a name="drop"></a>

### Dropping tokens

Let's start the Java grammar :

```java
public interface WAuth extends Grammar {

    @Fragment Token SEPARATORS = isOneOf("()<>@,;:\\\"/[]?={} \t"); // '\', '"' and TAB are escaped with \ in Java
    @Fragment Token CTRLS = range(0, 31).union(127); // octets 0 - 31 and DEL (127)

    @WhitespacePolicy(preserve=true)
    @Fragment Token TOKEN_CHAR = isNot(SEPARATORS, CTRLS);

    @WhitespacePolicy
    Token TOKEN = TOKEN_CHAR.oneOrMore()   // we have a Token called TOKEN, why not...
            .asToken();

    @Fragment Token DOUBLE_QUOTE = is('"');
    @Fragment Token BACKSLASH = is('\\')
            .drop();

    @WhitespacePolicy(preserve=true)
    @Fragment Token QuotedPair = BACKSLASH.seq( $any ).asToken();

    // other tokens here

    WAuth $ = $();

}
```

We defined various tokens as ranges of characters, and tokens made of other tokens.

* [`$any`](apidocs/ml/alternet/parser/Grammar.html#Z:Z:Dany) is a built-in token that matches any character.
* Remember that as soon as you use modifiers such as [`.oneOrMore()`](apidocs/ml/alternet/parser/Grammar.Rule.html#oneOrMore--) or combiners such as [`.seq()`](apidocs/ml/alternet/parser/Grammar.Rule.html#seq-ml.alternet.parser.Grammar.Rule...-) you get a `Rule`, but you can turn it back to a `Token` with [`.asToken()`](apidocs/ml/alternet/parser/Grammar.Rule.html#asToken--).
* The `BACKSLASH` token is dropped with [`.drop()`](apidocs/ml/alternet/parser/Grammar.Rule.html#drop--). It means that the handler won't ever receive this token, but the grammar ensure that the following character is properly escaped, and that the relevant value received is properly stripped from the `\` character.

Conversely, the `DOUBLE_QUOTE` token is not dropped at the token definition, because sometimes it stands for a delimiter (and in that case it has to be removed), and some other times it stands for itself (as `"`). Therefore, we use [`.drop()`](apidocs/ml/alternet/parser/Grammar.Rule.html#drop--) at the places we don't want to get `"` as data :

```java
    @WhitespacePolicy(preserve=true)
    @Fragment Token QdText = isNot(DOUBLE_QUOTE);

    Token QuotedString = DOUBLE_QUOTE.drop().seq( // " is a separator
            QuotedPair.or(QdText).zeroOrMore(),
            DOUBLE_QUOTE.drop())                  // " is a separator too
        .asToken();

    Token ParameterValue = TOKEN.or(QuotedString).asToken();

    @Fragment Token EQUAL = is('=');
```

<a name="mapping"></a>

### Mapping tokens

Instead of having `String`s or `Numbers`s, we expect having our types (yes, fields and types may have the same name) :

| Field name      | Type expected     |
| --------------- |:-----------------:|
| `Parameter`     | `Parameter`       |
| `Parameters`    | `List<Parameter>` |
| `Challenge`     | `Challenge`       |

We already used [`.asNumber()`](apidocs/ml/alternet/parser/Grammar.Rule.html#asNumber--) for getting a number value and [`.asToken()`](apidocs/ml/alternet/parser/Grammar.Rule.html#asToken--) for turning a rule to a token ; now we will use [`.asToken(mapper)`](apidocs/ml/alternet/parser/Grammar.Rule.html#asToken-java.util.function.Function-) to turn the tokens of a rule to a custom object. Actually, we expect our `Parameter` object, and we have a special type for the counterpart definition : [`TypedToken<T>`](apidocs/ml/alternet/parser/Grammar.TypedToken.html), in our case [`TypedToken<Parameter>`](apidocs/ml/alternet/parser/Grammar.TypedToken.html). The mapper is just a function that takes as argument the `List` of tokens parsed by the rule and that returns a value that can be consumed by the enclosing rule.

Below, the rule will match `aName = aValue` in 3 tokens (`aName` then `=` then `aValue`), and we produce a `Parameter` object with the **first** and the **last** tokens because the `=` token is useless ; we could use [`.drop()`](apidocs/ml/alternet/parser/Grammar.Rule.html#drop--) on it, but it wouldn't change anything because we are just ignoring it :

```java
    //                    Parameter ::= TOKEN     EQUAL  ParameterValue
    TypedToken<Parameter> Parameter =   TOKEN.seq(EQUAL, ParameterValue)  // e.g. "aName = aValue"
        .asToken(tokens ->
            new Parameter(
                tokens.getFirst().getValue(), // TOKEN          e.g. "aName"
                tokens.getLast().getValue()   // ParameterValue e.g. "aValue"
        ));
```

Similarly, a list of parameters –`List<Parameter>`– can be produced easily, but since we don't know how many tokens will be available in that list, we are streaming the list of tokens. Since the <tt>Parameter<b>s</b></tt> rule is made of `Parameter` rules that create new instances of our `Parameter` POJO (yes, we have the same name for a rule and our POJO), we can safely cast the token value. Below, instead of dropping the `COMMA` token, as an alternative we filter it while processing the stream (but both techniques would work) :

```java
    @WhitespacePolicy
    @Fragment Token COMMA = is(',');

    //                          Parameters ::= Parameter    (COMMA     Parameter?)*
    TypedToken<List<Parameter>> Parameters =   Parameter.seq(COMMA.seq(Parameter.optional()).zeroOrMore())
        .asToken(tokens ->
            tokens.stream()
                .filter(t -> t.getRule() != COMMA)   // drop ","
                .map(t -> (Parameter) t.getValue())  // extract the value as a Parameter object
                .collect(toList())                   // collect to List<Parameter>
        );
```

It's worth to mention that the tokens available in the list are instances of [`TokenValue<V>`](apidocs/ml/alternet/parser/EventsHandler.TokenValue.html) from which you can extract the rule/token that matched the input ([`.getRule()`](apidocs/ml/alternet/parser/EventsHandler.RuleEvent.html#getRule--)) and the actual value ([`.getValue()`](apidocs/ml/alternet/parser/EventsHandler.TokenValue.html#getValue--)). You can also retrieve the type of the value or set a new value.

Finally, the production of the `Challenge` is obvious, and it is marked as the main rule of our grammar (more about `@MainRule` on the next section) :

```java
    //                    Challenge ::= TOKEN     Parameters
    @MainRule
    TypedToken<Challenge> Challenge =   TOKEN.seq(Parameters)
        .asToken(tokens -> new Challenge(
            tokens.removeFirst().getValue(), // TOKEN,      as String
            tokens.removeFirst().getValue()) // Parameters, as List<Parameter>
        );
```

In a nutshell, the line #3 that defines the rule refers to other rules or tokens. The lines #4 to #7 that create an object refers to other objects previsouly created.

Now we can create a parser (outside of our grammar), to get optionally our challenge (it is optional because the parsing may fail) :

```java
public class WAuthParser {

    public Challenge parse(String input) {
        Optional<Challenge> result = new NodeBuilder<Challenge>(WAuth.$).build(input, true);
        return result.get(); // or throw an error
    }
}
```

We are using the [`NodeBuilder<T>`](apidocs/ml/alternet/parser/ast/NodeBuilder.html) class that can supply an instance of `T` if the parsing succeeds, actually our POJO `Challenge`. The boolean parameter indicates when set to `true` to consume all the characters from the input (it would be a failure if some characters remain at the end).


<a name="augmented"></a>

### Separating the raw grammar and the augmented grammar

In the previous example, we made an augmented grammar as a whole ("augmented" means augmented with our custom classes). Imagine that somebody want to use our grammar but using its own classes ; he had to rewrite all the augmented rules + its own mappings, whereas it would be better if he had to write only its own mappings. We need a clean separation of the "raw" grammar that only deal with scalar and enum values, and the "augmented" grammar that uses our objects.

<div class="alert alert-info" role="alert">
A "raw grammar" should supply only scalar values (Strings, Numbers, booleans) and enum values, but sometimes basic objects such as <code>LocalDate</code> or <code>URI</code> might be acceptable.
</div>

First, let's clean the grammar in order to get back a raw grammar :

```java
public interface WAuth extends Grammar {

    // all the tokens are defined here as shown previously...

    // Parameter ::= TOKEN     EQUAL  ParameterValue
    Rule Parameter = TOKEN.seq(EQUAL, ParameterValue);

    // Parameters ::= Parameter (COMMA Parameter?)*
    Rule Parameters = Parameter.seq(COMMA.seq(Parameter.optional()).zeroOrMore());

    // Challenge ::= TOKEN     Parameters
    @MainRule
    Rule Challenge = TOKEN.seq(Parameters);

    WAuth $ = $();

}
```

Now we can extend it in a second grammar with our own mappings :

```java
public interface WAuthAugmented extends WAuth {

    //            augment Parameter from the rule of the raw grammar
    TypedToken<Parameter> Parameter = WAuth.Parameter
        .asToken(tokens ->
            new Parameter(
                tokens.getFirst().getValue(), // TOKEN
                tokens.getLast().getValue()   // ParameterValue
        ));

    //                  augment Parameters from the rule of the raw grammar
    TypedToken<List<Parameter>> Parameters = WAuth.Parameters
        .asToken(tokens ->
            tokens.stream()
                // drop ","
                .filter(t -> t.getRule() != COMMA)
                // extract the value as a Parameter
                .map(t -> (Parameter) t.getValue())
                .collect(toList())
        );

    @MainRule
    //            augment Challenge from the rule of the raw grammar
    TypedToken<Challenge> Challenge = WAuth.Challenge
        .asToken(tokens -> new Challenge(
            tokens.removeFirst().getValue(),
            tokens.removeFirst().getValue())
        );

    WAuthAugmented $ = $();

}
```

That way your users may use either the raw grammar, the augmented grammar with your classes, or their own augmented grammar with their own classes.

It is certainly a good practice to follow this pattern to make your grammar really reusable.

<a name="parsing"></a>

## Parsing

Now that your grammar is well designed, you are able to parse your data.

<a name="input"></a>

### Parsing an input

An input can be parsed :

* on a special rule that stands for the main rule (this is the more useful case)
* on any of the token rules
* on a given rule

We have a grammar interface, `Calc`, and a field which is an instance of that grammar, `Calc.$`.
From that instance, we can parse with [`Calc.$.parse()`](apidocs/index.html?ml/alternet/parser/Grammar.Rule.html) an input on a given rule, actually the `Calc.Expression` rule :

```java
    Handler handler = ...;
    String input = "sin(x)*(1+var_12)";
    Calc.$.parse(
        Scanner.of(input),  // scan a String or a character stream
        handler,            // more about handlers later
        Calc.Expression,    // main rule in our grammar
        true);              // true to expect consume all the input
```

The [Handler](apidocs/ml/alternet/parser/Handler.html) is the component that accept low-level parsing events (more about that on see next section). We will see that out-of-the-box sophisticated handler implementations are available.

If we consider that the field `Expression` is the [main rule](apidocs/ml/alternet/parser/Grammar.MainRule.html) in our grammar, we can annotate it like this :

```java
    @MainRule
    Rule Expression = SignedTerm.seq(SumOp);
```

If the `parse()` method is invoked without specifying any rule, that **main rule** will be used :

```java
    Handler handler = ...;
    String input = "sin(x)*(1+var_12)";
    Calc.$.parse(Scanner.of(input), handler, true);
```

<a name="tokenizer"></a>

#### The "tokenizer" rule

A special rule is available in every grammar, it is the rule that takes all the tokens that are not fragment :

```java
    Handler handler = ...;
    String input = "sin(x)*(1+var_12)";
    Calc.$.parse(Scanner.of(input), handler, Calc.tokenizer());
```

The tokenizer will match tokens regardlesss the structure, therefore inputs badly structured can be parsed,
but you will be sure that the input is made of valid tokens.

<a name="remainder"></a>

#### The remainder

Sometimes, you want to parse your data until no more matching is possible :

```java
    Handler handler = ...;
    String input = "sin(x)*(1+var_12) is an expression";
    Scanner scanner = Scanner.of(input);
    Calc.$.parse(
        scanner,
        handler,
        false);  // false to stop parsing when nothing matches any longer
```

Then, the [`scanner`](http://alternet.ml/alternet-libs/scanner/apidocs/ml/alternet/scan/Scanner.html) instance contains the remainder, ready for further processing with whatever ; you can also extract the remainder as a character `Reader` or a `String`.

<a name="handlers"></a>

### Handlers

Alternet Parsing comes with out-of-the-box [`Handler`s](apidocs/ml/alternet/parser/Handler.html) that can receive the result of the parsing, that will be handy for processing that result :

* [`TreeHandler`](apidocs/ml/alternet/parser/handlers/TreeHandler.html) : low-level API
* [`NodeBuilder`](apidocs/ml/alternet/parser/ast/NodeBuilder.html) : high-level API for building an AST made of homogeneous nodes.
* [`ValueBuilder`](apidocs/ml/alternet/parser/ast/ValueBuilder.html) : high-level API for building an heterogeneous AST.

The [AST package](apidocs/ml/alternet/parser/ast/package-summary.html) contains helper classes to build
an Abstract Syntax Tree (**AST**) while parsing.

<a name="dataModel"></a>

### Target data model

We have seen [previously](#customTypes) that we are already able to produce custom types. Sometimes, we need more contextual data to be able to produce typed data ; typically, if you intend to build an homogeneous AST, you are in the right place.

Let's go back to our `Calc` grammar. We intend to compute expressions, and therefore build a target data model with the help of the [`Expression<T,C>`](apidocs/ml/alternet/parser/ast/Expression.html) class (of course you can use your own class instead) :

* where `T` is the type of the result of the computation, in our case it will be a `Number`
* and `C` the context of the evaluation ; according to the grammar, it may hold all the necessary for computing an expression, for example supply built-in and custom functions, namespace mappers, bound variables to their name, etc. In our example we just need to resolve variable names, and the context will be as simple as a `Map<String,Number>`.

```java
/**
 * The user data model is a tree of expressions.
 */
public interface NumericExpression extends Expression<Number, Map<String,Number>> { }
```

From this base, we are defining every kind of `NumericExpression` expected :

<div class="tabs">

<input class="tab1" id="tabConstant" type="radio" name="dataModel" checked="checked"><label for="tabConstant">Constant</label></input>
<input class="tab2" id="tabVariable" type="radio" name="dataModel"><label for="tabVariable">Variable</label></input>
<input class="tab3" id="tabExponent" type="radio" name="dataModel"><label for="tabExponent">Exponent</label></input>
<input class="tab4" id="tabFunction" type="radio" name="dataModel"><label for="tabFunction">Function</label></input>
<input class="tab5" id="tabTerm" type="radio" name="dataModel"><label for="tabTerm">Term</label></input>
<input class="tab6" id="tabSum" type="radio" name="dataModel"><label for="tabSum">Sum</label></input>
<input class="tab7" id="tabProduct" type="radio" name="dataModel"><label for="tabProduct">Product</label></input>

<div class="tab1">
<ul><li>A constant expression wraps a number value :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
public class Constant implements NumericExpression {

    Number n;

    public Constant(Number n) {
        this.n = n;
    }

    @Override
    public Number eval(Map<String, Number> variables) {
        return n;
    }

}]]></pre></div>

<div class="tab2">
<ul><li>A variable expression wraps a variable name and can be resolved with the context :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
public class Variable implements NumericExpression {

    String name;

    public Variable(String name) {
        this.name = name;
    }

    @Override
    public Number eval(Map<String, Number> variables) {
        return variables.get(this.name);
    }

}]]></pre></div>

<div class="tab3">
<ul><li>An exponent expression is made of a base and an exponent :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
public class Exponent implements NumericExpression {

    NumericExpression base;
    NumericExpression exponent;

    public Exponent(NumericExpression base, NumericExpression exponent) {
        this.base = base;
        this.exponent = exponent;
    }

    @Override
    public Number eval(Map<String, Number> variables) {
        Number base = this.base.eval(variables);
        Number exponent = this.exponent.eval(variables);
        return Math.pow(base.doubleValue(), exponent.doubleValue());
    }
}]]></pre></div>

<div class="tab4">

<input class="tab1" id="tabFuncModel" type="radio" name="dataModelFunc" checked="checked"><label for="tabFuncModel">Model</label></input>
<input class="tab2" id="tabFuncGrammar" type="radio" name="dataModelFunc"><label for="tabFuncGrammar">Grammar</label></input>

<div class="tab1">
<ul><li>A function expression embeds an evaluable function :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
public class Function implements NumericExpression {

    NumericExpression argument;
    EvaluableFunction function;

    public Function(EvaluableFunction function, NumericExpression argument) {
        this.function = function;
        this.argument = argument;
    }

    @Override
    public Number eval(Map<String, Number> variables) {
        Number arg = this.argument.eval(variables);
        return function.eval(arg);
    }

}]]></pre>

<pre class="prettyprint linenums"><![CDATA[
public interface EvaluableFunction {

    Number eval(Number value);
}]]></pre>
</div>

<div class="tab2">
<p>In the <code>Calc</code> grammar, we enhance the enum class accordingly :</p>
<pre class="prettyprint linenums"><![CDATA[
    // FUNCTION ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt'
    enum Function implements EvaluableFunction {
        sin {
            @Override
            public java.lang.Number eval(java.lang.Number value) {
                return Math.sin(value.doubleValue());
            }
        },
        cos {
            @Override
            public java.lang.Number eval(java.lang.Number value) {
                return Math.cos(value.doubleValue());
            }
        },
        exp {
            @Override
            public java.lang.Number eval(java.lang.Number value) {
                return Math.exp(value.doubleValue());
            }
        },
        ln {
            @Override
            public java.lang.Number eval(java.lang.Number value) {
                return Math.log(value.doubleValue());
            }
        },
        sqrt {
            @Override
            public java.lang.Number eval(java.lang.Number value) {
                return Math.sqrt(value.doubleValue());
            }
        };
    }
    Token FUNCTION = is(Function.class);]]></pre></div>
</div>

<div class="tab5">
<ul><li>A term is an expression bound with an operation that appears in sums or products :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
public class Term<T> implements NumericExpression {
    T operation;
    NumericExpression term;

    public Term(T operation, NumericExpression term) {
        this.operation = operation;
        this.term = term;
    }

    @Override
    public Number eval(Map<String, Number> variables) {
        return (this.operation == Additive.MINUS) ?
            - term.eval(variables).doubleValue() :
            + term.eval(variables).doubleValue();
        // alone, a term can have +a or -a but can't have *a or /a
    }
}]]></pre></div>

<div class="tab6">
<pre class="prettyprint linenums"><![CDATA[
public class Sum implements NumericExpression {

    List<Term<Additive>> arguments = new ArrayList<>();

    public Sum(List<Term<Additive>> arguments) {
        this.arguments = arguments;
    }

    @Override
    public Number eval(Map<String, Number> variables) {
        double sum = this.arguments.stream()
            .mapToDouble(t -> (t.operation == Additive.MINUS) ?
                        - t.term.eval(variables).doubleValue()
                    :   + t.term.eval(variables).doubleValue())
            .sum();
        return sum;
    }

}]]></pre></div>

<div class="tab7">
<pre class="prettyprint linenums"><![CDATA[
public class Product implements NumericExpression {

    List<Term<Multiplicative>> arguments = new ArrayList<>();

    public Product(List<Term<Multiplicative>> arguments) {
        this.arguments = arguments;
    }

    @Override
    public Number eval(Map<String, Number> variables) {
        double product = this.arguments.stream()
            .reduce(1d,
                (val, term) -> term.operation == Multiplicative.DIV ?
                        val / term.term.eval(variables).doubleValue()
                    :   val * term.term.eval(variables).doubleValue(),
                (t1, t2) -> t1 * t2);
        return product;
    }

}]]></pre></div>
</div>

Here is the complete code of [`NumericExpression`](https://github.com/alternet/alternet.ml/blob/master/parsing/src/test/java/ml/alternet/parser/step4/NumericExpression.java)

<a name="ast"></a>

### AST builder

As mentionned previously, a node builder can be designed from our `Calc` grammar instance.

We just need to map every token to the relevant class, and every rule to the relevant class. For that purpose, we will use [`TokenMapper<T>`](apidocs/ml/alternet/parser/ast/TokenMapper.html) and [`RuleMapper<T>`](apidocs/ml/alternet/parser/ast/RuleMapper.html) where `<T>` is our `<NumericExpression>`. The simplest way to create such mappers is to enumerate the mappings :

```java
    enum CalcTokens implements TokenMapper<NumericExpression> {
        FUNCTION,    // implementation code supplied later
        RAISED,
        ADDITIVE,
        MULTIPLICATIVE,
        NUMBER,
        VARIABLE
    }

    enum CalcRules implements RuleMapper<NumericExpression> {
        Sum,    // implementation code supplied later
        Product,
        Factor
    }
```

You might notice that the name of the token mappers as well as the name of the rule mappers are the same than those defined in the `Calc` grammar, this is why the mapping will work. You also might notice that some rules in the grammar are not defined here, which means that their tokens will be simply pass to the enclosing rule. Tokens marked as `@Fragment` as well as tokens that are aggregated (enclosed in a rule exposed as a token with `.asToken(...)`) are not present either in the mappings.

<a name="nodeBuilder"></a>

#### Node builder

From this base, we are able to build our node builder :

```java
public class ExpressionBuilder extends NodeBuilder<NumericExpression> {

    public ExpressionBuilder() {
        super(Calc.$);
        setTokenMapper(CalcTokens.class);
        setRuleMapper(CalcRules.class);
    }

}
```

And run the result :

```java
    Map<String, Number > variables = new HashMap<>();
    variables.put("x", 1.0);
    variables.put("var_12", 10.0);

    Optional<NumericExpression> exp = new ExpressionBuilder().build("sin( x )* (1 + var_12)", true);
    Number result = exp.get().eval(variables);
```

<a name="mappers"></a>

#### AST mappers

What is missing is the implementation of each mapper `CalcTokens` and `CalcRules`.

<div style="columns: 2">
<div>
<p>The signature of a token mapper is :</p>
<div class="source"><pre class="prettyprint"><![CDATA[
    Node transform(
            ValueStack<Value<Node>> stack,
            TokenValue<?> token,
            Deque<Value<Node>> next);]]>
</pre></div>
</div>
<div style="break-before: column">
<p>The signature of a rule mapper is :</p>
<div class="source"><pre class="prettyprint"><![CDATA[
    Node transform(
            ValueStack<Value<Node>> stack,
            Rule rule,
            Deque<Value<Node>> args);
]]>
</pre></div>
</div>
</div>

They are both very similar, and apply to the `<Node>` type, which is in our builder the `<NumericExpression>` type :

* The first parameter contains the **stack** of raw items encountered so far. "Raw" means that they are not yet transformed since the production is performed bottom up.
The more often the stack doesn't serve the transformation but sometimes it may help to peek the last previous item.
* The second parameter is the current **token** / **rule**.
* The last parameter contains all the values that are either the **arguments** of the rule to transform, or all the values coming **next** from the token to transform in the context of its enclosed rule. That values can be raw values or transformed values, according to how you process them individually.
In fact `Value<NumericExpression>` is a wrapper around an object that can be either the raw token or a `NumericExpression`. You are free to supply tokens left as-is or transformed ones, and to get the raw value with [`.getSource()`](apidocs/ml/alternet/parser/util/Dual.html#getSource--) or the transformed one with [`.getTarget()`](apidocs/ml/alternet/parser/util/Dual.html#getTarget--).

For example, if a rule defines a comma-separated list of digits such as "`1,2,3,4`", that the input is "<code>i=<b>1,2,3,4</b>;</code>", and that the current **token** is "`2`", then the **next** elements are "`,3,4`" (note that `;` is outside of the rule considered and **not** within the next elements) and the stack is "`i=1,`" (note that "`i=`" are tokens outside of the scope of the rule considered, but **present** in the stack). Some elements may be consumed during the production of the target node.

In our enum classes `CalcTokens` and `CalcRules`, we are creating a constructor that accept the mapper to which the transformation will be delegated. This let us using a lambda in our enum values :

<div class="tabs">

<input class="tab1" id="tabCalcRules" type="radio" name="calcMappers" checked="checked"><label for="tabCalcRules">CalcRules</label></input>
<input class="tab2" id="tabCalcTokens" type="radio" name="calcMappers" checked="checked"><label for="tabCalcTokens">CalcTokens</label></input>

<div class="tab1"><pre class="prettyprint linenums"><![CDATA[
enum CalcRules implements RuleMapper<NumericExpression> {

    Sum( (stack, rule, args) -> {} ),    // implementation code supplied later
    Product( (stack, rule, args) -> {} ),
    Factor( (stack, rule, args) -> {} );

    RuleMapper<NumericExpression> mapper;

    CalcRules(RuleMapper<NumericExpression> mapper) { // pass the mapper to the constructor
        this.mapper = mapper;
    }

    @Override
    public NumericExpression transform(
            ValueStack<Value<NumericExpression>> stack,
            Rule rule,
            Deque<Value<NumericExpression>> args)
    {
        return this.mapper.transform(stack, rule, args); // delegate the mapping
    }

}]]></pre></div>

<div class="tab2"><pre class="prettyprint linenums"><![CDATA[
enum CalcTokens implements TokenMapper<NumericExpression> {

    FUNCTION( (stack, token, next) -> {} ),    // implementation code supplied later
    RAISED( (stack, token, next) -> {} ),
    ADDITIVE( (stack, token, next) -> {} ),
    MULTIPLICATIVE( (stack, token, next) -> {} ),
    NUMBER( (stack, token, next) -> {} ),
    VARIABLE( (stack, token, next) -> {} );

    TokenMapper<NumericExpression> mapper;

    CalcTokens(TokenMapper<NumericExpression> mapper) { // pass the mapper to the constructor
        this.mapper = mapper;
    }

    @Override
    public NumericExpression transform(
        ValueStack<Value<NumericExpression>> stack,
        TokenValue<?> token,
        Deque<Value<NumericExpression>> next)
    {
        return this.mapper.transform(stack, token, next); // delegate the mapping
    }

}]]></pre>
</div>

</div>

<a name="tokenMappers"></a>

#### Token mappers

Now we can focus on the mappers inside `enum CalcTokens`, starting with the simplest ones :

<div class="tabs">

<input class="tab1" id="tabVARIABLE" type="radio" name="tokenMappers" checked="checked"><label for="tabVARIABLE">VARIABLE</label></input>
<input class="tab2" id="tabNUMBER" type="radio" name="tokenMappers"><label for="tabNUMBER">NUMBER</label></input>
<input class="tab3" id="tabFUNCTION" type="radio" name="tokenMappers"><label for="tabFUNCTION">FUNCTION</label></input>
<input class="tab4" id="tabRAISED" type="radio" name="tokenMappers"><label for="tabRAISED">RAISED</label></input>
<input class="tab5" id="tabMULTIPLICATIVE" type="radio" name="tokenMappers"><label for="tabMULTIPLICATIVE">MULTIPLICATIVE</label></input>
<input class="tab6" id="tabADDITIVE" type="radio" name="tokenMappers"><label for="tabADDITIVE">ADDITIVE</label></input>

<div class="tab1">
<ul><li>the <code>VARIABLE</code> <b>token</b> is just a sequence of characters, we write the <code>VARIABLE</code> <b>mapper</b> (with the same name as the token) that create a <code>Variable</code> <b>instance</b> from our data model with that token :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
    VARIABLE( (stack, token, next) -> {
        // mapper for : Token VARIABLE = ( (LOWERCASE).or( <etc...> ).asToken()
        String name = token.getValue();
        return new Variable(name);
    });]]></pre>
</div>

<div class="tab2">
<ul><li>the <code>NUMBER</code> token is produced in the grammar with <a href="apidocs/ml/alternet/parser/Grammar.Rule.html#asNumber--"><code>.asNumber()</code></a>, we write the <code>NUMBER</code> mapper that create a <code>Constant</code> instance from our data model with that number token :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
    NUMBER( (stack, token, next) -> {
        // mapper for : Token NUMBER = DIGIT.oneOrMore().asNumber();
        Number n = token.getValue(); // thanks to ".asNumber();"
        return new Constant(n);
    }),]]></pre>
</div>

<div class="tab3">
<ul><li>the <code>FUNCTION</code> token is produced in the grammar with the <code>Calc.Function</code> enum class, we write the <code>FUNCTION</code> mapper that create a <code>Function</code> instance from our data model with that enum value token. In our grammar, functions are taking a single argument, we can retrieve it as the next following value :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
    FUNCTION( (stack, token, next) -> {
        // mapper for : Token FUNCTION = is(Calc.Function.class);
        // e.g.   sin  x
        //   function  argument
        Operation.Function function = token.getValue();            // e.g.   Calc.Function.sin
        NumericExpression argument = next.pollFirst().getTarget(); // e.g.   Expression.Variable("x")
        return new Function(function, argument);
    }),]]></pre>
</div>

<div class="tab4">
<ul><li>the <code>RAISED</code> token is a little special because it uses the previous item and the next one. Unfortunately, the previous item (which is in the stack) is not yet transformed to a <code>NumericExpression</code>, therefore we can't do something useful in that mapper : we must delegate the mapping to the enclosed rule mapper <code>Factor</code> (see later) :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
    RAISED( (stack, token, next) -> {
        // mapper for : Token RAISED = is('^');
        // e.g. a ^ b
        return null; // we don't know how to process it here => keep the source value
    }),]]></pre>
</div>

<div class="tab5">
<ul><li>the <code>MULTIPLICATIVE</code> token is produced in the grammar with the <code>Calc.Multiplicative</code> enum class. It is transformed to a <code>Term&lt;Calc.Multiplicative&gt;</code> ; the grammar say that the token is <b>always</b> followed by an argument :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
    MULTIPLICATIVE( (stack, token, next) -> {
        // mapper for : Token MULTIPLICATIVE = is(Multiplicative.class);
        // e.g. a * b
        Multiplication op = token.getValue(); // * | /
        // * is always followed by an argument
        NumericExpression arg = next.pollFirst().getTarget(); // b argument
        Term<Multiplication> term = new Term<>(op, arg);
        return term;
    }),]]></pre>
</div>

<div class="tab6">
<ul><li><code>Calc.Additive</code> is slightly different because it always appears alone in <code>SignedTerm</code> and <code>SignedFactor</code>. Both rules are wrapping the additive term <b>within</b> an optional rule, which means that <b>nothing</b> comes next in that rule.</li></ul>

<div style="columns: 2">
<div>
<p>Aside, <code>Product</code> or <code>Factor</code> are after the optional rule, <b>not after</b> the <code>ADDITIVE</code> term, which means that we have to consider just <code>ADDITIVE?</code> as the enclosed rule.</p>
</div>
<div style="break-before: column">
<div class="source"><pre class="prettyprint">
SignedTerm   ::= ADDITIVE? Product
SignedFactor ::= ADDITIVE? Factor
</pre></div>
</div>
</div>

<div style="columns: 2">
<div>
<p>Conversely, in a <code>Sum</code>, the <code>ADDITIVE</code> term is <b>always</b> followed by a term.</p>
</div>
<div style="break-before: column">
<div class="source"><pre class="prettyprint">
Sum ::= SignedTerm (ADDITIVE Product)*
</pre></div>
</div>
</div>

<p>A partial dump allow to visualize the relevant rules :</p>
<input class="tab1" id="tabSTermDump" type="radio" name="partialDump" checked="checked"><label for="tabSTermDump">SignedTerm</label></input>
<input class="tab2" id="tabSFactorDump" type="radio" name="partialDump"><label for="tabSFactorDump">SignedFactor</label></input>
<input class="tab3" id="tabSumDump" type="radio" name="partialDump"><label for="tabSumDump">Sum</label></input>
<input class="tab4" id="tabCodeDump" type="radio" name="partialDump"><label for="tabCodeDump">Java code</label></input>
<input class="tab5" id="tabWhyDump" type="radio" name="partialDump"><label for="tabWhyDump">Why ?</label></input>
<div class="tab1">
<p><code>ADDITIVE</code> <b>is not</b> followed by <code>Product</code></p>
<div class="source"><pre class="prettyprint" style="line-height: 17px">
SignedTerm
┣━━ ADDITIVE?
┃   ┗━━ ADDITIVE ━━━ ( '+' | '-' )
┗━━ Product</pre>
</div>
</div>
<div class="tab2">
<p><code>ADDITIVE</code> <b>is not</b> followed by <code>Product</code></p>
<div class="source"><pre class="prettyprint" style="line-height: 17px">
SignedFactor
┗━━ ( ADDITIVE? Factor )
    ┣━━ ADDITIVE?
    ┃   ┗━━ ADDITIVE ━━━ ( '+' | '-' )
    ┗━━ Factor</pre>
</div>
</div>
<div class="tab3">
<p><code>ADDITIVE</code> <b>IS</b> followed by <code>Product</code> !!!</p>
<div class="source"><pre class="prettyprint" style="line-height: 17px">
Sum
┗━━ ( SignedTerm ( ADDITIVE Product )* )
    ┣━━ SignedTerm
    ┗━━ ( ADDITIVE Product )*
        ┗━━ ( ADDITIVE Product )
            ┣━━ ADDITIVE ━━━ ( '+' | '-' )
            ┗━━ Product</pre></div>
</div>
<div class="tab4">
<p>Java program allowing to dump a rule or token :</p>
<pre class="prettyprint linenums">
    Dump dump = new Dump().withoutClass()
                          .withoutHash()
                          .setVisited(Calc.SignedTerm)
                          .setVisited(Calc.Product);
    Calc.Sum.accept(dump);
    System.out.println(dump);</pre></div>

<div class="tab5">
Why <code>SignedTerm</code> and <code>SignedFactor</code> doesn't look the same in the dump ? This is because <code>SignedFactor</code> is not defined directly but is enclosed in a proxy rule, unlike <code>SignedTerm</code>.
<pre class="prettyprint linenums"><![CDATA[
    Rule SignedTerm = ADDITIVE.optional().seq(Product);
    Rule SignedFactor = $(() -> ADDITIVE.optional().seq(Calc.Factor));]]></pre>
</div>

<p>Therefore, we can check what comes next to decide if we can create a <code>Term&lt;Calc.Additive&gt;</code> :</p>
<pre class="prettyprint linenums"><![CDATA[
    ADDITIVE( (stack, token, next) -> {
        // mapper for : Token ADDITIVE = is(Additive.class);
        // e.g. a + b
        Addition op = token.getValue(); // + | -
        if (next.isEmpty()) {
            // SignedTerm   ::= ADDITIVE? Product
            // SignedFactor ::= ADDITIVE? Factor
            return null; // raw value Additive
        } else {
            // + is always followed by an argument
            // Sum ::= SignedTerm (ADDITIVE Product)*
            NumericExpression arg = next.pollFirst().getTarget(); // b argument
            Term<Addition> term = new Term<>(op, arg);
            return term;
        }
    }),]]></pre>
</div>

</div>

<a name="ruleMappers"></a>

#### Rule mappers

The last but not the least is to map the rules inside `enum CalcRules`.

<div class="tabs">

<input class="tab1" id="tabFactorMapper" type="radio" name="ruleMappers" checked="checked"><label for="tabFactorMapper">Factor</label></input>
<input class="tab2" id="tabProductMapper" type="radio" name="ruleMappers"><label for="tabProductMapper">Product</label></input>
<input class="tab3" id="tabSumMapper" type="radio" name="ruleMappers"><label for="tabSumMapper">Sum</label></input>

<div class="tab1">
<ul><li>let's start with the <code>Factor</code> rule, that handles the <code>RAISED</code> token. But this time, all values are available within the <b>arguments</b> :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
    Factor( (stack, rule, args) -> { // mapper for : Rule Factor = ...
        // Factor ::= Argument ('^' SignedFactor)?
        //              base         exponent
        NumericExpression base = args.pollFirst().getTarget();
        Value<NumericExpression> raised = args.peekFirst();
        if (raised != null && raised.isSource() && raised.getSource().getRule() == Calc.RAISED) {
            args.pollFirst(); // ^
            NumericExpression exponent = args.pollFirst().getTarget();
            return new Exponent(base, exponent);
        } else {
            // a single term is not a factor
            return base;
        }
    });]]></pre>
<p>Note that the <code>Factor</code> mapper doesn't necessary give an <code>Exponent</code> instance. Sometimes it is traversed because it doesn't contain the <code>^</code> token. In that case we return the argument as-is.</p>
<p>Similarly, for the <code>Sum</code> and <code>Product</code> mappers, the semantic of the grammar allow to traverse the counterpart rules without producing the target instances.</p>
</div>

<div class="tab2">
<ul><li>the <code>Product</code> mapper is more simpler than the <code>Sum</code> mapper :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
    Product( (stack, rule, args) -> { // mapper for : Rule Product = ...
        // Product ::= Factor (MULTIPLICATIVE SignedFactor)*
        if (args.size() == 1) {
            // a single term is not a product
            return args.pollFirst().getTarget();
        } else {
            // assume x to be *x, because the product will start by 1*x
            Term<Multiplication> factor = new Term<>(Multiplicative.MULT, args.removeFirst().getTarget());
            List<Term<Multiplication>> arguments = new LinkedList<>();
            arguments.add(factor);
            args.stream()
                // next arguments are all Term<Multiplicative>
                .map(v -> (Term<Multiplication>) v.getTarget())
                .forEachOrdered(arguments::add);
            return new Product(arguments);
        }
    }),]]></pre>
</div>

<div class="tab3">
<ul><li>for the <code>Sum</code> rule, when we do have several arguments, we must check whether the first term had a sign, or was just an argument, and transform it to conform with the signature of the target constructor :</li></ul>
<pre class="prettyprint linenums"><![CDATA[
    Sum( (stack, rule, args) -> { // mapper for : Rule Sum = ...
        // Sum ::= SignedTerm (ADDITIVE Product)*
        if (args.size() == 1) {
            // a single term is not a sum
            return args.pollFirst().getTarget();
        } else {
            NumericExpression signedTerm = args.removeFirst().getTarget();
            if (! (signedTerm instanceof Term<?>)
                || ! (((Term<?>) signedTerm).operator instanceof Addition)) {
                // force "x" to be "+x"
                signedTerm = new Term<>(Additive.PLUS, signedTerm);
            }
            List<Term<Addition>> arguments = new LinkedList<>();
            arguments.add((Term<Addition>) signedTerm);
            args.stream()
                // next arguments are all Term<Additive>
                .map(v -> (Term<Addition>) v.getTarget())
                .forEachOrdered(arguments::add);
            return new Sum(arguments);
        }
    }),]]></pre>
</div>

</div>

<hr/>

Here is the complete code of [`ExpressionBuilder`](https://github.com/alternet/alternet.ml/blob/master/parsing/src/test/java/ml/alternet/parser/step4/ExpressionBuilder.java)

You may wonder whether to use the "typed token" technique or this "AST builder" ? Well, the former is certainly easier to use, but the latter gives you more context for the mapping ; it is taken in charge by the handler in the parsing phase.

Even if the [`NodeBuilder`](apidocs/ml/alternet/parser/ast/NodeBuilder.html) can deal with homogeneous node types, we will see in the next section how easily we can expose a grammar as a token, and therefore having the capability to mix different node types.

<a name="grammarToken"></a>

### A grammar as a token

Sometimes it is preferable to expose a grammar as a token rather than extending an existing grammar.

For example, imagine that our expression `sin( x ) * ( 1 + var_12 )` always appear in fact in value templates, delimited with `{` and `}` like this :

<div class="source"><pre class="prettyprint">
the result of the expression sin(x)*(1+var_12) is { sin(x)*(1+var_12) } !
</pre></div>

Everything between the curly braces is the expression, and everything around is just text, and after parsing and evaluating, we expect an output like this :

<div class="source"><pre class="prettyprint">
the result of the expression sin(x)*(1+var_12) is 42 !
</pre></div>

(as an exercise you can find a couple for x and var_12 that gives 42)

To achieve this, we need a new grammar, but instead of extending our `Calc` grammar, we simply expose it as a token :

```java
public interface ValueTemplate extends Grammar {

    @Fragment Token LCB = is('{');
    @Fragment Token RCB = is('}');

    Token ESCAPE_LCB = is("{{"); // a double {{ is an escape for {
    Token ESCAPE_RCB = is("}}"); // a double }} is an escape for }

    //   EXPRESSION ::= Calc
    Token EXPRESSION = is(
            Calc.$,
            () -> new ExpressionBuilder());

    Rule Text = ESCAPE_LCB.or(ESCAPE_RCB, isnot(LCB, RCB)).zeroOrMore();

    @MainRule
    Rule ValueTemplate = Text.seq(EXPRESSION.optional()).zeroOrMore();

    ValueTemplate $ = $();

}
```

We are using our `Calc` grammar jointly with `ExpressionBuilder()` for parsing an expression to a `NumericExpression`. This is exposed in the token `EXPRESSION`.

This approach is preferable than extending a grammar when the target result type differs from the other grammar. In our case, an expression is evaluated to a number, whereas a value template expression is evaluated to a string.

Here is how we can evaluate such expression :

```java
    Map<String, Number> variables = new HashMap<>();
    variables.put("x", 1.0);
    variables.put("var_12", 10.0);

    Optional<StringExpression> exp = new ValueTemplateBuilder()
        .build(expression, true);

    String res = exp.get().eval(variables);
```

You can examine the code of the [`ValueTemplateBuilder`](https://github.com/alternet/alternet.ml/blob/master/parsing/src/test/java/ml/alternet/parser/step4/ValueTemplateBuilder.java) and [`StringExpression`](https://github.com/alternet/alternet.ml/blob/master/parsing/src/test/java/ml/alternet/parser/step4/StringExpression.java) on Github.

<a name="extendingMapping"></a>

### Extending the mappings

We already extend our `Calc` grammar to the `Math` grammar, by rewriting some rules and tokens. Those rewritings has an impact on our mappings. We have to rewrite those that changed accordingly.

First, our custom model is not really ideal for extension : our additions and multiplications are just exposed as enum values, whereas their underlying operations should be exposed as interfaces. This let us rewrite more easily a new multiplication with `×` and `÷` instead of `*` and `/` : with an interface, we don't change the operation, just the token.

<div class="alert alert-info" role="alert">
Each time an enum is defined in a grammar, it should implement an interface in order to ease extensibility.
</div>

With that enhancements, it is really easy to extend the existing mappings (the extension of `Calc` to `Math` has been discussed previously).

Remember the changes :

<div class="source"><pre class="prettyprint">
[03] FUNCTION       ::= 'sin' | 'cos' | 'exp' | 'ln' | 'sqrt' | 'asin' | 'acos'
[05] MULTIPLICATIVE ::= '×' | '÷'
[11] VARIABLE       ::= UPPERCASE (UPPERCASE | DIGIT | UNDERSCORE)*

[14] Argument       ::= FUNCTION LBRACKET Argument RBRACKET | Value | LBRACKET Expression RBRACKET
</pre></div>

In fact, with our enhanced data model, the sole impact is around the multiplicative operation because the enum class changed ; therefore we need to rewrite the mapping where it is produced (the `MULTIPLICATIVE` mapper) and where it is consumed (the `Product` mapper) :

<div class="tabs">

<input class="tab1" id="tabMathExpressionBuilder" type="radio" name="mathBuilder" checked="checked"><label for="tabMathExpressionBuilder">MathExpressionBuilder</label></input>
<input class="tab2" id="tabMathRules" type="radio" name="mathBuilder"><label for="tabMathRules">MathRules</label></input>
<input class="tab3" id="tabMathTokens" type="radio" name="mathBuilder"><label for="tabMathTokens">MathTokens</label></input>

<div class="tab1"><pre class="prettyprint linenums"><![CDATA[
public class MathExpressionBuilder extends NodeBuilder<NumericExpression> {

    public MathExpressionBuilder() {
        this(Math.$);
    }

    public MathExpressionBuilder(Grammar g) {
        super(g);
        setTokenMapper(MathTokens.class);
        setRuleMapper(MathRules.class);
    }

}]]></pre></div>

<div class="tab2"><pre class="prettyprint linenums"><![CDATA[
enum MathRules implements RuleMapper<NumericExpression> {

    Product( (stack, rule, args) -> {
        // Product ::= Factor (MULTIPLICATIVE SignedFactor)*
        if (args.size() == 1) {
            // a single term is not a product
            return args.pollFirst().getTarget();
        } else {
            // assume x to be ×x, because the product will start by 1×x
            Term<Multiplication> factor = new Term<>(MathMultiplicative.MULT, args.removeFirst().getTarget());
            List<Term<Multiplication >> arguments = new LinkedList<>();
            arguments.add(factor);
            args.stream()
                // next arguments are all Term<Multiplicative>
                .map(v -> (Term<Multiplication>) v.getTarget())
                .forEachOrdered(arguments::add);
            return new Product(arguments);
        }
    });

    static {
        EnumUtil.extend(ExpressionBuilder.CalcRules.class); // look, Ma ! Easy enum extension !
    }

    RuleMapper<NumericExpression> rm;

    MathRules(ExpressionBuilder.CalcRules cr) { // constructor required by EnumUtil.extend
        this.rm = cr;
    }

    MathRules(RuleMapper<NumericExpression> rm) {
        this.rm = rm;
    }

    @Override
    public NumericExpression transform(
        ValueStack<Value<NumericExpression>> stack,
        Rule rule,
        Deque<Value<NumericExpression>> args)
    {
        return rm.transform(stack, rule, args);
    }

}]]></pre></div>

<div class="tab3"><pre class="prettyprint linenums"><![CDATA[
enum MathTokens implements TokenMapper<NumericExpression> {

    MULTIPLICATIVE( (stack, token, next) -> {
        // e.g. a × b
        MathMultiplicative op = token.getValue(); // × | ÷
        // × is always followed by an argument
        NumericExpression arg = next.pollFirst().getTarget(); // b argument
        Term<MathMultiplicative> term = new Term<>(op, arg);
        return term;
    });

    static {
        EnumUtil.extend(ExpressionBuilder.CalcTokens.class);
    }

    TokenMapper<NumericExpression> tm;

    MathTokens(ExpressionBuilder.CalcTokens ct) { // constructor required by EnumUtil.extend
        this.tm = ct;
    }

    MathTokens(TokenMapper<NumericExpression> tm) {
        this.tm = tm;
    }

    @Override
    public NumericExpression transform(
        ValueStack<Value<NumericExpression>> stack,
        TokenValue<?> token,
        Deque<Value<NumericExpression>> next)
    {
        return tm.transform(stack, token, next);
    }

}]]></pre></div>

</div>

Finally, if we also expect a value template based on the `Math` builder instead of the `Calc` builder, we just need this new grammar :

```java
public interface MathValueTemplate extends ValueTemplate {

    //    EXPRESSION ::= Math
    Token EXPRESSION = is(
            Math.$,
            () -> new MathExpressionBuilder());

    MathValueTemplate $ = $();

}
```

We don't need a new builder for this grammar, because the token value produced by `MathExpressionBuilder` is a `NumericExpression`, just like for `ExpressionBuilder`. Therefore, the existing `ValueTemplateBuilder` is just enough, but need a minor update to be able to accept either the `ValueTemplate` grammar or the `MathValueTemplate` grammar.

<a name="examples"></a>

## Additional examples

You will find various examples in the [Github repo](https://github.com/alternet/alternet.ml/tree/master/parsing/src/test/java/ml/alternet/parser).

* [WWW-Authenticate parser](https://github.com/alternet/alternet.ml/blob/master/parsing/src/test/java/ml/alternet/parser/www/WAuth.java)
* [Calc grammar and parser](https://github.com/alternet/alternet.ml/tree/master/parsing/src/test/java/ml/alternet/parser/step4)
* [Argon2 crypt format parser](https://github.com/alternet/alternet.ml/blob/master/parsing/src/test/java/ml/alternet/parser/examples/Argon2CryptFormatter.java) : breakdown the crypt in its parts, decode bytes and supply parameters


<a name="troubleshooting"></a>

## Troubleshooting

<a name="dump"></a>

### Dump

A convenient tool allow to [dump](apidocs/ml/alternet/parser/visit/Dump.html) a rule :

```java
    Dump.tree(rule);
    Dump.tree(grammar); // dump the main rule

    Dump.detailed(rule); // displays additional informations about the class
```

Sometimes, a rule exist in a grammar that has been extended, but that rule hasn't been overriden, therefore, it may be helpful to specify from which grammar we want its dump, since it may affect the content :

```java
    Dump.tree(Calc.$, Math.Expression); // the original rule
    Dump.tree(Math.$, Math.Expression); // the same rule but altered in the extension
```

The output looks like this. Every named rule composed is expanded once the first time it is encountered.

<div class="tabs">

<input class="tab1" id="tabExpr" type="radio" name="dump" checked="checked"><label for="tabExpr">Expression</label></input>
<input class="tab2" id="tabArg" type="radio" name="dump"><label for="tabArg">Argument</label></input>

<div class="tab1">
<p>Below is displayed the dump of our <code>Math</code> grammar (after rule rewrites) :</p>
<div class="source"><pre class="prettyprint" style="line-height: 17px">
Expression
┗━━ Sum
    ┗━━ ( SignedTerm ( ADDITIVE Product )* )
        ┣━━ SignedTerm
        ┃   ┣━━ ADDITIVE?
        ┃   ┃   ┗━━ ADDITIVE ━━━ ( '+' | '-' )
        ┃   ┗━━ Product
        ┃       ┣━━ Factor
        ┃       ┃   ┣━━ Argument
        ┃       ┃   ┃   ┣━━ ( FUNCTION LBRACKET Argument RBRACKET )
        ┃       ┃   ┃   ┃   ┣━━ FUNCTION ━━━ ( 'ln' | 'cos' | 'exp' | 'sin' | 'acos' | 'asin' | 'sqrt' )
        ┃       ┃   ┃   ┃   ┣━━ LBRACKET ━━━ '('
        ┃       ┃   ┃   ┃   ┣━━ Argument
        ┃       ┃   ┃   ┃   ┗━━ RBRACKET ━━━ ')'
        ┃       ┃   ┃   ┣━━ Value
        ┃       ┃   ┃   ┃   ┣━━ NUMBER
        ┃       ┃   ┃   ┃   ┃   ┗━━ DIGIT+
        ┃       ┃   ┃   ┃   ┃       ┗━━ DIGIT
        ┃       ┃   ┃   ┃   ┃           ┗━━ ['0'-'9']
        ┃       ┃   ┃   ┃   ┗━━ VARIABLE
        ┃       ┃   ┃   ┃       ┗━━ ( UPPERCASE ( UPPERCASE | DIGIT | UNDERSCORE )* )
        ┃       ┃   ┃   ┃           ┣━━ UPPERCASE ━━━ ['A'-'Z']
        ┃       ┃   ┃   ┃           ┗━━ ( UPPERCASE | DIGIT | UNDERSCORE )*
        ┃       ┃   ┃   ┃               ┗━━ ( UPPERCASE | DIGIT | UNDERSCORE )
        ┃       ┃   ┃   ┃                   ┣━━ UPPERCASE ━━━ ['A'-'Z']
        ┃       ┃   ┃   ┃                   ┣━━ DIGIT
        ┃       ┃   ┃   ┃                   ┗━━ UNDERSCORE ━━━ '_'
        ┃       ┃   ┃   ┗━━ ( LBRACKET Expression RBRACKET )
        ┃       ┃   ┃       ┣━━ LBRACKET ━━━ '('
        ┃       ┃   ┃       ┣━━ Expression
        ┃       ┃   ┃       ┗━━ RBRACKET ━━━ ')'
        ┃       ┃   ┗━━ ( RAISED SignedFactor )?
        ┃       ┃       ┗━━ ( RAISED SignedFactor )
        ┃       ┃           ┣━━ RAISED ━━━ '^'
        ┃       ┃           ┗━━ SignedFactor
        ┃       ┃               ┗━━ ( ADDITIVE? Factor )
        ┃       ┃                   ┣━━ ADDITIVE?
        ┃       ┃                   ┃   ┗━━ ADDITIVE ━━━ ( '+' | '-' )
        ┃       ┃                   ┗━━ Factor
        ┃       ┗━━ ( MULTIPLICATIVE SignedFactor )*
        ┃           ┗━━ ( MULTIPLICATIVE SignedFactor )
        ┃               ┣━━ MULTIPLICATIVE ━━━ ( '×' | '÷' )
        ┃               ┗━━ SignedFactor
        ┗━━ ( ADDITIVE Product )*
            ┗━━ ( ADDITIVE Product )
                ┣━━ ADDITIVE ━━━ ( '+' | '-' )
                ┗━━ Product
</pre></div>
</div>

<div class="tab2"> 
<p>The dump of <code>Argument</code> in the <code>Math</code> grammar :</p>
<div class="source"><pre class="prettyprint" style="line-height: 17px">
Argument
┣━━ ( FUNCTION LBRACKET Argument RBRACKET )
┃   ┣━━ FUNCTION ━━━ ( 'ln' | 'cos' | 'exp' | 'sin' | 'acos' | 'asin' | 'sqrt' )
┃   ┣━━ LBRACKET ━━━ '('
┃   ┣━━ Argument
┃   ┗━━ RBRACKET ━━━ ')'
┣━━ Value
┃   ┣━━ NUMBER
┃   ┃   ┗━━ DIGIT+
┃   ┃       ┗━━ DIGIT
┃   ┃           ┗━━ ['0'-'9']
┃   ┗━━ VARIABLE
┃       ┗━━ ( UPPERCASE ( UPPERCASE | DIGIT | UNDERSCORE )* )
┃           ┣━━ UPPERCASE ━━━ ['A'-'Z']
┃           ┗━━ ( UPPERCASE | DIGIT | UNDERSCORE )*
┃               ┗━━ ( UPPERCASE | DIGIT | UNDERSCORE )
┃                   ┣━━ UPPERCASE ━━━ ['A'-'Z']
┃                   ┣━━ DIGIT
┃                   ┗━━ UNDERSCORE ━━━ '_'
┗━━ ( LBRACKET Expression RBRACKET )
    ┣━━ LBRACKET ━━━ '('
    ┣━━ Expression
    ┃   ┗━━ Sum
    ┃       ┗━━ ( SignedTerm ( ADDITIVE Product )* )
    ┃           ┣━━ SignedTerm
    ┃           ┃   ┣━━ ADDITIVE?
    ┃           ┃   ┃   ┗━━ ADDITIVE ━━━ ( '+' | '-' )
    ┃           ┃   ┗━━ Product
    ┃           ┃       ┣━━ Factor
    ┃           ┃       ┃   ┣━━ Argument
    ┃           ┃       ┃   ┗━━ ( RAISED SignedFactor )?
    ┃           ┃       ┃       ┗━━ ( RAISED SignedFactor )
    ┃           ┃       ┃           ┣━━ RAISED ━━━ '^'
    ┃           ┃       ┃           ┗━━ SignedFactor
    ┃           ┃       ┃               ┗━━ ( ADDITIVE? Factor )
    ┃           ┃       ┃                   ┣━━ ADDITIVE?
    ┃           ┃       ┃                   ┃   ┗━━ ADDITIVE ━━━ ( '+' | '-' )
    ┃           ┃       ┃                   ┗━━ Factor
    ┃           ┃       ┗━━ ( MULTIPLICATIVE SignedFactor )*
    ┃           ┃           ┗━━ ( MULTIPLICATIVE SignedFactor )
    ┃           ┃               ┣━━ MULTIPLICATIVE ━━━ ( '×' | '÷' )
    ┃           ┃               ┗━━ SignedFactor
    ┃           ┗━━ ( ADDITIVE Product )*
    ┃               ┗━━ ( ADDITIVE Product )
    ┃                   ┣━━ ADDITIVE ━━━ ( '+' | '-' )
    ┃                   ┗━━ Product
    ┗━━ RBRACKET ━━━ ')'
</pre></div>
</div>
</div>


<a name="issues"></a>

### Common issues

#### Unable to initialize a grammar :

```
java.lang.ExceptionInInitializerError
        at ...
        at ...
Caused by: java.lang.ClassCastException: ml.alternet.parser.Grammar$Proxy cannot be cast to org.example.grammar.Foo
    at org.example.grammar.Foo.<clinit>(Foo.java:9)
```

➡ Ensure to declare the grammar field `Foo $ = $();` **as the last field** in the grammar `Foo`

#### Fail to parse with `NodeBuilder` :

```
FAILED: calcExpression_CanBe_evaluated
java.lang.IllegalArgumentException: No enum constant ml.alternet.parser.step4.ExpressionBuilder.CalcRules.Argument
    at java.lang.Enum.valueOf(Enum.java:238)
    at ml.alternet.parser.ast.NodeBuilder.lambda$14(NodeBuilder.java:154)
    at ml.alternet.parser.ast.NodeBuilder.ruleToNode(NodeBuilder.java:174)
    at ml.alternet.parser.handlers.TreeHandler.receive(TreeHandler.java:149)
```

➡ Ensure to declare a mapping for`Argument` in your node builder or to annotate it with `@Fragment` in the grammar.

