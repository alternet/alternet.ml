# Alternet Scanner

Alternet Scanner supply means to scan character streams.

## Maven import

```xml
<dependency>
    <groupId>ml.alternet</groupId>
    <artifactId>alternet-scanner</artifactId>
    <version>1.0</version>
</dependency>
```

## JavaDoc API documentation

* [Alternet Scanner](apidocs/index.html)

Other Alternet APIs :

* [Alternet Libs](../apidocs/index.html)

## Usage

A scanner can read characters from an input stream under conditions.

Convenient methods are available for testing for example whether the next content
is a number or not, for reading characters, strings and numbers.

Additional classes can help to read an object under constraints, for example "get
the next number that has less than 5 digits", or the next number that fit in a
specific type.

It is a progressive scanner in the sense that the next content is inherently independent of
what was read before. To hold a context and go back when successive items doesn't satisfy a
specific grammar, the user will have to set markers.

The `#mark(int)`, `#consume()`, and `#cancel()` methods can help to read some
characters and go back to the marked position.
Several successive positions can be marked without canceling or consuming the previous ones ;
as marks are stacked, it falls to the user to apply the appropriate number of cancel + consume
calls.

A specific device is available for single characters : just use `#hasNextChar(char, boolean)`,
`#hasNextChar(String, boolean)`, or get it simply with `#lookAhead()` ; you don't need to set a mark
for testing the next character to read.

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
    Scanner scan = new StringScanner(uri);
    StringBuilder uriScheme = new StringBuilder(uri.length());
    scan.nextString(new StringConstraint.ReadUntilChar("/?#:"), uriScheme);
    String scheme = uriScheme.toString(); // maybe empty
    if (scan.hasNextChar(':', true)) {
        String schemeSpecificPart = scan.getRemainderString();
        // schemeSpecificPart maybe empty
        // do whatever needed with the scheme and the schemeSpecificPart
    } else {
        // the candidate URI must be well-formed
        throw new MalformedURLException(uri); // and it is not
    }
```
