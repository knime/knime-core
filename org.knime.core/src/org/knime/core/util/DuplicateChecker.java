/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 */
package org.knime.core.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

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
    /** The default chunk size. */
    public static final int MAX_CHUNK_SIZE = 100000;

    /** The default number of streams open during merging. */
    public static final int MAX_STREAMS = 50;

    private final int m_maxChunkSize;

    private final int m_maxStreams;

    private Set<String> m_chunk = new HashSet<String>();

    private List<File> m_storedChunks = new ArrayList<File>();

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
     *            the merge process
     */
    public DuplicateChecker(final int maxChunkSize, final int maxStreams) {
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
        // bug fix #1737: keys may be just wrappers of very large strings ...
        // we make a copy, which consist of the important characters only
        if (!m_chunk.add(new String(s))) {
            throw new DuplicateKeyException(s);
        }
        if (m_chunk.size() >= m_maxChunkSize) {
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
        writeChunk();
        checkForDuplicates(m_storedChunks);
    }

    /**
     * Clears the checker, i.e. removes all temporary files and all keys in
     * memory.
     */
    public void clear() {
        for (File f : m_storedChunks) {
            f.delete();
        }
        m_storedChunks.clear();
        m_chunk.clear();
    }

    /**
     * Checks for duplicates.
     * 
     * @param storedChunks the list of chunk files to process
     * @throws NumberFormatException should not happen
     * @throws IOException if an I/O error occurs
     * @throws DuplicateKeyException if a duplicate key has been detected
     */
    private void checkForDuplicates(final List<File> storedChunks)
            throws NumberFormatException, IOException, DuplicateKeyException {
        final int nrChunks =
                (int)Math.ceil(storedChunks.size() / (double)m_maxStreams);
        List<File> newChunks = new ArrayList<File>(nrChunks);

        int chunkCount = 0;
        for (int i = 0; i < nrChunks; i++) {
            BufferedReader[] in = 
                new BufferedReader[Math.min(
                        m_maxStreams, storedChunks.size() - chunkCount)];
            if (in.length == 1) {
                // only one (remaining) chunk => no need to merge anything
                newChunks.add(storedChunks.get(chunkCount++));
                break;
            }

            int entries = 0;
            PriorityQueue<Helper> heap = new PriorityQueue<Helper>(in.length);
            for (int j = 0; j < in.length; j++) {
                in[j] = new BufferedReader(new FileReader(
                        storedChunks.get(chunkCount++)));
                int count = Integer.parseInt(in[j].readLine());
                entries += count;

                if (count > 0) {
                    String s = in[j].readLine();
                    heap.add(new Helper(s, j));
                }
            }

            final File f = 
                File.createTempFile("KNIME_DuplicateChecker", ".txt");
            f.deleteOnExit();
            newChunks.add(f);
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            out.write(Integer.toString(entries));
            out.newLine();

            String lastKey = null;

            while (entries-- > 0) {
                Helper top = heap.poll();
                if (top.m_s.equals(lastKey)) {
                    out.close();
                    StringBuilder b = new StringBuilder(top.m_s.length());
                    for (int k = 0; k < lastKey.length(); k++) {
                        char c = lastKey.charAt(k);
                        switch (c) {
                        // all sequences starting with '%' are encoded
                        // special characters
                        case '%' : 
                            char[] array = new char[2];
                            array[0] = lastKey.charAt(++k); 
                            array[1] = lastKey.charAt(++k);
                            int toHex = Integer.parseInt(new String(array), 16);
                            b.append((char)(toHex));
                            break;
                        default :
                            b.append(c);
                        }
                    }
                    throw new DuplicateKeyException(b.toString());
                }
                lastKey = top.m_s;

                if (nrChunks > 1) {
                    out.write(top.m_s);
                    out.newLine();
                }

                String next = in[top.m_streamIndex].readLine();
                if (next != null) {
                    top.m_s = next;
                    heap.add(top);
                }
            }

            out.close();
        }

        if (newChunks.size() > 1) {
            checkForDuplicates(newChunks);
        }
        for (File f : newChunks) {
            f.delete();
        }
    }

    /**
     * Writes the current chunk to disk and clears the set.
     * 
     * @throws IOException if an I/O error occurs
     */
    private void writeChunk() throws IOException {
        if (m_chunk.isEmpty()) {
            return;
        }
        String[] sorted = m_chunk.toArray(new String[m_chunk.size()]);
        m_chunk.clear();
        Arrays.sort(sorted);

        File f = File.createTempFile("KNIME_DuplicateChecker", ".txt");
        f.deleteOnExit();

        BufferedWriter out = new BufferedWriter(new FileWriter(f));
        out.write(Integer.toString(sorted.length));
        out.newLine();
        for (String s : sorted) {
            // line breaking characters need to be escaped in order for
            // readLine to work correctly
            StringBuilder buf = new StringBuilder(s.length() + 20);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                case '%':  buf.append("%25"); break;
                case '\n': buf.append("%0A"); break;
                case '\r': buf.append("%0D"); break;
                default: buf.append(c);
                }
            }
            out.write(buf.toString());
            out.newLine();
        }
        out.close();

        m_storedChunks.add(f);
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
        public int compareTo(final Helper o) {
            return m_s.compareTo(o.m_s);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_s;
        }
    }

}
