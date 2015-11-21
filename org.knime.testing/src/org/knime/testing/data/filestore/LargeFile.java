/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 27, 2012 (wiswedel): created
 */
package org.knime.testing.data.filestore;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.knime.core.data.filestore.FileStore;

/** Held in {@link LargeFileStoreCell} and {@link LargeFileStorePortObject} - it contains the "content" (a seed hidden
 * in binary garbage).
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class LargeFile {

    public static final int SIZE_OF_FILE = 1024;

    /** Non null if kept in memory (special option in create node). */
    private byte[] m_binaryContent;
    private final FileStore m_fileStore;

    /** Constructor like factory method. */
    public static LargeFile create(final FileStore fs, final long seed, final boolean keepInMemory) throws IOException {
        return new LargeFile(fs, seed, keepInMemory);
    }

    /** Deserialization factory method. */
    public static LargeFile restore(final FileStore fs) {
        return new LargeFile(fs);
    }

    /** Deserialization constructor. */
    LargeFile(final FileStore fs) {
        m_fileStore = fs;
    }

    private LargeFile(final FileStore fs, final Long seed, final boolean keepInMemory) throws IOException {
        m_fileStore = fs;
        OutputStream output;
        if (keepInMemory) {
            output = new ByteArrayOutputStream();
        } else {
            File file = m_fileStore.getFile();
            output = new FileOutputStream(file);
        }
        write(seed, output);
        if (keepInMemory) {
            m_binaryContent = ((ByteArrayOutputStream)output).toByteArray();
        }
    }

    /** @return the fileStore */
    public FileStore getFileStore() {
        return m_fileStore;
    }

    public synchronized void flushToFileStore() throws IOException {
        if (m_binaryContent != null) {
            FileUtils.writeByteArrayToFile(m_fileStore.getFile(), m_binaryContent);
            m_binaryContent = null;
        }
    }

    void write(final long seed, final OutputStream output) throws IOException {
        Random random = new Random(seed);
        DataOutputStream out = new DataOutputStream(output);
        byte[] ar = new byte[SIZE_OF_FILE / 2];
        random.nextBytes(ar);
        out.write(ar);
        out.writeUTF(Long.toString(seed));
        random.nextBytes(ar);
        out.write(ar);
        out.close();
    }

    /** Read the seed that is hidden in the underlying file store. Used by nodes to compare to the reference seed.
     * @return the seed in the binary content.
     * @throws IOException If reading fails due to I/O issues. */
    public long read() throws IOException {
        DataInputStream input;
        if (m_binaryContent != null) {
            input = new DataInputStream(new ByteArrayInputStream(m_binaryContent));
        } else {
            File file = m_fileStore.getFile();
            input = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        }
        for (int i = 0; i < SIZE_OF_FILE / 2; i++) {
            input.readByte();
        }
        String identifier = input.readUTF();
        for (int i = 0; i < SIZE_OF_FILE / 2; i++) {
            input.readByte();
        }
        input.close();
        return Long.parseLong(identifier);
    }

}
