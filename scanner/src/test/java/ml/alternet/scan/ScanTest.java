package ml.alternet.scan;

import java.io.IOException;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public abstract class ScanTest {

    public abstract Scanner scanner(String string) throws IOException;

    public void scanner_Should_parseCharactersWithConsuming() throws IOException {
        String uri = "http://example.org";
        Scanner scan = scanner(uri);
        StringBuilder uriScheme = new StringBuilder(uri.length());
        scan.nextString(new StringConstraint.ReadUntilChar("/?#:"), uriScheme);

        // true -> do consume the char
        Assertions.assertThat(scan.hasNextChar(':', true)).isTrue();

        String scheme = uriScheme.toString();
        String schemeSpecificPart = scan.getRemainderString().get();

        Assertions.assertThat(scheme).isEqualTo("http");
        Assertions.assertThat(schemeSpecificPart).isEqualTo("//example.org");
    }

    public void scanner_Should_parseCharactersAndTerminate() throws IOException {
        String uri = "http:";
        Scanner scan = scanner(uri);
        StringBuilder uriScheme = new StringBuilder(uri.length());
        scan.nextString(new StringConstraint.ReadUntilChar("/?#:"), uriScheme);

        // true -> do consume the char
        Assertions.assertThat(scan.hasNextChar(':', true)).isTrue();

        String scheme = uriScheme.toString();
        Optional<String> schemeSpecificPart = scan.getRemainderString();

        Assertions.assertThat(scheme).isEqualTo("http");
        Assertions.assertThat(schemeSpecificPart.isPresent()).isFalse();
    }

    public void scanner_Should_parseCharactersWithoutConsuming() throws IOException {
        String uri = "http://example.org";
        Scanner scan = scanner(uri);
        StringBuilder uriScheme = new StringBuilder(uri.length());
        scan.nextString(new StringConstraint.ReadUntilChar("/?#:"), uriScheme);

        // false -> don't consume the char
        Assertions.assertThat(scan.hasNextChar(':', false)).isTrue();

        String scheme = uriScheme.toString();
        String schemeSpecificPart = scan.getRemainderString().get();

        Assertions.assertThat(scheme).isEqualTo("http");
        Assertions.assertThat(schemeSpecificPart).isEqualTo("://example.org");
    }

    public void scanner_Should_parseCharactersWithMarkFromStart() throws IOException {
        String uri = "http://example.org";
        Scanner scan = scanner(uri);

        scan.mark(); // MARK IS HERE
        boolean schemeIsHttp = scan.hasNextString("http", false); // don't consume
        Assertions.assertThat(schemeIsHttp).isTrue();

        // not consumed, we can retry
        schemeIsHttp = scan.hasNextString("http", true); // this time, consume it
        Assertions.assertThat(schemeIsHttp).isTrue();

        Assertions.assertThat(scan.hasNextChar(':', false)).isTrue(); // don't consume

        String schemeSpecificPart = scan.getRemainderString().get();
        Assertions.assertThat(schemeSpecificPart).isEqualTo("://example.org");

        scan.cancel(); // CANCEL THE MARK ABOVE
        String all = scan.getRemainderString().get();
        Assertions.assertThat(all).isEqualTo(uri);

        Optional<String> nothing = scan.getRemainderString();
        Assertions.assertThat(nothing).isEmpty();
    }

    public void scanner_Should_parseCharactersWithMark() throws IOException {
        String uri = "{http://example.org}";
        Scanner scan = scanner(uri);

        Assertions.assertThat(scan.hasNextChar('{', true)).isTrue(); // consume

        scan.mark(); // MARK IS HERE
        boolean schemeIsHttp = scan.hasNextString("http", false); // don't consume
        Assertions.assertThat(schemeIsHttp).isTrue();

        // not consumed, we can retry
        schemeIsHttp = scan.hasNextString("http", true); // this time, consume it
        Assertions.assertThat(schemeIsHttp).isTrue();

        Assertions.assertThat(scan.hasNextChar(':', false)).isTrue(); // don't consume

        String schemeSpecificPart = scan.getRemainderString().get();
        Assertions.assertThat(schemeSpecificPart).isEqualTo("://example.org}");

        scan.cancel(); // CANCEL THE MARK ABOVE
        String all = scan.getRemainderString().get();
        Assertions.assertThat(all).isEqualTo("http://example.org}");

        Optional<String> nothing = scan.getRemainderString();
        Assertions.assertThat(nothing).isEmpty();
    }

    public static enum Protocol {
        file, ftp, ftps, http, mailto;
        // "f" -> file,
        //        "tp" -> ftp, ftps
        // "h" -> http
        // "m" -> mailto
    }
    @DataProvider(name = "Protocol")
    public static Object[][] createData() {
        Object[][] data = new Object[Protocol.values().length][1];
        int i = 0;
        for (Protocol a: Protocol.values()) {
            data[i++][0] = a;
        }
        return data;
    }

    @Test(dataProvider="Protocol")
    public void scanner_Should_parseEnumAlone(Protocol p) throws IOException {
        String protocol = p.name();
        Scanner scan = scanner(protocol);

        Optional<Protocol> scheme = scan.nextEnumValue(Protocol.class);

        Assertions.assertThat(scheme.isPresent()).isTrue();
        Assertions.assertThat(scheme.get()).isSameAs(p);

        Optional<String> nothing = scan.getRemainderString();
        Assertions.assertThat(nothing).isEmpty();
    }

    @Test(dataProvider="Protocol")
    public void scanner_Should_parseWrappedEnum(Protocol p) throws IOException {
        String protocol = '{' + p.name() + '}';
        Scanner scan = scanner(protocol);

        Assertions.assertThat(scan.hasNextChar('{', true)).isTrue();
        Optional<Protocol> scheme = scan.nextEnumValue(Protocol.class);

        Assertions.assertThat(scheme.isPresent()).isTrue();
        Assertions.assertThat(scheme.get()).isSameAs(p);

        Assertions.assertThat(scan.hasNextChar('}', true)).isTrue();

        Optional<String> nothing = scan.getRemainderString();
        Assertions.assertThat(nothing).isEmpty();
    }

    @Test(dataProvider="Protocol")
    public void scanner_Should_parseWithMarkEnumAlone(Protocol p) throws IOException {
        String protocol = p.name();
        Scanner scan = scanner(protocol);

        scan.mark();
        Optional<Protocol> scheme = scan.nextEnumValue(Protocol.class);

        Assertions.assertThat(scheme.isPresent()).isTrue();
        Assertions.assertThat(scheme.get()).isSameAs(p);

        scan.cancel();
        scheme = scan.nextEnumValue(Protocol.class);

        Assertions.assertThat(scheme.isPresent()).isTrue();
        Assertions.assertThat(scheme.get()).isSameAs(p);

        Optional<String> nothing = scan.getRemainderString();
        Assertions.assertThat(nothing).isEmpty();
    }

    @Test(dataProvider="Protocol")
    public void scanner_Should_parseWithMarkWrappedEnum(Protocol p) throws IOException {
        String protocol = '{' + p.name() + '}';
        Scanner scan = scanner(protocol);

        Assertions.assertThat(scan.hasNextChar('{', true)).isTrue();
        scan.mark();
        Optional<Protocol> scheme = scan.nextEnumValue(Protocol.class);

        Assertions.assertThat(scheme.isPresent()).isTrue();
        Assertions.assertThat(scheme.get()).isSameAs(p);

        scan.cancel();

        scheme = scan.nextEnumValue(Protocol.class);

        Assertions.assertThat(scheme.isPresent()).isTrue();
        Assertions.assertThat(scheme.get()).isSameAs(p);
        Assertions.assertThat(scan.hasNextChar('}', true)).isTrue();

        Optional<String> nothing = scan.getRemainderString();
        Assertions.assertThat(nothing).isEmpty();
    }

    public void enumValues_Should_beEnumeratedBySize() throws IOException {
        Assertions.assertThat(
            EnumValues.from(Protocol.class).toPrettyString().toString()
        ).isEqualTo("( ftp | file | ftps | http | mailto )");
    }

    public void scanner_Should_parseEnum() throws IOException {
        String uri = "http://example.org";
        Scanner scan = scanner(uri);

        Optional<Protocol> scheme = scan.nextEnumValue(Protocol.class);

        Assertions.assertThat(scheme.isPresent()).isTrue();

        Assertions.assertThat(scheme.get()).isSameAs(Protocol.http);

        String schemeSpecificPart = scan.getRemainderString().get();
        Assertions.assertThat(schemeSpecificPart).isEqualTo("://example.org");
    }

    public void scanner_Should_parseEnumFromSet() throws IOException {
        String uri = "ftp://example.org"; // we have the set "f" -> "file", "ftp", "ftps"
        Scanner scan = scanner(uri);

        Optional<Protocol> scheme = scan.nextEnumValue(Protocol.class);

        Assertions.assertThat(scheme.isPresent()).isTrue();

        Assertions.assertThat(scheme.get()).isSameAs(Protocol.ftp);

        String schemeSpecificPart = scan.getRemainderString().get();
        Assertions.assertThat(schemeSpecificPart).isEqualTo("://example.org");
    }

    public void scanner_Should_parseEnumFromSetWithMultipleCandidates() throws IOException {
        String uri = "file:///path/to/file"; // we have the set "f" -> "file", "ftp", "ftps"
        Scanner scan = scanner(uri);

        Optional<Protocol> scheme = scan.nextEnumValue(Protocol.class);

        Assertions.assertThat(scheme.isPresent()).isTrue();

        Assertions.assertThat(scheme.get()).isSameAs(Protocol.file);

        String schemeSpecificPart = scan.getRemainderString().get();
        Assertions.assertThat(schemeSpecificPart).isEqualTo(":///path/to/file");
    }

    public void scanner_Should_cancelMarkOnEOFProperly() throws IOException {
        String text = "1234";
        Scanner scan = scanner(text);
        Number one234 = scan.nextNumber();
        Assertions.assertThat(scan.hasNext()).isFalse();
        scan.mark();
        scan.cancel();
        Assertions.assertThat(scan.hasNext()).isFalse();
    }

}
