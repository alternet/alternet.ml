package ml.alternet.scan;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class StringScanTest {

    @Test
    public void scanner_Should_parseCharactersWithConsuming() throws IOException {
        String uri = "http://alternet.ml";
        Scanner scan = new StringScanner(uri);
        StringBuilder uriScheme = new StringBuilder(uri.length());
        scan.nextString(new StringConstraint.ReadUntilChar("/?#:"), uriScheme);

        // true -> do consume the char
        Assertions.assertThat(scan.hasNextChar(':', true)).isTrue();

        String scheme = uriScheme.toString();
        String schemeSpecificPart = scan.getRemainderString();

        Assertions.assertThat(scheme).isEqualTo("http");
        Assertions.assertThat(schemeSpecificPart).isEqualTo("//alternet.ml");
    }

    @Test
    public void scanner_Should_parseCharactersAndEnd() throws IOException {
        String uri = "http:";
        Scanner scan = new StringScanner(uri);
        StringBuilder uriScheme = new StringBuilder(uri.length());
        scan.nextString(new StringConstraint.ReadUntilChar("/?#:"), uriScheme);

        // true -> do consume the char
        Assertions.assertThat(scan.hasNextChar(':', true)).isTrue();

        String scheme = uriScheme.toString();
        String schemeSpecificPart = scan.getRemainderString();

        Assertions.assertThat(scheme).isEqualTo("http");
        Assertions.assertThat(schemeSpecificPart).isNullOrEmpty();
    }

    @Test
    public void scanner_Should_parseCharactersWithoutConsuming() throws IOException {
        String uri = "http://alternet.ml";
        Scanner scan = new StringScanner(uri);
        StringBuilder uriScheme = new StringBuilder(uri.length());
        scan.nextString(new StringConstraint.ReadUntilChar("/?#:"), uriScheme);

        // false -> don't consume the char
        Assertions.assertThat(scan.hasNextChar(':', false)).isTrue();

        String scheme = uriScheme.toString();
        String schemeSpecificPart = scan.getRemainderString();

        Assertions.assertThat(scheme).isEqualTo("http");
        Assertions.assertThat(schemeSpecificPart).isEqualTo("://alternet.ml");
    }

}
