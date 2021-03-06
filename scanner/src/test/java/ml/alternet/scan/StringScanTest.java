package ml.alternet.scan;

import java.io.IOException;

import org.testng.annotations.Test;

@Test
public class StringScanTest extends ScanTest {

    @Override
    public Scanner scanner(String string) throws IOException {
        return new StringScanner(string);
    }

}
