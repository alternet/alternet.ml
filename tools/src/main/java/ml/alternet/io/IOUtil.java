package ml.alternet.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ml.alternet.misc.OmgException;
import ml.alternet.misc.Thrower;
import ml.alternet.util.Util;

/**
 * A set of constants and static methods related to I/O.
 *
 * @author Philippe Poulard
 */
@Util
public class IOUtil {

    /** EOF = -1 */
    public static final int EOF = -1;

    /** The void input stream. */
    public static final InputStream VOID_INPUT_STREAM = new InputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }
    };

    /** The void character stream. */
    public static final Reader VOID_CHARACTER_STREAM = new Reader() {
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            return -1;
        }

        @Override
        public void close() throws IOException {
        }
    };

    /** The <tt>/dev/null</tt> output stream. */
    public static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
        }
    };

    /** The <tt>/dev/null</tt> output stream. */
    public static final Writer NULL_CHARACTER_STREAM = new Writer() {
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    };

    /** The <tt>/dev/null</tt> print stream. */
    public static final PrintStream NULL_PRINT_STREAM = new PrintStream(NULL_OUTPUT_STREAM);

    /** The default buffer size. */
    public static final int BUFFER_SIZE = 8192;

    /**
     * Close output streams that are not print streams.
     *
     * @param os
     *            The output stream to close.
     *
     * @throws IOException
     *             When an I/O exception occurs.
     *
     * @see PrintStream
     */
    public static void close(OutputStream os) throws IOException {
        if (!(os instanceof PrintStream)) {
            os.close();
        }
    }

    /**
     * Convenient method to read a byte stream as a character stream.
     *
     * @param input
     *            The stream of bytes to read.
     *
     * @return A character stream of bytes decoded in UTF-8.
     */
    public static Reader asReader(InputStream input) {
        return asReader(input, StandardCharsets.UTF_8);
    }

    /**
     * Convenient method to read a byte stream as a character stream.
     *
     * @param input
     *            The stream of bytes to read.
     * @param charset
     *            Specify how to decode the bytes.
     *
     * @return A character stream of bytes properly decoded.
     */
    public static Reader asReader(InputStream input, Charset charset) {
        return new InputStreamReader(input, charset);
    }

    /**
     * Convenient method to read a character stream as an input stream.
     *
     * @param input
     *            The stream of characters to read.
     *
     * @return A byte stream of characters encoded in UTF-8.
     *
     * @throws IOException
     *             When an I/O error occurs.
     */
    public static InputStream asInputStream(Reader input) throws IOException {
        return asInputStream(input, StandardCharsets.UTF_8);
    }

    /**
     * Convenient method to read a character stream as an input stream.
     *
     * @param input
     *            The stream of characters to read.
     * @param charset
     *            Specify how to encode the character stream.
     *
     * @return A byte stream of characters properly encoded.
     *
     * @throws IOException
     *             When an I/O error occurs.
     */
    public static InputStream asInputStream(final Reader input, final Charset charset) throws IOException {
        return new InputStreamAggregator(new Iterator<InputStream>() {
            CharBuffer cb = CharBuffer.allocate(IOUtil.BUFFER_SIZE);
            ByteBuffer bb = ByteBuffer.allocate(IOUtil.BUFFER_SIZE);
            CharsetEncoder enc = charset.newEncoder();
            int n = 0;

            @Override
            public void remove() {
            }

            @Override
            public InputStream next() {
                CoderResult res = this.enc.encode(this.cb, this.bb, this.n == -1);
                if (res == CoderResult.UNDERFLOW) { // all data consumed or more
                                                    // data expected
                    ByteArrayInputStream bais = new ByteArrayInputStream(this.bb.array(), 0, this.bb.position());
                    this.bb.clear();
                    this.cb.compact();
                    this.n = 0;
                    return bais;
                } else {
                    throw new OmgException();
                }
            }

            @Override
            public boolean hasNext() {
                if (this.n == 0) {
                    try {
                        this.n = input.read(this.cb);
                        if (this.n == 0) {
                            // there must be enough room in the buffer
                            throw new OmgException("No remaining in the Buffer"); // buffer
                                                                                  // is
                                                                                  // large
                                                                                  // enough
                                                                                  // to
                                                                                  // be
                                                                                  // processed
                        } else {
                            this.cb.flip();
                        }
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }
                return this.cb.hasRemaining();
            }
        });
    }

    /**
     * Convenient method to write a character stream as an output stream.
     *
     * @param output
     *            The stream of characters to write.
     *
     * @return A byte stream of characters encoded in UTF-8.
     */
    public static OutputStream asOutputStream(Writer output) {
        return asOutputStream(output, StandardCharsets.UTF_8);
    }

    /**
     * Convenient method to write a character stream as an output stream.
     *
     * @param output
     *            The stream of characters to write.
     * @param charset
     *            Specify how to decode the character stream.
     *
     * @return A byte stream of characters properly decoded.
     */
    public static OutputStream asOutputStream(final Writer output, Charset charset) {
        final CharsetDecoder dec = charset.newDecoder();
        dec.onMalformedInput(CodingErrorAction.REPLACE);
        dec.onUnmappableCharacter(CodingErrorAction.REPLACE);
        return new OutputStream() {
            ByteBuffer bb = ByteBuffer.allocate(IOUtil.BUFFER_SIZE);
            CharBuffer cb = CharBuffer.allocate(IOUtil.BUFFER_SIZE);

            @Override
            public void write(int b) throws IOException {
                if (this.bb.remaining() == 0) {
                    decode(false);
                }
                this.bb.put((byte) b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                while (len > 0) {
                    if (this.bb.remaining() == 0) {
                        decode(false);
                    }
                    int size = Math.min(this.bb.remaining(), len);
                    this.bb.put(b, off, size);
                    off += size;
                    len -= size;
                }
            }

            @Override
            public void flush() throws IOException {
                decode(false);
                output.write(this.cb.array(), 0, this.cb.position());
                this.cb.clear();
                output.flush();
            }

            @Override
            public void close() throws IOException {
                decode(true);
                output.write(this.cb.array(), 0, this.cb.position());
                output.close();
                this.bb = null;
                this.cb = null;
            }

            void decode(boolean endOfInput) throws IOException {
                this.bb.flip();
                while (true) {
                    CoderResult res = dec.decode(this.bb, this.cb, endOfInput);
                    if (res == CoderResult.OVERFLOW) {
                        output.write(this.cb.array(), 0, this.cb.position());
                        this.cb.clear();
                    } else if (res == CoderResult.UNDERFLOW) { // all data
                                                               // consumed or
                                                               // more data
                                                               // expected
                        this.bb.compact();
                        return;
                    } else {
                        res.throwException();
                    }
                }
            }
        };
    }

    /**
     * Convenient method to write an output stream as a character stream.
     *
     * @param output
     *            The stream of bytes to write.
     *
     * @return A character stream of bytes encoded in UTF-8.
     */
    public static Writer asWriter(OutputStream output) {
        return asWriter(output, StandardCharsets.UTF_8);
    }

    /**
     * Convenient method to write an output stream as a character stream.
     *
     * @param output
     *            The stream of bytes to write.
     * @param charset
     *            Specify how to encode the character stream.
     *
     * @return A character stream of bytes properly encoded.
     */
    public static Writer asWriter(OutputStream output, Charset charset) {
        return new OutputStreamWriter(output, charset);
    }

    /**
     * Convert a reader to a stream of characters.
     *
     * @param data The reader.
     *
     * @return A stream of characters.
     */
    public static Stream<Character> asStream(Reader data) {
        return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<Character>(Long.MAX_VALUE,
                        Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {
                    @Override
                    public boolean tryAdvance(Consumer<? super Character> action) {
                        int c = Thrower.safeCall(() -> data.read());
                        if (c == EOF) {
                            return false;
                        } else {
                            action.accept((char) c);
                            return true;
                        }
                    }
                },
            false);
    }

    /**
     * Convert an input stream to a stream of ints.
     *
     * @param data The input stream.
     *
     * @return A stream of ints.
     */
    public static IntStream asStream(InputStream data) {
        return StreamSupport.intStream(
            new Spliterators.AbstractIntSpliterator(Long.MAX_VALUE,
                    Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {
                @Override
                public boolean tryAdvance(IntConsumer action) {
                    int i = Thrower.safeCall(() -> data.read());
                    if (i == EOF) {
                        return false;
                    } else {
                        action.accept(i);
                        return true;
                    }
                }
            },
        false);
    }

    /**
     * Read a resource line by line using UTF-8.
     *
     * @param resource The input source.
     * @return A stream of lines.
     */
    public static Stream<String> readLines(InputStream resource) {
        return readLines(new InputStreamReader(resource, StandardCharsets.UTF_8));
    }

    /**
     * Read a resource line by line.
     *
     * @param resource The input source.
     * @return A stream of lines.
     */
    public static Stream<String> readLines(Reader resource) {
        BufferedReader lines = new BufferedReader(resource);
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<String>(Long.MAX_VALUE,
                Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL) {
            @SuppressWarnings("finally")
            @Override
            public boolean tryAdvance(Consumer<? super String> action) {
                try {
                    String line = lines.readLine();
                    if (line == null) {
                        lines.close();
                        return false;
                    } else {
                        action.accept(line);
                        return true;
                    }
                } catch (IOException e) {
                    try {
                        lines.close();
                    } finally {
                        return Thrower.doThrow(e);
                    }
                }
            }
        }, false);
    }

}
