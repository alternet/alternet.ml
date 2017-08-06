package ml.alternet.security.web.server;

//
//========================================================================
//Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//All rights reserved. This program and the accompanying materials
//are made available under the terms of the Eclipse Public License v1.0
//and Apache License v2.0 which accompanies this distribution.
//
//  The Eclipse Public License is available at
//  http://www.eclipse.org/legal/epl-v10.html
//
//  The Apache License v2.0 is available at
//  http://www.opensource.org/licenses/apache2.0.php
//
//You may elect to redistribute this code under either of these licenses.
//========================================================================
//

/* ------------------------------------------------------------ */
/**
 * UTF-8 StringBuilder, based on the same Jetty class + additional code
 * in order to allow char extraction and cleaning.
 *
 * This class wraps a standard {@link java.lang.StringBuilder} and provides methods to append
 * UTF-8 encoded bytes, that are converted into characters.
 *
 * This class is stateful and up to 4 calls to {@link #append(byte)} may be needed before
 * state a character is appended to the string buffer.
 *
 * The UTF-8 decoding is done by this class and no additional buffers or Readers are used.
 * The UTF-8 code was inspired by http://bjoern.hoehrmann.de/utf-8/decoder/dfa/
 *
 */
public class Utf8StringBuilder extends Utf8Appendable {

    final StringBuilder _buffer;

    public Utf8StringBuilder()
    {
        super(new StringBuilder());
        _buffer=(StringBuilder)_appendable;
    }

    public Utf8StringBuilder(int capacity)
    {
        super(new StringBuilder(capacity));
        _buffer=(StringBuilder)_appendable;
    }

    /**
     * Return the content as chars
     *
     * @return A char array of the UTF-8 encoded content.
     */
    public char[] toChars() {
        checkState();
        StringBuilder sb = getStringBuilder();
        char[] c = new char[sb.length()];
        sb.getChars(0, sb.length(), c, 0);
        return c;
    }

    /**
     * Clear all the char in this content.
     */
    public void clear() {
        StringBuilder sb = getStringBuilder();
        for (int i = 0 ; i < sb.length() ; i++) {
            sb.setCharAt(i, ' ');
        }
    }

    @Override
    public int length()
    {
        return _buffer.length();
    }

    @Override
    public void reset()
    {
        super.reset();
        _buffer.setLength(0);
    }

    public StringBuilder getStringBuilder()
    {
        checkState();
        return _buffer;
    }

    @Override
    public String toString()
    {
        checkState();
        return _buffer.toString();
    }


}
