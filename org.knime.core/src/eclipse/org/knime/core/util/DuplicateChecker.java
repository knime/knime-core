/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * -------------------------------------------------------------------
 */
package org.knime.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Set;

import org.knime.core.node.KNIMEConstants;

/**
 * This class checks for duplicates in an (almost) arbitrary number of strings.
 * This can be used to check for e.g. unique row keys. The checking is done in
 * two stages: first new keys are added to a set. If the set already contains a
 * key an exception is thrown. If the set gets bigger than the maximum chunk
 * size it is written to disk and the set is cleared. If then after adding all
 * keys {@link #checkForDuplicates()} is called all created chunks are processed
 * and sorted by a merge sort like algorithm. If any duplicate keys are detected
 * during this process an exception is thrown.
 *
 * <p>Note: This implementation is not thread-safe, it's supposed to be used
 * by a single thread only.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class DuplicateChecker {
    private static class Chunk {
        private final File m_file;
        private DataOutputStream m_out;
        private long m_count = 0;

        public Chunk() throws IOException {
            m_file = File.createTempFile("KNIME_DuplicateChecker", ".bin");
            m_out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(m_file)));
        }

        public void addKeys(final Set<String> keys) throws IOException {
            if (m_out == null) {
                throw new IllegalStateException("Chunck has already been closed");
            }

            String[] sorted = keys.toArray(new String[keys.size()]);
            Arrays.sort(sorted);

            for (String s : sorted) {
                m_out.writeUTF(s);
                m_count++;
            }
        }

        public void addKey(final String key) throws IOException {
            if (m_out == null) {
                throw new IllegalStateException("Chunck has already been closed");
            }

            m_out.writeUTF(key);
            m_count++;
        }

        public void close() throws IOException {
            if (m_out == null) {
                throw new IllegalStateException("Chunck has already been closed");
            }
            m_out.close();
            m_out = null;
        }

        public Iterator<String> iterator() throws FileNotFoundException {
            if (m_out != null) {
                throw new IllegalStateException("Bucket has not been closed yet");
            }
            return new Iterator<String>() {
                private DataInputStream m_in;
                private long m_read;

                {
                    m_in = new DataInputStream(new BufferedInputStream(new FileInputStream(m_file)));
                }

                @Override
                public boolean hasNext() {
                    boolean b = (m_read < m_count);
                    if (!b && (m_in != null)) {
                        try {
                            m_in.close();
                            m_in = null;
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    return b;
                }

                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    try {
                        m_read++;
                        return m_in.readUTF();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove is not supported");
                }
            };
        }

        public long size() {
            return m_count;
        }

        public boolean dispose() {
            return m_file.delete();
        }
    }

    /** The default chunk size. */
    public static final int MAX_CHUNK_SIZE = 100000;

    /** The default number of streams open during merging. */
    public static final int MAX_STREAMS = 50;

    private final int m_maxChunkSize;

    private final int m_maxStreams;

    private Set<String> m_currentChunk = new HashSet<String>();

    private List<Chunk> m_storedChunks = new ArrayList<Chunk>();

    private static final boolean DISABLE_DUPLICATE_CHECK =
        Boolean.getBoolean(
                KNIMEConstants.PROPERTY_DISABLE_ROWID_DUPLICATE_CHECK);

    /** Custom hash set to keep list of to-be-deleted files, see bug 2966:
     * "DuplicateChecker always writes to disc (even for small tables) + temp
     * file names are hashed in core java (increased mem consumption for loops)"
     * for details. */
    private static final Collection<Chunk> ALL_CHUNKS = new ArrayList<Chunk>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void run() {
                removeTempFiles();
            }
        });
    }

    private static void removeTempFiles() {
        synchronized (ALL_CHUNKS) {
            for (Chunk c : ALL_CHUNKS) {
                c.dispose();
            }
            ALL_CHUNKS.clear();
        }
    }

    /**
     * Creates a new duplicate checker with default parameters.
     */
    public DuplicateChecker() {
        this(MAX_CHUNK_SIZE, MAX_STREAMS);
    }

    /**
     * Creates a new duplicate checker.
     *
     * @param maxChunkSize the size of each chunk, i.e. the maximum number of
     *            elements kept in memory
     * @param maxStreams the maximum number of streams that are kept open during
     *            the merge process, must be at least 2
     */
    public DuplicateChecker(final int maxChunkSize, final int maxStreams) {
        if (maxStreams < 2) {
            throw new IllegalArgumentException("The number of streams must be at least 2");
        }
        m_maxChunkSize = maxChunkSize;
        m_maxStreams = maxStreams;
    }

    /**
     * Adds a new key to the duplicate checker.
     *
     * @param s the key
     * @throws DuplicateKeyException if a duplicate within the current chunk has
     *             been detected
     * @throws IOException if an I/O error occurs while writing the chunk to
     *             disk
     */
    public void addKey(final String s) throws DuplicateKeyException,
            IOException {
        if (DISABLE_DUPLICATE_CHECK) {
            return;
        }
        // bug fix #1737: keys may be just wrappers of very large strings ...
        // we make a copy, which consist of the important characters only
        if (!m_currentChunk.add(new String(s))) {
            throw new DuplicateKeyException(s);
        }
        if (m_currentChunk.size() >= m_maxChunkSize) {
            writeChunk();
        }
    }

    /**
     * Checks for duplicates in all added keys.
     *
     * @throws DuplicateKeyException if a duplicate key has been detected
     * @throws IOException if an I/O error occurs
     */
    public void checkForDuplicates() throws DuplicateKeyException, IOException {
        if (m_storedChunks.size() == 0) {
            // less than MAX_CHUNK_SIZE keys, no need to write
            // a file because the check for duplicates has already
            // been done in addKey
            return;
        }
        writeChunk();
        checkForDuplicates(m_storedChunks);
    }

    /**
     * Clears the checker, i.e. removes all temporary files and all keys in
     * memory.
     */
    public void clear() {
        for (Chunk c : m_storedChunks) {
            c.dispose();
        }
        synchronized (ALL_CHUNKS) { ALL_CHUNKS.removeAll(m_storedChunks); }
        m_storedChunks.clear();
        m_currentChunk.clear();
    }

    /**
     * Checks for duplicates.
     *
     * @param storedChunks the list of chunk files to process
     * @throws NumberFormatException should not happen
     * @throws IOException if an I/O error occurs
     * @throws DuplicateKeyException if a duplicate key has been detected
     */
    private void checkForDuplicates(final List<Chunk> storedChunks)
            throws NumberFormatException, IOException, DuplicateKeyException {
        final int nrChunks =
                (int)Math.ceil(storedChunks.size() / (double)m_maxStreams);
        List<Chunk> newChunks = new ArrayList<Chunk>(nrChunks);

        int chunkCount = 0;
        for (int i = 0; i < nrChunks; i++) {
            @SuppressWarnings("unchecked")
            Iterator<String>[] in =
                new Iterator[Math.min(
                        m_maxStreams, storedChunks.size() - chunkCount)];
            if (in.length == 1) {
                // only one (remaining) chunk => no need to merge anything
                newChunks.add(storedChunks.get(chunkCount++));
                break;
            }

            long entries = 0;
            PriorityQueue<Helper> heap = new PriorityQueue<Helper>(in.length);
            for (int j = 0; j < in.length; j++) {
                Chunk c = storedChunks.get(chunkCount++);
                entries += c.size();
                in[j] = c.iterator();

                if (in[j].hasNext()) {
                    heap.add(new Helper(in[j].next(), j));
                }
            }

            Chunk chunk = new Chunk();
            synchronized (ALL_CHUNKS) { ALL_CHUNKS.add(chunk); }
            newChunks.add(chunk);

            String lastKey = null;
            while (entries-- > 0) {
                Helper top = heap.poll();
                if (top.m_s.equals(lastKey)) {
                    chunk.close();
                    throw new DuplicateKeyException(top.m_s);
                }
                lastKey = top.m_s;

                if (nrChunks > 1) {
                    chunk.addKey(top.m_s);
                }

                if (in[top.m_streamIndex].hasNext()) {
                    top.m_s = in[top.m_streamIndex].next();
                    heap.add(top);
                }
            }

            chunk.close();
        }

        if (newChunks.size() > 1) {
            checkForDuplicates(newChunks);
        }
        for (Chunk c : newChunks) {
            c.dispose();
        }
        synchronized (ALL_CHUNKS) { ALL_CHUNKS.removeAll(newChunks); }
    }

    /**
     * Writes the current chunk to disk and clears the set.
     *
     * @throws IOException if an I/O error occurs
     */
    private void writeChunk() throws IOException {
        if (m_currentChunk.isEmpty()) {
            return;
        }
        Chunk c = new Chunk();
        c.addKeys(m_currentChunk);
        c.close();
        m_storedChunks.add(c);
        m_currentChunk.clear();
    }

    /**
     * Container to hold a string and the stream index where the string
     * was read from.
     */
    private static final class Helper implements Comparable<Helper> {
        private String m_s;

        private final int m_streamIndex;

        private Helper(final String string, final int streamIdx) {
            m_s = string;
            m_streamIndex = streamIdx;
        }

        /** {@inheritDoc} */
        @Override
        public int compareTo(final Helper o) {
            return m_s.compareTo(o.m_s);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_s;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((m_s == null) ? 0 : m_s.hashCode());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Helper other = (Helper)obj;
            if (m_s == null) {
                if (other.m_s != null) {
                    return false;
                }
            } else if (!m_s.equals(other.m_s)) {
                return false;
            }
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        clear();
    }
}
