/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Mar 27, 2007 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * A reader that counts the number of bytes read.
 * 
 * @author ohl, University of Konstanz
 */
public final class BufferedFileReader extends BufferedReader {

    private static final String ZIP_ENDING = ".gz";

    private final ByteCountingStream m_countingStream;

    private final long m_streamSize;

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
     * Creates a new reader from the specified location. The returned reader can
     * be asked for the number of bytes read from the stream, and, if the
     * location specifies a local file - and the size of it can be retrieved -
     * the overall byte count in the stream. If the specified file ends with
     * ".gz", it will try to create a ZIP stream (and the byte counts refer both
     * to the compressed file).
     * 
     * @param dataLocation the URL of the source to read from. If it ends with
     *            ".gz" it will try to open a ZIP stream on it.
     * @return reader reading from the specified location.
     * @throws IOException if something went wrong when opening the stream.
     */
    public static BufferedFileReader createNewReader(final URL dataLocation)
            throws IOException {

        if (dataLocation == null) {
            throw new NullPointerException("Can't open a stream on a null "
                    + "location");
        }

        // the stream used to get the byte count from
        ByteCountingStream sourceStream =
                new ByteCountingStream(dataLocation.openStream());
        // stream passed to the reader (maybe replaced with the zipped stream)
        InputStreamReader readerStream = new InputStreamReader(sourceStream);

        if (dataLocation.toString().endsWith(ZIP_ENDING)) {
            // if the file ends with ".gz" try opening a zip stream on it
            try {
                readerStream = new InputStreamReader(
                                new GZIPInputStream(sourceStream), 
                                "ISO-8859-1");
            } catch (IOException ioe) {
                // the exception will fly if the specified file is not a zip
                // file. Keep regular stream then.
            }
        }

        // see if the underlying source is a file and we can get the size of it
        long fileSize = 0;
        try {
            File dataFile = new File(dataLocation.toURI());
            if (dataFile.exists()) {
                fileSize = dataFile.length();
            }
        } catch (URISyntaxException use) {
            // then don't givem a filesize.
        }

        return new BufferedFileReader(readerStream, sourceStream, fileSize);
    }

    /**
     * Wraps an input stream and counts the number of bytes read from the
     * stream.
     * 
     * @author ohl, University of Konstanz
     */
    static class ByteCountingStream extends InputStream {

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

    }
}
