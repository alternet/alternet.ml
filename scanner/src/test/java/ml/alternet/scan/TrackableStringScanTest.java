package ml.alternet.scan;

import java.io.IOException;

import org.testng.annotations.Test;

@Test
public class TrackableStringScanTest extends StringScanTest {

    @Override
    public Scanner scanner(String string) throws IOException {
        return super.scanner(string).asTrackable();
    }

}
