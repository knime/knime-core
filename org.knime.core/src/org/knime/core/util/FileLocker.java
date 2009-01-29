/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   25.07.2007 (sieb): created
 */
package org.knime.core.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

/**
 * This class can be used to get a lock on the specified file. The lock can be
 * aquired and released. If it is not explicitly released, it will be released
 * as soon as the JVM is terminated.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class FileLocker {
    private File m_lockFile;

    private FileLock m_fileLock;

    private RandomAccessFile m_raFile;

    /**
     * Creates a {@link FileLocker} on the given file. Creating the locker does
     * not mean, that the file is also locked. For this, use the methods lock
     * and release.
     * 
     * @param lockFile the file for which to aquire and release locks
     */
    public FileLocker(final File lockFile) {
        m_lockFile = lockFile;
    }

    /**
     * Tries to auquire the lock on the given {@link File}.
     * 
     * @return true, if the lock could be aquired, false otherwise
     * 
     * @throws IOException in case there is something wrong with the io actions
     */
    public synchronized boolean lock() throws IOException {
        m_raFile = new RandomAccessFile(m_lockFile, "rw");
        try {
            m_fileLock = m_raFile.getChannel().tryLock();
        } catch (IOException ioe) {
            // produce a more specific message for clients
            String specificMessage =
                    "Lock could not be aquired on: '" + m_lockFile
                            + "'. Reason: " + ioe.getMessage();

            throw new IOException(specificMessage);
        }
        if (m_fileLock != null) {
            return true;
        }
        m_raFile.close();
        m_raFile = null;
        return false;
    }

    /**
     * Releases a previously aquired lock on the specified file. If there was no
     * lock on the file, nothing happens.
     */
    public synchronized void release() {
        if (m_fileLock != null) {
            try {
                m_fileLock.release();
            } catch (IOException e) {
                // don't complain, this is a best effort to clean up
            }
            m_fileLock = null;
        }
        if (m_raFile != null) {
            try {
                m_raFile.close();
            } catch (IOException e) {
                // don't complain, this is a best effort to clean up
            }
            m_raFile = null;
        }
    }
}
