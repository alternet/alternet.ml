package ml.alternet.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A filter that ignore {@link InputStream#close()}.
 *
 * @author Philippe Poulard
 */
public class NoCloseInputStream extends FilterInputStream {

    /**
     * Create a NoCloseInputStream.
     *
     * @param in The input stream.
     */
    public NoCloseInputStream( InputStream in ) {
        super( in );
    }

    /**
     * Do not close.
     * Use {@link #doClose()} instead.
     *
     * @see #doClose()
     * @see InputStream#close()
     */
    public void close() throws IOException {}

    /**
     * Really close the input.
     *
     * @see InputStream#close()
     */
    public void doClose() throws IOException {
        super.close();
    }

}
