/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Aug 31, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;

/**
 * Input Stream that's used to read from stream written by 
 * <code>LongUTFDataOuputStream</code>.
 * 
 * @see LongUTFDataOutputStream
 * @author wiswedel, University of Konstanz
 */
public class LongUTFDataInputStream extends InputStream 
    implements DataInput, Closeable {
    
    private final DataInputStream m_input;
    
    /** 
     * Constructor that wraps a given input stream.
     * @param input The input stream to read from.
     */
    public LongUTFDataInputStream(final DataInputStream input) {
        if (input == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_input = input;
    }

    /** {@inheritDoc} */
    @Override
    public int available() throws IOException {
        return m_input.available();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        m_input.close();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_input.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public void mark(final int readlimit) {
        m_input.mark(readlimit);
    }

    /** {@inheritDoc} */
    @Override
    public boolean markSupported() {
        return m_input.markSupported();
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return m_input.read();
    }

    /** {@inheritDoc} */
    @Override
    public int read(
            final byte[] b, final int off, final int len) throws IOException {
        return m_input.read(b, off, len);
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] b) throws IOException {
        return m_input.read(b);
    }

    /** {@inheritDoc} */
    @Override
    public boolean readBoolean() throws IOException {
        return m_input.readBoolean();
    }

    /** {@inheritDoc} */
    @Override
    public byte readByte() throws IOException {
        return m_input.readByte();
    }

    /** {@inheritDoc} */
    @Override
    public char readChar() throws IOException {
        return m_input.readChar();
    }

    /** {@inheritDoc} */
    @Override
    public double readDouble() throws IOException {
        return m_input.readDouble();
    }

    /** {@inheritDoc} */
    @Override
    public float readFloat() throws IOException {
        return m_input.readFloat();
    }

    /** {@inheritDoc} */
    @Override
    public void readFully(
            final byte[] b, final int off, final int len) throws IOException {
        m_input.readFully(b, off, len);
    }

    /** {@inheritDoc} */
    @Override
    public void readFully(final byte[] b) throws IOException {
        m_input.readFully(b);
    }

    /** {@inheritDoc} */
    @Override
    public int readInt() throws IOException {
        return m_input.readInt();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public String readLine() throws IOException {
        return m_input.readLine();
    }

    /** {@inheritDoc} */
    @Override
    public long readLong() throws IOException {
        return m_input.readLong();
    }

    /** {@inheritDoc} */
    @Override
    public short readShort() throws IOException {
        return m_input.readShort();
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedByte() throws IOException {
        return m_input.readUnsignedByte();
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedShort() throws IOException {
        return m_input.readUnsignedShort();
    }
    
    /** {@inheritDoc} */
    @Override
    public void reset() throws IOException {
        m_input.reset();
    }
    
    /** {@inheritDoc} */
    @Override
    public long skip(final long n) throws IOException {
        return m_input.skip(n);
    }
    
    /** {@inheritDoc} */
    @Override
    public int skipBytes(final int n) throws IOException {
        return m_input.skipBytes(n);
    }

    /** {@inheritDoc} */
    @Override
    public String readUTF() throws IOException {
        int s = readUnsignedShort();
        long utflen;
        if (s == LongUTFDataOutputStream.USE_LONG_UTF) {
            utflen = readLong();
        } else {
            utflen = s;
        }
        return readUTFBody(utflen);
    }
    
    /** maximum data block length. */
    private static final int MAX_BLOCK_SIZE = 1024;
    /** (tunable) length of char buffer (for reading strings). */
    private static final int CHAR_BUF_SIZE = 256;

    /** buffer for reading general/block data. */
    private final byte[] m_buf = new byte[MAX_BLOCK_SIZE];
    /** char buffer for fast string reads. */
    private final char[] m_cbuf = new char[CHAR_BUF_SIZE];
    
    private int m_pos;
    private int m_end;
    
    /**
     * Reads in the "body" (i.e., the UTF representation minus the 2-byte or
     * 8-byte length header) of a UTF encoding, which occupies the next utflen
     * bytes.
     */
    private String readUTFBody(final long utflength) throws IOException {
        long utflen = utflength;
        StringBuilder sbuf = new StringBuilder();
        m_pos = 0;
        m_end = 0;

        while (utflen > 0) {
            int avail = m_end - m_pos;
            if (avail >= 3 || avail == utflen) {
                long processed = readUTFSpan(sbuf, utflen);
                if (processed == 0) {
                    throw new UTFDataFormatException(
                            "Illegal number of UTF bytes");
                }
                utflen -= processed;
            } else {
                // shift and refill buffer manually
                if (avail > 0) {
                    System.arraycopy(m_buf, m_pos, m_buf, 0, avail);
                }
                m_pos = 0;
                m_end = (int)Math.min(MAX_BLOCK_SIZE, utflen);
                m_input.readFully(m_buf, avail, m_end - avail);
            }
        }
        return sbuf.toString();
    }

    /**
     * Reads span of UTF-encoded characters out of internal buffer (starting at
     * offset m_pos and ending at or before offset m_end), consuming no more 
     * than utflen bytes. Appends read characters to sbuf. Returns the number of
     * bytes consumed.
     */
    private long readUTFSpan(final StringBuilder sbuf, final long utflen)
            throws IOException {
        int cpos = 0;
        int start = m_pos;
        int avail = Math.min(m_end - m_pos, CHAR_BUF_SIZE);
        // stop short of last char unless all of utf bytes in buffer
        int stop = m_pos + ((utflen > avail) ? avail - 2 : (int)utflen);
        boolean outOfBounds = false;

        try {
            while (m_pos < stop) {
                int b1, b2, b3;
                b1 = m_buf[m_pos++] & 0xFF;
                switch (b1 >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7: // 1 byte format: 0xxxxxxx
                    m_cbuf[cpos++] = (char)b1;
                    break;

                case 12:
                case 13: // 2 byte format: 110xxxxx 10xxxxxx
                    b2 = m_buf[m_pos++];
                    if ((b2 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException();
                    }
                    m_cbuf[cpos++] = 
                        (char)(((b1 & 0x1F) << 6) | ((b2 & 0x3F) << 0));
                    break;

                case 14: // 3 byte format: 1110xxxx 10xxxxxx 10xxxxxx
                    b3 = m_buf[m_pos + 1];
                    b2 = m_buf[m_pos + 0];
                    m_pos += 2;
                    if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException();
                    }
                    m_cbuf[cpos++] = (char)(((b1 & 0x0F) << 12)
                            | ((b2 & 0x3F) << 6) | ((b3 & 0x3F) << 0));
                    break;

                default: // 10xx xxxx, 1111 xxxx
                    throw new UTFDataFormatException();
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            outOfBounds = true;
        } finally {
            if (outOfBounds || (m_pos - start) > utflen) {
                /*
                 * Fix for 4450867: if a malformed utf char causes the
                 * conversion loop to scan past the expected m_end of the utf
                 * string, only consume the expected number of utf bytes.
                 */
                m_pos = start + (int)utflen;
                throw new UTFDataFormatException();
            }
        }

        sbuf.append(m_cbuf, 0, cpos);
        return m_pos - start;
    }
}
