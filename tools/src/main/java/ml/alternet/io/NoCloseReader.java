package ml.alternet.io;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A filter that ignore {@link Reader#close()}.
 *
 * @author Philippe Poulard
 */
public class NoCloseReader extends FilterReader {

    /**
     * Create a NoCloseReader.
     *
     * @param in The input reader.
     */
    public NoCloseReader( Reader in ) {
        super( in );
    }

    /**
     * Do not close.
     * Use {@link #doClose()} instead.
     *
     * @see java.io.Reader#close()
     *
     * @throws IOException Never thrown.
     */
    @Override
    public void close() throws IOException { }

    /**
     * Really close the input.
     *
     * @see java.io.Reader#close()
     *
     * @throws IOException When an I/O error occurs.
     */
    public void doClose() throws IOException {
        super.close();
    }

}
