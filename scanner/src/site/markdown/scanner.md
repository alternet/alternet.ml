# Alternet Scanner

<div class="nopub">
<a href="http://alternet.ml/alternet-libs/scanner/scanner.html">
Published version of this page available HERE</a></div>

**Alternet Scanner** supply means to scan character streams.

<div style="columns: 2">
<div>
<h3 style="margin: 0">Maven import</h3>
<pre class="prettyprint"><![CDATA[
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-scanner</artifactId>
    <version>1.0</version>
</dependency>]]>
</pre>
</div>
<div style="break-before: column">
<h3>JavaDoc API documentation</h3>
<ul><li><a href="apidocs/index.html">Alternet Scanner</a></li></ul>
<p>Other Alternet APIs :</p>
<ul><li><a href="../apidocs/index.html">Alternet Libs</a></li></ul>
</div>
</div>

## Usage

A [scanner](apidocs/ml/alternet/scan/Scanner.html) can read characters (Unicode codepoints) from an input stream under conditions.

Alternet Scanner is a low level API, you might consider [Alternet Parsing](../parsing/parsing.html)
for advanced features (**parsing expression grammar** framework and abstract syntax tree builder).

Convenient methods are available in Alternet Scanner for testing for example whether the next content is a number or not, for reading characters, strings and numbers, [pick a value from a set](apidocs/ml/alternet/scan/EnumValues.html) of possible strings or Enum values, and even pick the next
character if it belongs to a [range of characters](../tools/apidocs/ml/alternet/misc/CharRange.html) (possibly built with union and exclusions of other ranges).

Additional classes can help to read an object under constraints, for example "get
the next number that has less than 5 digits", or the next number that fit in a
specific type.

It is a progressive scanner in the sense that the next content is inherently independent of
what was read before. To hold a context and go back when successive items doesn't satisfy a
specific grammar, the user will have to set markers.

The [`#mark()`](apidocs/ml/alternet/scan/Scanner.html#mark--), [`#consume()`](apidocs/ml/alternet/scan/Scanner.html#consume--), and [`#cancel()`](apidocs/ml/alternet/scan/Scanner.html#cancel--) methods can help to read some characters and go back to the marked position.
Several successive positions can be marked without canceling or consuming the previous ones ;
as marks are stacked, it falls to the user to apply the appropriate number of cancel + consume
calls.

A specific device is available for single characters : just use [`#hasNextChar(int, boolean)`](apidocs/ml/alternet/scan/Scanner.html#hasNextChar-int-boolean-),
[`#hasNextChar(String, boolean)`](apidocs/ml/alternet/scan/Scanner.html#hasNextChar-java.lang.String-boolean-), or get it simply with [`#lookAhead()`](apidocs/ml/alternet/scan/Scanner.html#lookAhead--) ; you don't need to set a mark for testing the next character to read.

It is markable-reentrant that is to say that once marked, it can used itself for further
scanning with or without setting new marks.

### Example

We have to extract the scheme from an URI of the form
`[scheme]:[schemeSpecificPart]` ; the class `java.net.URI` can do the job
only if a scheme specific part is present and fail if it is absent. We do need
to parse URIs where the scheme specific part maybe missing. The following code
achieve this :

```java
    String uri = // consider we have an URI
    Scanner scan = Scanner.of(uri);
    StringBuilder uriScheme = new StringBuilder(uri.length());
    // fill the buffer until one of the separator is reached
    scan.nextString(new StringConstraint.ReadUntilChar("/?#:"), uriScheme);
    String scheme = uriScheme.toString(); // maybe empty
    if (scan.hasNextChar(':', true)) {
        Optional<String> schemeSpecificPart = scan.getRemainderString();
        // schemeSpecificPart maybe empty
        // do whatever needed with the scheme and the schemeSpecificPart
    } else {
        // the candidate URI must be well-formed
        throw new MalformedURLException(uri); // and it was not
    }
```
