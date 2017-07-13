package ml.alternet.scan;

import java.io.IOException;
import java.io.StringReader;

import org.testng.annotations.Test;

@Test
public class ReaderScanTest extends ScanTest {

    @Override
    public Scanner scanner(String string) throws IOException {
        return new ReaderScanner(new StringReader(string));
    }

}
