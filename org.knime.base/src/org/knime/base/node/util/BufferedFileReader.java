/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 27, 2007 (ohl): created
 */
package org.knime.base.node.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.knime.core.util.FileUtil;

/**
 * A reader that counts the number of bytes read.
 *
 * @author ohl, University of Konstanz
 */
public final class BufferedFileReader extends BufferedReader {

    private static final int LINELENGTH = 2048;

    private final ByteCountingStream m_countingStream;

    private final long m_streamSize;

    /*
     * the line we are currently reading from
     */
    private StringBuilder m_currentLine = new StringBuilder(LINELENGTH);

    private StringBuilder m_tmpLine = new StringBuilder(LINELENGTH);

    private int m_length = 0;

    private ZipInputStream m_zippedSource = null;

    private String m_zipEntryName = null;

    // set only after the first entry was read entirely!
    private boolean m_hasMoreEntries = false;

    /*
     * next points to the next char in the currentLine that is to be returned,
     * or at length() if the line got fully returned (and a new line must be
     * read from the underlying stream). And its value is -1 if the EOF has been
     * reached, or the stream is closed. Initialized with 0 to trigger reading
     * of next line.
     */
    private int m_next = 0;

    private long m_currentLineNumber = 0;

    /**
     * Should only be instantiated by the methods provided. The specified stream
     * m must be the source of the stream in, from which we read.
     */
    private BufferedFileReader(final InputStreamReader in,
            final ByteCountingStream m, final long streamSize) {
        super(in);
        m_countingStream = m;
        m_streamSize = streamSize;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * @return the number of bytes read from the inner stream. If the input
     *         stream is a zipped stream, it returns the number of bytes read
     *         from this zipped stream. It the EOF of the stream has been read
     *         once, the returned number might not be accurate anymore.
     */
    public long getNumberOfBytesRead() {
        return m_countingStream.bytesRead();
    }

    /**
     * @return the total byte count of the stream, if this stream was created
     *         from a file (and the file delivered its file size). Zero
     *         otherwise!
     */
    public long getFileSize() {
        return m_streamSize;
    }

    /**
     * Returns the line currently read (if the \n char was already read, it
     * still returns the current/last line). If no characters were read so far,
     * the empty string is returned.<br>
     * In contrast to {@link #readLine} this method doesn't modify the stream
     * (doesn't read from it!) and it returns the entire line (also the
     * characters at the beginning of the line if they were read already).
     *
     * @return the line that is currently read. Returns the empty string, if no
     *         character was read as of yet, and null if the reader was closed.
     * @throws IOException if the stream has been closed
     */
    public String getCurrentLine() throws IOException {
        synchronized (lock) {

            checkOpen();

            if (m_currentLine.length() == 0) {
                // no characters were read before
                return "";
            }

            // remove the LF from the end of the string.
            int endIdx = m_currentLine.length() - 1;
            if (m_currentLine.charAt(endIdx) == '\n') {
                endIdx--;
            }
            if ((endIdx >= 0) && (m_currentLine.charAt(endIdx) == '\r')) {
                endIdx--;
            }
            if (endIdx < 0) {
                return "";
            }
            return m_currentLine.substring(0, endIdx + 1);
        }
    }

    /**
     * @return the number of the line currently read. It returns zero if no
     *         character was read yet.
     * @throws IOException if the stream has been closed
     */
    public long getCurrentLineNumber() throws IOException {
        synchronized (lock) {
            checkOpen();
            return m_currentLineNumber;
        }
    }

    /**
     * Reads the next line from the underlying reader and stores it in our
     * currentLine variable from which this reader reads the next chars. The
     * '\n' character is stored in the currentLine.
     *
     */
    private void readNextLine() throws IOException {
        // we can't use super.readLine() as it swallows \n characters (which is
        // kind of important at the end of the file).
        m_tmpLine.setLength(0);
        while (true) {
            int c = super.read();
            if (c == -1) {
                // reached EOF
                /*
                 * if we reached EOF we can - in a ZIP archive - check
                 * inexpensively if there is another entry following
                 */
                if (m_zippedSource != null) {
                    m_hasMoreEntries = m_zippedSource.getNextEntry() != null;
                }
                break;
            }

            m_tmpLine.append((char)c);

            if (c == '\n') {
                break;
            }
        }

        if (m_tmpLine.length() == 0) {
            // we are at the EOF
            // (keep the currentLine until they close the reader)
            m_length = -1;
            m_next = -1;
        } else {
            // swap tmp and the currentLine (reuse the mem next time around)
            StringBuilder s = m_currentLine;
            m_currentLine = m_tmpLine;
            m_tmpLine = s;

            m_length = m_currentLine.length();
            m_next = 0;
            m_currentLineNumber++;
        }
    }

    /**
     * Always use this to get the next character from the underlying reader. It
     * returns the next char of the currentLine or sets everything to indicate
     * the EOF, if the underlying reader is done.<br>
     * NOTE: this method is not synchronized and does not check the open status.
     *
     * @return either the next char from the currentLine string, or -1 if the
     *         EOF is reached and sets all flags to the appropriate states then.
     * @throws IOException if something goes wrong during reading
     */
    private int readNextChar() throws IOException {
        if (m_next == m_length) {
            readNextLine();
        }

        if (m_next == -1) {
            return -1;
        }

        // as we store \n in the currentLine it can never be empty.
        assert m_length > 0;

        return m_currentLine.charAt(m_next++);

    }

    /*
     * ---------------------------------------------------------------------
     * overriding implementations of all read methods.
     * ---------------------------------------------------------------------
     */

    /**
     * Check to make sure that the stream has not been closed. Throws an
     * IOException if it has been closed.
     */
    private void checkOpen() throws IOException {
        if ((m_currentLine == null) && (m_next == -1)) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        super.close();
        m_currentLine = null;
        m_length = -1;
        m_next = -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mark(final int readAheadLimit) throws IOException {
        // we don't need it - let's not support it.
        throw new IOException("Mark/Reset not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        synchronized (lock) {
            checkOpen();
            return readNextChar();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final char[] cbuf, final int off, final int len)
            throws IOException {
        synchronized (lock) {
            checkOpen();
            if ((off < 0) || (off > cbuf.length) || (len < 0)
                    || ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int count = 0;
            int buf = off;
            while (count < len) {
                int c = readNextChar();
                if (c == -1) {
                    // if we didn't read no character, return -1
                    return (count == 0 ? -1 : count);
                }

                cbuf[buf++] = (char)c;
                count++;
            }

            return count;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readLine() throws IOException {
        synchronized (lock) {
            checkOpen();

            if (m_next == m_length) {
                readNextLine();
            }
            if (m_next == -1) {
                // end of file.
                return null;
            }

            // we can return the unreturned (i.e. not read yet) part of
            // currentLine
            // - but need to remove the \n from the end (and a possible \r).
            int endIdx = m_length - 1;
            if (m_currentLine.charAt(endIdx) == '\n') {
                endIdx--;
            }
            if ((endIdx >= 0) && (m_currentLine.charAt(endIdx) == '\r')) {
                endIdx--;
            }

            if (m_next > endIdx) {
                // everything up to (but not including) the \n was read
                // (this also handles an empty line where endIdx is negative)
                m_next = m_length;
                // as readLine doesn't return \n we need to set 'next' to
                // 'length' to indicate we've returned everything
                return "";
            }

            // as readLine doesn't return \n we need to set 'next' to
            // 'length' to indicate we've returned everything
            int startIdx = m_next;
            m_next = m_length;
            return m_currentLine.substring(startIdx, endIdx + 1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ready() throws IOException {
        if ((m_next >= 0) && (m_next < m_length)) {
            return true;
        }
        return super.ready();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() throws IOException {
        throw new IOException("Mark/Reset not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long skip(final long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException(
                    "Can't skip a negative number of characters");
        }
        synchronized (lock) {
            checkOpen();

            long skipped = 0;

            while (skipped < n) {

                if (n <= skipped + (m_length - m_next)) {
                    // we can satisfy it with the currentLine
                    m_next += n - skipped;
                    return n;
                }

                // discard the currentLine and read a new one in
                skipped += m_length - m_next;
                m_next = m_length;
                readNextLine();
                if (m_next < 0) {
                    // hit the EOF
                    return skipped;
                }

            }

            return n;

        }

    }

    /**
     * Creates a new reader from the specified location with the default
     * character set from the Java VM. The returned reader can be asked for the
     * number of bytes read from the stream ({@link #getNumberOfBytesRead()}),
     * and, if the location specifies a local file - and the size of it can be
     * retrieved - the overall byte count in the stream
     * ({@link #getFileSize()}).<br>
     * If the specified file is compressed, it will try to create a ZIP stream
     * (and the byte counts refer both to the compressed file).<br>
     * In addition this reader can be asked for the current line it is reading
     * from ({@link #getCurrentLine()})and the current line number
     * ({@link #getCurrentLineNumber()}).
     *
     *
     * @param dataLocation the URL of the source to read from. If it is zipped
     *            it will try to open a ZIP stream on it.
     * @return reader reading from the specified location.
     * @throws IOException if something went wrong when opening the stream.
     */
    public static BufferedFileReader createNewReader(final URL dataLocation)
            throws IOException {
        // creates one with the default character set
        return createNewReader(dataLocation, null);
    }

    /**
     * Creates a new reader from the specified location with the default
     * character set from the Java VM. The returned reader can be asked for the
     * number of bytes read from the stream ({@link #getNumberOfBytesRead()}),
     * and, if the location specifies a local file - and the size of it can be
     * retrieved - the overall byte count in the stream
     * ({@link #getFileSize()}).<br>
     * If the specified file is compressed, it will try to create a ZIP stream
     * (and the byte counts refer both to the compressed file).<br>
     * In addition this reader can be asked for the current line it is reading
     * from ({@link #getCurrentLine()})and the current line number
     * ({@link #getCurrentLineNumber()}).
     *
     * @param dataLocation the URL of the source to read from. If it is zipped
     *            it will try to open a ZIP stream on it.
     * @param charsetName the character set to use. Must be supported by the VM
     * @return reader reading from the specified location.
     *
     * @throws IOException if something went wrong when opening the stream.
     * @throws java.nio.charset.IllegalCharsetNameException If the given charset
     *             name is illegal
     * @throws java.nio.charset.UnsupportedCharsetException If no support for
     *             the named charset is available in this instance of the Java
     *             virtual machine
     */
    public static BufferedFileReader createNewReader(final URL dataLocation,
            final String charsetName) throws IOException {

        if (dataLocation == null) {
            throw new NullPointerException("Can't open a stream on a null "
                    + "location");
        }

        try {

            Charset cs = Charset.defaultCharset();
            if (charsetName != null) {
                cs = Charset.forName(charsetName);
            }

            // if non-null, the source is zipped and we are reading this entry
            ZipInputStream zipStream = null;
            String zipEntryName = null;

            // stream passed to the reader (either zipped or unzipped stream)
            InputStreamReader readerStream;
            // the stream used to get the byte count from
            ByteCountingStream sourceStream =
                    new ByteCountingStream(new BufferedInputStream(
                            FileUtil.openStreamWithTimeout(dataLocation)));

            try {
                // first see if its a GZIPped file
                readerStream =
                        new InputStreamReader(
                                new GZIPInputStream(sourceStream), cs);
            } catch (Exception e) {
                // if not, it could be a ZIPped file
                sourceStream.close(); // close and reopen
                sourceStream =
                        new ByteCountingStream(new BufferedInputStream(
                            FileUtil.openStreamWithTimeout(dataLocation)));

                try {
                    zipStream = new ZipInputStream(sourceStream);
                    readerStream = new InputStreamReader(zipStream, cs);
                    // go to the first zip archive entry
                    ZipEntry ze = zipStream.getNextEntry();
                    if (ze == null) {
                        // not a zip archive...
                        zipStream = null;
                        readerStream = null;
                    } else {
                        if (ze.getName() != null) {
                            zipEntryName = ze.getName();
                        }
                    }

                } catch (Exception ie) {
                    // if something explodes, use a regular reader.
                    readerStream = null;
                }
            }

            // couldn't figure out which reader to use? Take a regular one.
            if (readerStream == null) {
                sourceStream.close(); // close and reopen
                sourceStream =
                        new ByteCountingStream(new BufferedInputStream(
                                FileUtil.openStreamWithTimeout(dataLocation)));
                readerStream = new InputStreamReader(sourceStream, cs);

            }

            // see if the underlying source is a file and we can get the size of
            // it
            long fileSize = 0;
            try {
                File f = FileUtil.getFileFromURL(dataLocation);
                if (f.exists()) {
                    fileSize = f.length();
                }
            } catch (Exception e) {
                // then don't give them a filesize.
            }

            BufferedFileReader result =
                    new BufferedFileReader(readerStream, sourceStream,
                            fileSize);
            if (zipStream != null) {
                // if it's a zipped source, store some stuff in the reader
                result.setZippedSource(zipStream);
                result.setZipEntryName(zipEntryName);
            }
            return result;

        } catch (Exception e) {
            // a npe flies, if windows tries to open an URL with a space
            String msg = e.getMessage();
            if (msg == null) {
                msg = e.getClass().getSimpleName() + ": <no details>";
            }
            throw new IOException("Can't access '"
                    + dataLocation + "'. (" + msg + ")");
        }
    }

    /**
     * Same as the method above ({@link #createNewReader(URL)}), but with an
     * input stream as argument. The {@link #getFileSize()} method of the
     * created reader always returns zero.
     *
     * @param in the stream to read from
     * @return a new buffered reader with some extra functionality (compared to
     *         the {@link BufferedReader}), but no file size (even if the
     *         stream reads from a file).
     */
    public static BufferedFileReader createNewReader(final InputStream in) {
        if (in == null) {
            throw new NullPointerException("Can't open a reader on a null "
                    + "input stream");
        }

        // the stream used to get the byte count from
        ByteCountingStream sourceStream = new ByteCountingStream(in);
        InputStreamReader readerStream = new InputStreamReader(sourceStream);

        return new BufferedFileReader(readerStream, sourceStream, 0);

    }

    private void setZipEntryName(final String name) {
        m_zipEntryName = name;
    }

    private void setZippedSource(final ZipInputStream zipStream) {
        m_zippedSource = zipStream;
    }

    /**
     * If the underlying source is a ZIP archive this method returns the name of
     * the entry read. Otherwise null.
     *
     * @return the entry read, if the source is a ZIP archive, or null.
     */
    public String getZipEntryName() {
        return m_zipEntryName;
    }

    /**
     * @return true, if the underlying source is a ZIP archive (not gzip).
     */
    public boolean isZippedSource() {
        return m_zippedSource != null;
    }

    /**
     * @return true, if the underlying source is a ZIP archive, if the EOF of
     *         the first entry was read and if there are more entries in the
     *         archive. False, otherwise, especially if the (first) EOF was not
     *         read!
     */
    public boolean hasMoreZipEntries() {
        return m_hasMoreEntries;
    }

    /**
     * Wraps an input stream and counts the number of bytes read from the
     * stream.
     *
     * @author ohl, University of Konstanz
     */
    public static final class ByteCountingStream extends InputStream {

        private final InputStream m_in;

        private long m_byteCount;

        /**
         * @param in the input stream to wrap and count the bytes read.
         */
        public ByteCountingStream(final InputStream in) {
            m_in = in;
            m_byteCount = 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            m_byteCount++; // inaccurate if EOF is met
            return m_in.read();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(final byte[] b) throws IOException {
            int r = m_in.read(b);
            m_byteCount += r; // inaccurate if EOF is met
            return r;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(final byte[] b, final int off, final int len)
                throws IOException {
            int r = m_in.read(b, off, len);
            m_byteCount += r; // inaccurate if EOF is met
            return r;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long skip(final long n) throws IOException {
            long r = m_in.skip(n);
            m_byteCount += r;
            return r;
        }

        /**
         * @return the number of bytes read from the stream. As soon as the end
         *         of the stream is reached this number is not accurate anymore.
         *         (As it also counts the "end-of-file" tags returned.)
         */
        public long bytesRead() {
            return m_byteCount;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean markSupported() {
            // not supporting mark - we don't need that.
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void reset() throws IOException {
            throw new IOException("Mark/Reset not supported");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void mark(final int readlimit) {
            throw new IllegalStateException("Mark/Reset not supported");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            m_in.close();
        }

    }

}
