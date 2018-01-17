# Grammars and Parsers

<div class="nopub">
<a href="http://alternet.ml/alternet-libs/parsing/parsing.html">
Published version of this page available HERE</a></div>

**Alternet Parsing** is a nice [Parsing Expression Grammar](https://en.wikipedia.org/wiki/Parsing_expression_grammar) framework that includes an Abstract Syntax Tree builder.

1. [Overview](#overview)
    1. [Features](#features)
2. [Grammar tutorial](#grammar-tutorial)
    1. [The grammar skeleton](#skeleton)
    1. [Tokens](#tokens)
        1. [Enum tokens](#enumTokens)
        1. [Fragment tokens and composed tokens](#fragments)
    1. [Rules](#rules)
        1. [Self rule and deferred rules](#self)
        1. [Proxy rules](#proxy)
        1. [Direct reference](#directRef)
        1. [Handling whitespaces](#whitespaces)
        1. [Extending grammars and overriding rules](#extending)
        1. [A grammar as a token](#grammarToken)
    1. [Grammar with custom token types](#customTypes)
        1. [The target custom classes](#targetClasses)
        1. [Skipping tokens](#skip)
        1. [Mapping tokens](#mapping)
    1. [Separating the raw grammar and the augmented grammar](#augmented)
3. [Parsing tutorial](#parsing-tutorial)
    1. [Parsing an input](#input)
        1. [The “tokenizer” rule](#tokenizer)
    1. [Handlers](#handlers)
    1. [Target data model](#dataModel)
    1. [AST builder](#ast)
        1. [Node builder](#nodeBuilder)
        1. [Token mappers and rule mappers](#mappers)

<a name="overview"></a>

## Overview

### Maven import

```java
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-parsing</artifactId>
    <version>1.0</version>
</dependency>
```

### JavaDoc API documentation

 * [Alternet Parsing](apidocs/index.html)

Other Alternet APIs :

 * [Alternet Libs](../apidocs/index.html)

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

<a name="grammar-tutorial"></a>

## Grammar tutorial

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

The static import contains all the material useful for building your grammar. If you use an IDE you will have it available on autocompletion.

As a requirement, the last field of your grammar (actually the only one so far : `$`) **must** hold an instance of your grammar (generated by [`$()`](apidocs/ml/alternet/parser/Grammar.html#Z:Z:D--)), which will allow to parse the input text (we will talk later about that), and in some rare cases referring fields through the grammar instance.

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
            replace(Additive.class, this, s -> str);
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
            replace(Multiplicative.class, this, s -> str);
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
            replace(Axis.class, this, s -> s.replace('_', '-'));
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

Rule parts can be combined together, and combined with operators such as * ? or +, that have their counterpart Java methods :

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

To fix this, we introduce `$("Sum")` and `$("Argument")` that are placeholder for rules not yet defined 
or defined later in our Java program :

```java
    // Expression ::=    Sum
    Rule Expression = $("Sum");
    //   Argument ::= FUNCTION        Argument  |     Value   |   LBRACKET      Expression  RBRACKET
    Rule Argument =   FUNCTION.seq($("Argument")).or( Value ).or( LBRACKET.seq( Expression, RBRACKET ) );
```

The built-in token `$self` can be used instead of `$("Argument")` within the
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

(at this time we assume that `SignedTerm` and `Product` have been already defined).

<a name="proxy"></a>

#### Proxy rules

If you are in trouble by writing `$("Sum")` (which reduce the ease of reading), you can instead define it previously by being a [`Proxy`](apidocs/ml/alternet/parser/Grammar.Proxy.html) rule:

```java
    Proxy Sum = $(); // we expect a definition later
    // Expression ::=    Sum
    Rule Expression = is(Sum);
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

* The second writing consist on declaring a STATIC method that has the same name of the field, actually `Sum()`, that return the actual rule.

```java
    // Sum ::= SignedTerm (ADDITIVE Product)*
    static Rule Sum() {
        return SignedTerm.seq(ADDITIVE.seq(Product).zeroOrMore());
    }
```

* The latter writing consist on declaring a property that has the same name of the field prepend with $, actually `$Sum`, this property being a supplier of the expected rule.

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

The engine won't let you write that and reject such grammar. Rules must hold a specific value, not identical values because if one rule was annotated, it would affect both fields.

<a name="whitespaces"></a>

#### Handling whitespaces

In fact, we would like to parse inputs like this :

```
sin (x) * ( 1 + var_12 )
```

So simple in Alternet Parsing ;) with [`@WhitespacePolicy`](../scanner/apidocs/ml/alternet/parser/Grammar.WhitespacePolicy.html)

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

[14] Argument       ::= FUNCTION '(' Argument ')' | Value | '(' Expression ')'
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
```

If the new field has another name, we have to specify with the
[`@Replace`](apidocs/ml/alternet/parser/Grammar.Replace.html) annotation which one it is replacing.
Below, everywhere `UPPERCASE` appears in the `Calc` grammar, it will
be replaced by `UPPERCASE_VARIABLE` in the `Math` grammar (and stay unchanged
in the `Calc` grammar) :

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
    @Replace(field="FUNCTION", grammar=Calc.class)
    Token ADVANCED_FUNCTION = is(MathFunction.class);
```

You don't need to rewrite the rules where those new definitions are used, they will be replaced in the new grammar. Of course, the original grammar will stay unchanged.

Here is a parsing example with each grammar :

```java
    Calc.$.parse(Scanner.of("sin( x ) * ( 1 + var_12 )"), handler);
    Math.$.parse(Scanner.of("asin( X ) × ( 1 + VAR_12 )"), handler);
```

<a name="grammarToken"></a>

#### A grammar as a token

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

We will learn later about `ExpressionBuilder()`, it is just the handler that we create in order to build an AST.

This approach is preferable than extending a grammar when the target result type differs from the other grammar. In our case, an expression is evaluated to a number, whereas a value template expression is evaluated to a string.

In the next section we will learn how to build a custom data model.

<a name="customTypes"></a>

### Grammar with custom token types

A **token value** represent the input characters that are parsed. We have seen before that a token value may have various types :

* **a single character** : this is the default behaviour. E.g. `"("`
* **a string** : this is also the default behaviour when a sequence of characters is matched. E.g. `"var_12"`
* **a number** : when it is specified by [`.asNumber()`](apidocs/ml/alternet/parser/Grammar.Rule.html#asNumber--). E.g. `123.45`
* **an enum value** : when the token is defined with an enum class. E.g. `Axis.ancestor_or_self`

It is also possible to specify in the grammar :

* characters that we want to skip. E.g. `"\\"` where the first `\` stand for an escape character and the second `\` for the data.
* characters rendered as a custom object. E.g. our class `Challenge` defined here after.

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

We will design our `WAuth` grammar and our custom result objects, say
the `Challenge` class for the global result and the `Parameter` class
for each {name, value} pair.

<a name="targetClasses"></a>

#### The target custom classes

Our custom classes, first, are very simple ; notice they are agnostic
regarding our future grammar, they are just POJOs :

```java
public class Parameter {

    public String name;  // e.g. "realm"
    public String value; // e.g. "FooCorp"

    public Parameter(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
```

```java
public class Challenge {

    public String scheme; // e.g. "Basic"
    public List<Parameter> parameters;

    public Challenge(String scheme, List<Parameter> parameters) {
        this.scheme = scheme;
        this.parameters = parameters;
    }

}
```

#### The WAuth grammar

Now, the grammar (using the "Augmented BNF" syntax, see §2.1 in [RFC-2616](https://www.ietf.org/rfc/rfc2616.txt)) :

```
    # from RFC-2617 (HTTP Basic and Digest authentication)

    challenge      = auth-scheme 1*SP 1#auth-param
    auth-scheme    = token
    auth-param     = token "=" ( token | quoted-string )

    # from RFC-2616 (HTTP/1.1)

    token          = 1*<any CHAR except CTLs or separators>
    separators     = "(" | ")" | "<" | ">" | "@"
                   | "," | ";" | ":" | "\" | <">
                   | "/" | "[" | "]" | "?" | "="
                   | "{" | "}" | SP | HT
    quoted-string  = ( <"> *(qdtext | quoted-pair ) <"> )
    qdtext         = <any TEXT except <">>
    quoted-pair    = "\" CHAR
```

<a name="skip"></a>

#### Skipping tokens

Let's start the Java grammar :

```java
public interface WAuth extends Grammar {

    @Fragment Token SEPARATORS = isOneOf("()<>@,;:\\\"/[]?={} \t");
    @Fragment Token CTRLS = range(0, 31).union(127); // octets 0 - 31 and DEL (127)

    @WhitespacePolicy(preserve=true)
    @Fragment Token TOKEN_CHAR = isNot(SEPARATORS, CTRLS);

    @WhitespacePolicy
    Token TOKEN = TOKEN_CHAR.oneOrMore()
            .asToken();

    @Fragment Token DOUBLE_QUOTE = is('"');
    @Fragment Token BACKSLASH = is('\\')
            .skip();

    @WhitespacePolicy(preserve=true)
    @Fragment Token QuotedPair = BACKSLASH.seq($any).asToken();

    // other tokens here

    WAuth $ = $();

}
```

We defined various tokens as ranges of characters, and tokens made of other tokens.

* [`$any`](apidocs/ml/alternet/parser/Grammar.html#Z:Z:Dany) is a built-in token that matches any character.
* Remember that as soon as you use modifiers such as [`.oneOrMore()`](apidocs/ml/alternet/parser/Grammar.Rule.html#oneOrMore--) or combiners such as [`.seq()`](apidocs/ml/alternet/parser/Grammar.Rule.html#seq-ml.alternet.parser.Grammar.Rule...-) you get a `Rule`, but you can turn it back to a `Token` with [`.asToken()`](apidocs/ml/alternet/parser/Grammar.Rule.html#asToken--).
* The `BACKSLASH` token is skipped with [`.skip()`](apidocs/ml/alternet/parser/Grammar.Rule.html#skip--). It means that the handler won't ever receive this token, but the grammar ensure that the following character is properly escaped, and that the relevant value received is properly stripped from the `\` character.

Conversely, the `DOUBLE_QUOTE` token is not skipped at the token definition, because sometimes it stands for a delimiter (and in that case it has to be removed), and some other times it stands for itself (as `"`). Therefore, we use [`.skip()`](apidocs/ml/alternet/parser/Grammar.Rule.html#skip--) at the places we don't want to get `"` as data :

```java
    @WhitespacePolicy(preserve=true)
    @Fragment Token QdText = isNot(DOUBLE_QUOTE);

    Token QuotedString = DOUBLE_QUOTE.skip().seq( // " is a separator
            QuotedPair.or(QdText).zeroOrMore(),
            DOUBLE_QUOTE.skip())                  // " is a separator too
        .asToken();

    Token ParameterValue = TOKEN.or(QuotedString).asToken();

    @Fragment Token EQUAL = is('=');
```

<a name="mapping"></a>

#### Mapping tokens

Instead of having `String`s or `Numbers`s, we expect having our types (yes, fields and types may have the same name) :

| Field name      | Type expected     |
| --------------- |:-----------------:|
| `Parameter`     | `Parameter`       |
| `Parameters`    | `List<Parameter>` |
| `Challenge`     | `Challenge`       |

We already used [`.asNumber()`](apidocs/ml/alternet/parser/Grammar.Rule.html#asNumber--) for getting a number value and [`.asToken()`](apidocs/ml/alternet/parser/Grammar.Rule.html#asToken--) for turning a rule to a token ; now we will use [`.asToken(mapper)`](apidocs/ml/alternet/parser/Grammar.Rule.html#asToken-java.util.function.Function-) to turn the tokens of a rule to a custom object. Actually, we expect our `Parameter` object, and we have a special type for the counterpart definition : [`TypedToken<Parameter>`](apidocs/ml/alternet/parser/Grammar.TypedToken.html). The mapper is just a function that takes as argument the `List` of tokens parsed by the rule and that returns a value that can be consumed by the enclosing rule.

Below, the rule will match `aName = aValue` in 3 tokens (`aName`, `=`, and `aValue`), and we produce a `Parameter` object with the **first** and the **last** tokens ; the `=` token is ignored, no need to use [`.skip()`](apidocs/ml/alternet/parser/Grammar.Rule.html#skip--) on it (but you could, it wouldn't change anything).

```java
    //                    Parameter ::= TOKEN     EQUAL  ParameterValue
    TypedToken<Parameter> Parameter =   TOKEN.seq(EQUAL, ParameterValue)  // e.g. "aName = aValue"
        .asToken(tokens ->
            new Parameter(
                    tokens.getFirst().getValue(), // TOKEN          e.g. "aName"
                    tokens.getLast().getValue()   // ParameterValue e.g. "aValue"
        ));
```

Similarly, a list of parameters can be produced easily, but since we don't know how many tokens will be available in that list, we are streaming the list of tokens. Since the <tt>Parameter<b>s</b></tt> rule is made of `Parameter` rules that create new instances of our `Parameter` POJO (yes, we have the same name for a rule and our POJO), we can safely cast the token value. Below, instead of skipping the `COMMA` token, as an alternative we filter it while processing the stream :

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

It's worth to mention that the tokens available in the list are instances of [`TokenValue<V>`](apidocs/ml/alternet/parser/EventsHandler.TokenValue.html) from which you can extract the rule/token that matched the input ([`.getRule()`](apidocs/ml/alternet/parser/EventsHandler.RuleEvent.html#getRule--)) and the actual value ([`.getValue()`](apidocs/ml/alternet/parser/EventsHandler.TokenValue.html#getValue--)). You can aslo retrieve the type of the value or set a new value.

Finally, the production of the `Challenge` is obvious,
and it is marked as the main rule of our grammar :

```java
    //                    Challenge ::= TOKEN     Parameters
    @MainRule
    TypedToken<Challenge> Challenge =   TOKEN.seq(Parameters)
        .asToken(tokens -> new Challenge(
                tokens.removeFirst().getValue(), // TOKEN
                tokens.removeFirst().getValue()) // Parameters
            );
```

In a nutshell, the line #3 that defines the rule refers to other rules or tokens.
The lines #4 to #7 that create an object refers to other objects previsouly created.

Now we can create a parser (outside of our grammar), to get
optionally our challenge (it is optional because the parsing may
fail) :

```java
public class WAuthParser {

    public Challenge parse(String input) {
        Optional<Challenge> result = new NodeBuilder<Challenge>(WAuth.$).build(input, true);
        return result.get(); // or throw an error
    }
}
```

We are using the `NodeBuilder<T>` class that can supply an instance of `T`
if the parsing succeeds, actually our POJO `Challenge`. The boolean parameter
indicates when set to `true` to consume all the characters from the input
(it would be a failure if some characters remain at the end).


<a name="augmented"></a>

### Separating the raw grammar and the augmented grammar

In the previous example, we made an augmented grammar as a whole ("augmented" means augmented with our custom classes). Imagine that somebody want to use our grammar but using its own classes ; he had to rewrite all the augmented rules + its own mappings, whereas it would be better if he had to write only its own mappings. We need a clean separation of the "raw" grammar that only deal with scalar and enum values, and the "augmented" grammar that uses our objects.

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

<a name="parsing-tutorial"></a>

## Parsing tutorial

<a name="input"></a>

### Parsing an input

An input can be parsed :

* on a special rule that stands for the main rule
* on any of the token rules
* on a given rule

We have a grammar interface, and a field which is an instance of that grammar.
From that instance, we can parse an input on a given rule, actually the `Calc.Expression` rule :

```java
    Handler handler = ...;
    String input = "sin(x)*(1+var_12)";
    Calc.$.parse(Scanner.of(input), handler, Calc.Expression);
```

The handler is the component that accept the parsing result (more about that on see next section).

If we consider that the field `Expression` is the main rule in our grammar, we can annotate it like this :

```java
    @MainRule Rule Expression = $("SignedTerm").seq($("SumOp"));
```

If the `parse()` method is invoked without specifying any rule, the main rule will be used :

```java
    Handler handler = ...;
    String input = "sin(x)*(1+var_12)";
    Calc.$.parse(Scanner.of(input), handler);
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

<a name="handlers"></a>

### Handlers

Alternet Parsing comes with out-of-the-box [`Handler`s](apidocs/ml/alternet/parser/Handler.html)
that can receive the result of the parsing, that will be handy for processing that result :

* [`TreeHandler`](apidocs/ml/alternet/parser/handlers/TreeHandler.html) : low-level API
* [`NodeBuilder`](apidocs/ml/alternet/parser/ast/NodeBuilder.html) : high-level API

The [AST package](apidocs/ml/alternet/parser/ast/package-summary.html) contains helper classes to build
an abstract syntax tree while parsing.

<a name="dataModel"></a>

### Target data model

Let's go back to our `Calc` grammar. We intend to compute expressions, and therefore build a target data
model with the help of the [`Expression<T,C>`](apidocs/ml/alternet/parser/ast/Expression.html) class :

* where `T` is the type of the result of the computation, in our case it will be a `Number`
* and `C` the context of the evaluation ; according to the grammar, it may hold all the necessary for computing an expression,
for example supply built-in and custom functions, namespace mappers, bound variables to their name, etc. In our example we just need
to resolve variable name, and the context will be as simple as a `Map<String,Number>`.

```java
/**
 * The user data model is a tree of expressions.
 */
public interface NumericExpression extends Expression<Number, Map<String,Number>> {
}
```

From this base, we are defining every kind of `NumericExpression` expected :

* A constant expression wraps a number value :

```java
    class Constant implements NumericExpression {

        Number n;

        public Constant(Number n) {
            this.n = n;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            return n;
        }

    }
```

* A variable expression wraps a variable name and can be resolved with the context :

```java
    class Variable implements NumericExpression {

        String name;

        public Variable(String name) {
            this.name = name;
        }

        @Override
        public Number eval(Map<String, Number> variables) {
            return variables.get(this.name);
        }

    }
```

* An exponent expression is made of a base and an exponent :

```java
    class Exponent implements NumericExpression {

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
    }
```

* A function expression embeds an evaluable function :

```java
    class Function implements NumericExpression {

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

    }
```

```java
    public interface EvaluableFunction {

        Number eval(Number value);
    }
```

In the `Calc` grammar, we enhance the enum class accordingly :

```java
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
            public java.lang.Number  eval(java.lang.Number  value) {
                return Math.cos(value.doubleValue());
            }
        },
        exp {
            @Override
            public java.lang.Number  eval(java.lang.Number  value) {
                return Math.exp(value.doubleValue());
            }
        },
        ln {
            @Override
            public java.lang.Number  eval(java.lang.Number  value) {
                return Math.log(value.doubleValue());
            }
        },
        sqrt {
            @Override
            public java.lang.Number  eval(java.lang.Number  value) {
                return Math.sqrt(value.doubleValue());
            }
        };
    }
    Token FUNCTION = is(Function.class);
```

* A term is an expression bound with an operation that appears in sums or products :

```java
    class Term<T> implements NumericExpression {
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
            // can have +a or -a but can't have *a or /a
        }
    }
```

```java
    class Sum implements NumericExpression {

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

    }
```

```java
    class Product implements NumericExpression {

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

    }
```

Here is the complete code of [`NumericExpression`](https://github.com/alternet/alternet.ml/blob/master/parsing/src/test/java/ml/alternet/parser/step4/NumericExpression.java)

<a name="ast"></a>

### AST builder

As mentionned previously, a node builder can be designed from our `Calc` grammar instance.

We just need to map every token to the relevant class, and every rule to the relevant class. For that purpose, we will use [`TokenMapper<T>`](apidocs/ml/alternet/parser/ast/TokenMapper.html) and [`RuleMapper<T>`](apidocs/ml/alternet/parser/ast/RuleMapper.html) where `T` is our `NumericExpression`. The simplest way to create such
mappers is to enumerate the mappings :

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

You might notice that the name of the token mappers as well as the name of the rule mappers
are the same than those defined in the `Calc` grammar, this is why the mapping
will work. You also might notice that some rules in the grammar are not defined here,
which means that their tokens will be simply pass to the enclosing rule. Tokens marked as `@Fragment`
as well as tokens that are aggregated (enclosed in a rule exposed as a token with `.asToken(...)`) are
not present either in the mappings.

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

#### Token mappers and rule mappers

What is missing is the implementation of each mapper `CalcTokens` and `CalcRules`.
The signature of a token mapper is :

```java
    Node transform(
            ValueStack<Value<Node>> stack,
            TokenValue<?> token,
            Deque<Value<Node>> next);
```

The signature of a rule mapper is :

```java
    Node transform(
            ValueStack<Value<Node>> stack,
            Rule rule,
            Deque<Value<Node>> args);
```

They are both very similar, and apply to the `<Node>` type, which is in our builder the `<NumericExpression>` type :

* The first parameter contains the **stack** of raw items encountered so far. "Raw" means that they are not yet transformed since the production is performed bottom up.
The more often the stack doesn't serve the transformation but sometimes it may help to peek the last previous item.
* The second parameter is the current **token** / **rule**.
* The last parameter contains all the values that are either the **arguments** of the rule to transform, or all the values coming **next** from the token to transform in the context of its enclosed rule. That values can be raw values or transformed values, according to how you process them individually.
In fact `Value<NumericExpression>` is a wrapper around an object that can be either the raw token or a `NumericExpression`. You are free to supply tokens left as-is or transformed ones, and to get the raw value with `.getSource()` or the transformed one with `.getTarget()`.

For example, if a rule defines a comma-separated list of digits, that the input is `"1,2,3,4"`, and that the current **token** is `"2"`, then the **next** elements are `",3,4"` and the stack is `"1,"`. Some elements may be consumed during the production of the target node.

Now we can write the mappers, starting with the simplest ones :

* the `VARIABLE` **token** is just a sequence of characters, we write the `VARIABLE` **mapper**
(with the same name as the token) that create a `Variable` **instance** from our data model with that token :

```java
    VARIABLE { // mapper for : Token VARIABLE = ( (LOWERCASE).or( <etc...> ) .asToken()
        @Override
        public NumericExpression transform(
                ValueStack<Value<NumericExpression>> stack,
                TokenValue<?> token,
                Deque<Value<NumericExpression>> next)
        {
            String name = token.getValue();
            return new Variable(name);
        }
    };
```

* the `NUMBER` token is produced in the grammar with [`.asNumber()`](apidocs/ml/alternet/parser/Grammar.Rule.html#asNumber--), we write the `NUMBER` mapper that create a `Constant` instance from our data model with that number token :

```java
    NUMBER { // mapper for : Token NUMBER = DIGIT.oneOrMore().asNumber();
        @Override
        public NumericExpression transform(
                ValueStack<Value<NumericExpression>> stack,
                TokenValue<?> token,
                Deque<Value<NumericExpression>> next)
        {
            Number n = token.getValue();
            return new Constant(n);
        }
    },
```

* the `FUNCTION` token is produced in the grammar with the `Calc.Function` enum class, we write the `FUNCTION` mapper that create a `Function` instance from our data model with that enum value token. In our grammar, functions are taking a single argument, we can retrieve it as the next following value :

```java
    FUNCTION { // mapper for : Token FUNCTION = is(Calc.Function.class);
        @Override
        public NumericExpression transform(
                ValueStack<Value<NumericExpression>> stack,
                TokenValue<?> token,
                Deque<Value<NumericExpression>> next)
        {
            // e.g.   sin  x
            //   function  argument
            Calc.Function function = token.getValue();   // e.g.   Calc.Function.sin
            NumericExpression argument = next.pollFirst().getTarget(); // e.g.   Variable("x")
            return new Function(function, argument);
        }
    },
```

* the `RAISED` token is a little special because it uses the previous item and the next one.
Unfortunately, the previous item is not yet transformed to a `NumericExpression`, therefore
we can't do something useful in that mapper : we must delegate the mapping to the counterpart
rule mapper `Factor` :

```java
    RAISED { // mapper for : Token RAISED = is('^');
        @Override
        public NumericExpression transform(
                ValueStack<Value<NumericExpression>> stack,
                TokenValue<?> token,
                Deque<Value<NumericExpression>> next)
        {
            // e.g. a ^ b
            return null; // we don't know how to process it here => keep the source value
        }
    },
```

* the 2 last tokens `ADDITIVE` and `MULTIPLICATIVE` look similar, and produced
in the grammar with the `Calc.Additive` and `Calc.Multiplicative` enum classes.
They are transformed to either `Term<Calc.Additive>` or `Term<Calc.Multiplicative>` ;
the grammar say that the token is **always** followed by an argument :

```java
    ADDITIVE { // mapper for : Token ADDITIVE = is(Additive.class);
        @Override
        public NumericExpression transform(
                ValueStack<Value<NumericExpression>> stack,
                TokenValue<?> token,
                Deque<Value<NumericExpression>> next)
        {
            // e.g. a + b
            Additive op = token.getValue(); // + | -
            // + is always followed by an argument
            NumericExpression arg = next.pollFirst().getTarget(); // b argument
            Term<Additive> term = new Term<>(op, arg);
            return term;
        }
    },
    MULTIPLICATIVE { // mapper for : Token MULTIPLICATIVE = is(Multiplicative.class);
        @Override
        public NumericExpression transform(
                ValueStack<Value<NumericExpression>> stack,
                TokenValue<?> token,
                Deque<Value<NumericExpression>> next)
        {
            // e.g. a * b
            Multiplicative op = token.getValue(); // * | /
            // * is always followed by an argument
            NumericExpression arg = next.pollFirst().getTarget(); // b argument
            Term<Multiplicative> term = new Term<>(op, arg);
            return term;
        }
    },
```

The last but not the least is to map the rules.

* let's start with the `Factor` rule, that handles the `RAISED` token. But this time, all
values are available within the **arguments** :

```java
    Factor { // mapper for : Rule Factor = ...
        @Override
        public NumericExpression transform(
                ValueStack<Value<NumericExpression>> stack,
                Rule rule,
                Deque<Value<NumericExpression>> args)
        {
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
        }
    };
```

Note that the `Factor` mapper doesn't necessary give an `Exponent` instance. Sometimes it
is traversed because it doesn't contain the `^` token. In that case we return the
argument as-is.

Similarly, for the `Sum` and `Product` mappers, the semantic of the grammar allow to traverse
the counterpart rules without producing the target instances.

* for the `Sum` rule, when we do have several arguments, we must check whether the first
term had a sign, or was just an argument, and transform it to conform with the signature
of the target constructor :

```java
    Sum { // mapper for : Rule Sum = ...
        @SuppressWarnings("unchecked")
        @Override
        public NumericExpression transform(
                ValueStack<Value<NumericExpression>> stack,
                Rule rule,
                Deque<Value<NumericExpression>> args)
        {
            // Sum ::= SignedTerm (ADDITIVE Product)*
            if (args.size() == 1) {
                // a single term is not a sum
                return args.pollFirst().getTarget();
            } else {
                NumericExpression signedTerm = args.removeFirst().getTarget();
                if (! (signedTerm instanceof Term<?>)
                    || ! (((Term<?>) signedTerm).operation instanceof Additive)) {
                    // force "x" to be "+x"
                    signedTerm = new Term<>(Additive.PLUS, signedTerm);
                }
                List<Term<Additive>> arguments = new LinkedList<>();
                arguments.add((Term<Additive>) signedTerm);
                args.stream()
                    // next arguments are all Term<Additive>
                    .map(v -> (Term<Additive>) v.getTarget())
                    .forEachOrdered(arguments::add);
                return new Sum(arguments);
            }
        }
    },
```

* the `Product` mapper is more simpler :

```java
    Product { // mapper for : Rule Product = ...
        @SuppressWarnings("unchecked")
        @Override
        public NumericExpression transform(
                ValueStack<Value<NumericExpression>> stack,
                Rule rule,
                Deque<Value<NumericExpression>> args)
        {
            // Product ::= Factor (MULTIPLICATIVE SignedFactor)*
            if (args.size() == 1) {
                // a single term is not a product
                return args.pollFirst().getTarget();
            } else {
                // assume x to be *x, because the product will start by 1*x
                Term<Multiplicative> factor = new Term<>(Multiplicative.MULT, args.removeFirst().getTarget());
                List<Term<Multiplicative>> arguments = new LinkedList<>();
                arguments.add(factor);
                args.stream()
                    // next arguments are all Term<Multiplicative>
                    .map(v -> (Term<Multiplicative>) v.getTarget())
                    .forEachOrdered(arguments::add);
                return new Product(arguments);
            }
        }
    },
```

Here is the complete code of [`ExpressionBuilder`](https://github.com/alternet/alternet.ml/blob/master/parsing/src/test/java/ml/alternet/parser/step4/ExpressionBuilder.java)

