/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
