/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 * Created on Sep 12, 2012 by wiswedel
 */
package org.knime.core.data.blob;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.HexDump;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.knime.core.data.DataCell;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * Factory to create {@link DataCell} objects implementing the {@link BinaryObjectDataValue} interface.
 * There should be only one instance of this class per node execution (as files created in the workflow
 * follow a certain naming schema.)
 *
 * <p>The cells created by this factory are either instances of {@link BinaryObjectDataCell} or
 * {@link BinaryObjectFileStoreDataCell}, depending on their size.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.7
 */
public final class BinaryObjectCellFactory {

    /** System property to adjust the memory limit for "small" binary object.
     * Default is {@value #DEFAULT_MEMORY_LIMIT} bytes. Valid values are count in bytes or values like "1M" */
    public static final String PROPERTY_MEMORY_LIMIT = "org.knime.binaryobject.memorylimit";
    /** Default memory limit for small cells ({@value #DEFAULT_MEMORY_LIMIT} bytes). */
    public static final int DEFAULT_MEMORY_LIMIT = 4 * 1024; // 4kB

    private static final int MEMORY_LIMIT;
    private static final File TMP_DIR_FOLDER;

    static {
        long l = ConvenienceMethods.readSizeSystemProperty(PROPERTY_MEMORY_LIMIT, DEFAULT_MEMORY_LIMIT);
        if (l > Integer.MAX_VALUE) {
            MEMORY_LIMIT = DEFAULT_MEMORY_LIMIT;
        } else {
            MEMORY_LIMIT = (int)l;
        }
        TMP_DIR_FOLDER = new File(System.getProperty("java.io.tmpdir"));
    }

    private final FileStoreFactory m_fileStoreFactory;
    private int m_fileNameIndex;

    /** Create new cell factory based on a node's execution context. The argument object is used to
     * create an extension of {@link org.knime.core.data.filestore.FileStoreCell} that is used to keep the data
     * for large binary objects.
     * @param exec Non-null execution context used to create file store cells
     *             (using {@link ExecutionContext#createFileStore(String)}.
     */
    public BinaryObjectCellFactory(final ExecutionContext exec) {
        this(FileStoreFactory.createWorkflowFileStoreFactory(exec));
    }

    /** Factory of binary objects, which are <b>not</b> associated with the workflow. This constructor
     * should only be used if a factory is used for temporary, non-persistent objects (e.g. in views).
     */
    public BinaryObjectCellFactory() {
        this(FileStoreFactory.createNotInWorkflowFileStoreFactory());
    }

    /** Private constructor that just sets the fields.
     * @param fileStoreFactory ...
     */
    private BinaryObjectCellFactory(final FileStoreFactory fileStoreFactory) {
        m_fileNameIndex = 0;
        m_fileStoreFactory = fileStoreFactory;
    }

    /** Creates a new cell given a byte array.
     * @param bytes The byte array to wrap. Argument must not be null and must not be changed
     * after the call (data is not copied).
     * @return A new data cell wrapping the byte array content.
     * @throws IOException In case of IO problems when large byte arrays are written to a file store.
     * @throws NullPointerException If argument is null
     */
    public DataCell create(final byte[] bytes) throws IOException {
        if (bytes.length < MEMORY_LIMIT) {
            byte[] md5sum = newMD5Digest().digest(bytes);
            return new BinaryObjectDataCell(bytes, md5sum);
        }
        return create(new ByteArrayInputStream(bytes));
    }

    /** Creates cell given by reading from an input stream. The stream will be closed by this method.
     * @param input To read from.
     * @return A cell with a copy of the byte content.
     * @throws IOException If that fails (stream not readable, file store not writable, close problems, ...)
     * @throws NullPointerException If argument is null.
     */
    public DataCell create(final InputStream input) throws IOException {
        String uniqueFileName = "knime-binary-copy-";
        String suffix = ".bin";
        MessageDigest md5MessageDigest = newMD5Digest();
        DeferredFileOutputStream outStream = new DeferredFileOutputStream(
            MEMORY_LIMIT, uniqueFileName, suffix, TMP_DIR_FOLDER);
        DigestInputStream digestInputStream = new DigestInputStream(input, md5MessageDigest);
        IOUtils.copy(digestInputStream, outStream);
        digestInputStream.close();
        outStream.close();
        byte[] md5sum = md5MessageDigest.digest();
        if (outStream.isInMemory()) {
            return new BinaryObjectDataCell(outStream.getData(), md5sum);
        } else {
            FileStore fs;
            synchronized (this) {
                String name = "binaryObject-" + m_fileNameIndex++;
                fs = m_fileStoreFactory.createFileStore(name);
            }
            File f = outStream.getFile();
            assert f.exists() : "File " + f.getAbsolutePath() + " not created by file output stream";
            FileUtils.moveFile(f, fs.getFile());
            return new BinaryObjectFileStoreDataCell(fs, md5sum);
        }
    }

    /** Get new MD5 digest from system.
     * @return ...
     * @throws IOException ...
     */
    private MessageDigest newMD5Digest() throws IOException {
        MessageDigest md5MessageDigest;
        try {
            md5MessageDigest = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new IOException("Couldn't get MD5 digest from system", e);
        }
        return md5MessageDigest;
    }

    /** Utility method to get a hex dump of a binary input stream. Used in the value renderer.
     * @param in The input stream to read from (close will be called!)
     * @param length The maximum length to read (e.g. 1024 * 1024 * 1024 for a MB)
     * @return The hex dump, possible with a new line that the dump is truncated
     * @throws IOException If reading the stream fails.
     */
    public static String getHexDump(final InputStream in, final int length) throws IOException {
        byte[] bs = new byte[length];
        int i = 0;
        while (i < length) {
            int read = in.read(bs, i, length - i);
            if (read < 0) {
                break;
            }
            i += read;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HexDump.dump(bs, 0, out, 0);
        String s = new String(out.toByteArray());
        if (in.read() >= 0) {
            s = s.concat("...");
        }
        in.close();
        return s;
    }


}
