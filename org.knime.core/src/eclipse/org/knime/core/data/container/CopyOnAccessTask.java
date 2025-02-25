/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *
 * History
 *   Nov 28, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.Optional;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.container.BufferedDataContainerDelegate.BufferCreator;
import org.knime.core.data.container.BufferedDataContainerDelegate.NoKeyBufferCreator;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.util.FileUtil;
import org.knime.core.util.KNIMETimer;

/**
 * Opens (on demand) a zip file from the workspace location and copies the
 * binary data content to temp for further reading. This class creates
 * a {@link Buffer} which reads from the temp file.
 *
 * <p>Think of this class as an runnable that is executed once on demand. It
 * helps to delay the copy process of the data to speed up the loading of
 * saved workflows.
 * @author Bernd Wiswedel, University of Konstanz
 */
final class CopyOnAccessTask {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(CopyOnAccessTask.class);

    /** Delay im ms until copying process is reported to LOGGER, small
     * files won't report their copying (if faster than this threshold). */
    private static final long NOTIFICATION_DELAY = 3000;

    /** The node's context set at construction time. It will be set during copying (which happens asynchronously), to
     * guarantee that the copied file is extracted into the workflow's temp directory. Added as part of AP-24014. */
    private final Optional<NodeContext> m_contextOptional;
    /** To read from. */
    private final ReferencedFile m_fileRef;
    /** The spec corresponding to the table in m_fileRef. */
    private final DataTableSpec m_spec;
    /** The buffer's id used for blob (de)serialization. */
    private final int m_bufferID;
    /** file and table store repository. */
    private final IDataRepository m_dataRepository;
    /** To instantiate the buffer object. */
    private final BufferCreator m_bufferCreator;
    /** Flag to indicate that the buffer needs to restore its content
     * into memory once it is created. */
    private boolean m_needsRestoreIntoMemory;

    /**
     * Keeps reference, nothing else.
     * @param fileRef To read from.
     * @param spec The spec to the table in <code>file</code>.
     * @param bufferID The buffer's id used for blob (de)serialization.
     * @param dataRepository global data repository
     * @param creator To instantiate the buffer object.
     */
    CopyOnAccessTask(final ReferencedFile fileRef, final DataTableSpec spec,
            final int bufferID, final IDataRepository dataRepository,
            final boolean rowKeys) {
        m_fileRef = fileRef;
        m_spec = spec;
        m_bufferID = bufferID;
        m_dataRepository = dataRepository;
        m_bufferCreator = rowKeys ? new BufferCreator() : new NoKeyBufferCreator();
        m_contextOptional = Optional.ofNullable(NodeContext.getContext());
    }

    /**
     * Called to start the copy process. Is only called once.
     * @return The buffer instance reading from the temp file.
     * @throws IOException If the file can't be accessed.
     */
    Buffer createBuffer() throws IOException {
        // timer task which prints a INFO message that the copying
        // is in progress.
        TimerTask timerTask = null;
        m_fileRef.lock();
        m_contextOptional.ifPresent(NodeContext::pushContext);
        try {
            final File file = m_fileRef.getFile();
            timerTask = new TimerTask() {
                /** {@inheritDoc} */
                @Override
                public void run() {
                    double sizeInMB = file.length() / (double)(1 << 20);
                    String size = NumberFormat.getInstance().format(sizeInMB);
                    LOGGER.debug(
                            "Extracting data file \"" + file.getAbsolutePath()
                            + "\" to temp dir (" + size + "MB)");
                }
            };
            KNIMETimer.getInstance().schedule(timerTask, NOTIFICATION_DELAY);
            return createBuffer(
                    new BufferedInputStream(new FileInputStream(file)));
        } finally {
            if (timerTask != null) {
                timerTask.cancel();
            }
            m_fileRef.unlock();
            m_contextOptional.ifPresent(c -> NodeContext.removeLastContext());
        }
    }

    /**
     * Called to start the copy process. Is only called once.
     * @param in To read from, will instantiate a zip input stream on top of
     * it, which will call close() eventually
     * @return The buffer instance reading from the temp file.
     * @throws IOException If the file can't be accessed.
     */
    Buffer createBuffer(final InputStream in) throws IOException {
        ZipInputStream inStream = new ZipInputStream(in);
        ZipEntry entry;
        // file name ending may change later when meta info is read
        final String fallbackFileExtension = ".tmp";
        File binFile = DataContainer.createTempFile(fallbackFileExtension);
        File blobDir = null;
        File fileStoreDir = null;
        // we only need to read from this file while being in
        // this method; temp file is deleted later on.
        File metaTempFile = FileUtil.createTempFile("meta", ".xml", true);
        DataTableSpec spec = m_spec;
        boolean isSpecFound = m_spec != null;
        boolean isDataFound = false;
        boolean isMetaFound = false;
        while ((entry = inStream.getNextEntry()) != null) {
            String name = entry.getName();
            if (name.equals(Buffer.ZIP_ENTRY_DATA)) {
                try (OutputStream output = new BufferedOutputStream(new FileOutputStream(binFile))) {
                    FileUtil.copy(inStream, output);
                }
                inStream.closeEntry();
                isDataFound = true;
            } else if (name.equals(Buffer.ZIP_ENTRY_META)) {
                try (OutputStream output = new BufferedOutputStream(new FileOutputStream(metaTempFile))) {
                    FileUtil.copy(inStream, output);
                }
                inStream.closeEntry();
                isMetaFound = true;
            } else if (name.startsWith(Buffer.ZIP_ENTRY_BLOBS)) {
                if (blobDir == null) {
                    blobDir = Buffer.createBlobDirNameForTemp(binFile);
                }
                copyEntryToDir(entry, inStream, blobDir);
            } else if (name.startsWith(Buffer.ZIP_ENTRY_FILESTORES)) {
                if (fileStoreDir == null) {
                    fileStoreDir = FileUtil.createTempDir("knime_fs_datacontainer-");
                }
                copyEntryToDir(entry, inStream, fileStoreDir);
            } else if (name.equals(BufferedDataContainerDelegate.ZIP_ENTRY_SPEC)
                    && !isSpecFound) {
                InputStream nonClosableStream =
                    new NonClosableInputStream.Zip(inStream);
                NodeSettingsRO settings =
                    NodeSettings.loadFromXML(nonClosableStream);
                try {
                    NodeSettingsRO specSettings = settings.getNodeSettings(
                        BufferedDataContainerDelegate.CFG_TABLESPEC);
                    spec = DataTableSpec.load(specSettings);
                    isSpecFound = true;
                } catch (InvalidSettingsException ise) {
                    IOException ioe = new IOException(
                            "Unable to read spec from file");
                    ioe.initCause(ise);
                    throw ioe;
                }
            }
        }
        inStream.close();
        if (!isDataFound) {
            throw new IOException("No entry " + Buffer.ZIP_ENTRY_DATA
                    + " in file");
        }
        if (!isMetaFound) {
            throw new IOException("No entry " + Buffer.ZIP_ENTRY_META
                    + " in file");
        }
        if (!isSpecFound) {
            throw new IOException("No entry " + BufferedDataContainerDelegate.ZIP_ENTRY_SPEC
                    + " in file");
        }
        InputStream metaIn = new BufferedInputStream(
                new FileInputStream(metaTempFile));
        Buffer buffer =
            m_bufferCreator.createBuffer(binFile, blobDir, fileStoreDir, spec, metaIn, m_bufferID, m_dataRepository);
        // TODO fix the file ending of the temp file -- purely cosmetic change
        // the below currently doesn't work as we change the file name in the background and that breaks the reader
//        File binFileParent = binFile.getParentFile();
//        String binFileSimpleName = binFile.getName();
//        String binFileSimpleNameFixed = StringUtils.removeEnd(binFileSimpleName, fallbackFileExtension)
//                + buffer.getOutputFormat().getFilenameSuffix();
//        File binFileNew = new File(binFileParent, binFileSimpleNameFixed);
//        if (!binFileNew.exists()) {
//            binFile.renameTo(binFileNew); // we don't bother if that succeeds or not
//        }
        if (m_needsRestoreIntoMemory) {
            buffer.setRestoreIntoMemoryOnCacheMiss();
        }
        metaIn.close();
        metaTempFile.delete();
        return buffer;
    }

    /** Get name of file to copy from. Used for better error messages.
     * @return source file
     */
    String getFileName() {
        return m_fileRef.toString();
    }

    /** Get this buffer's ID.
     * @return the buffer ID or -1
     */
    int getBufferID() {
        return m_bufferID;
    }

    /** Requests the buffer to read its content into memory once it has
     * been created. */
    void setRestoreIntoMemory() {
        m_needsRestoreIntoMemory = true;
    }

    /** @return the dataRepository for blob and file store resolution. */
    IDataRepository getDataRepository() {
        return m_dataRepository;
    }

    /**
     * Adds the current entry of the zip input stream to the destination
     * directory. Used to copy the blobs from the zip file to /temp/.
     */
    private static void copyEntryToDir(final ZipEntry entry,
            final ZipInputStream in, final File tempDir) throws IOException {
        String path = entry.getName();
        if (path.startsWith(Buffer.ZIP_ENTRY_BLOBS + "/")) {
            path = path.substring((Buffer.ZIP_ENTRY_BLOBS + "/").length());
        }
        if (path.startsWith(Buffer.ZIP_ENTRY_FILESTORES + "/")) {
            path = path.substring((Buffer.ZIP_ENTRY_FILESTORES + "/").length());
        }

        File f = new File(tempDir, path);
        if (entry.isDirectory()) {
            Files.createDirectories(f.toPath());
        } else {
            try (OutputStream o = new FileOutputStream(f)) {
                FileUtil.copy(in, o);
            }
        }
    }
}
