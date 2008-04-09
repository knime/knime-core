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
import java.text.NumberFormat;
import java.util.Map;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer.BufferCreator;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
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

    /** To read from. */
    private final ReferencedFile m_fileRef;
    /** The spec corresponding to the table in m_fileRef. */
    private final DataTableSpec m_spec;
    /** The buffer's id used for blob (de)serialization. */
    private final int m_bufferID;
    /** Repository of tables in the workflow for blob (de)serialization. */
    private final Map<Integer, ContainerTable> m_tableRep;
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
     * @param tblRep Repository of tables for blob (de)serialization.
     * @param creator To instantiate the buffer object.
     */
    CopyOnAccessTask(final ReferencedFile fileRef, final DataTableSpec spec, 
            final int bufferID, final Map<Integer, ContainerTable> tblRep,
            final BufferCreator creator) {
        m_fileRef = fileRef;
        m_spec = spec;
        m_bufferID = bufferID;
        m_tableRep = tblRep;
        m_bufferCreator = creator;
    }
    
    /**
     * Called to start the copy process. Is only called once.
     * @return The buffer instance reading from the temp file.
     * @throws IOException If the file can't be accessed. 
     */
    final Buffer createBuffer() throws IOException {
        // timer task which prints a INFO message that the copying 
        // is in progress.
        TimerTask timerTask = null;
        m_fileRef.lock();
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
            ZipInputStream inStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(file)));
            ZipEntry entry;
            File binFile = DataContainer.createTempFile();
            File blobDir = null;
            // we only need to read from this file while being in 
            // this method; temp file is deleted later on.
            File metaTempFile = File.createTempFile("meta", ".xml");
            metaTempFile.deleteOnExit();
            DataTableSpec spec = m_spec;
            boolean isSpecFound = m_spec != null;
            boolean isDataFound = false;
            boolean isMetaFound = false;
            while ((entry = inStream.getNextEntry()) != null) {
                String name = entry.getName(); 
                if (name.equals(Buffer.ZIP_ENTRY_DATA)) {
                    OutputStream output = new BufferedOutputStream(
                            new FileOutputStream(binFile));
                    FileUtil.copy(inStream, output);
                    inStream.closeEntry();
                    output.close();
                    isDataFound = true;
                } else if (name.equals(Buffer.ZIP_ENTRY_META)) {
                    OutputStream output = new BufferedOutputStream(
                            new FileOutputStream(metaTempFile));
                    FileUtil.copy(inStream, output);
                    inStream.closeEntry();
                    output.close();
                    isMetaFound = true;
                } else if (name.startsWith(Buffer.ZIP_ENTRY_BLOBS)) {
                    if (blobDir == null) {
                        blobDir = Buffer.createBlobDirNameForTemp(binFile);
                    }
                    copyEntryToDir(entry, inStream, blobDir);
                } else if (name.equals(DataContainer.ZIP_ENTRY_SPEC) 
                        && !isSpecFound) {
                    InputStream nonClosableStream = 
                        new NonClosableZipInputStream(inStream);
                    @SuppressWarnings("unchecked") // cast with generics
                    NodeSettingsRO settings = 
                        NodeSettings.loadFromXML(nonClosableStream);
                    try {
                        NodeSettingsRO specSettings = settings.getNodeSettings(
                                DataContainer.CFG_TABLESPEC);
                        spec = DataTableSpec.load(specSettings);
                        isSpecFound = true;
                    } catch (InvalidSettingsException ise) {
                        IOException ioe = new IOException(
                                "Unable to read spec from file " + file);
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
                throw new IOException("No entry " + DataContainer.ZIP_ENTRY_SPEC
                        + " in file: " + file.getAbsolutePath());
            } 
            InputStream metaIn = new BufferedInputStream(
                    new FileInputStream(metaTempFile));
            Buffer buffer = m_bufferCreator.createBuffer(
                    binFile, blobDir, spec, metaIn, m_bufferID, m_tableRep);
            if (m_needsRestoreIntoMemory) {
                buffer.restoreIntoMemory();
            }
            metaIn.close();
            metaTempFile.delete();
            return buffer;
        } finally {
            if (timerTask != null) {
                timerTask.cancel();
            }
            m_fileRef.unlock();
        }
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
    
    /** Get table repository in workflow for blob (de)serialization.
     * @return table repository reference
     */
    Map<Integer, ContainerTable> getTableRepository() {
        return m_tableRep;
    }
    
    /**
     * Adds the current entry of the zip input stream to the destination
     * directory. Used to copy the blobs from the zip file to /temp/.
     */
    private static void copyEntryToDir(final ZipEntry entry, 
            final ZipInputStream in, final File blobDir) throws IOException {
        String path = entry.getName();
        if (path.startsWith(Buffer.ZIP_ENTRY_BLOBS + "/")) {
            path = path.substring((Buffer.ZIP_ENTRY_BLOBS + "/").length());
        }
        File f = new File(blobDir, path);
        if (entry.isDirectory()) {
            if (!f.mkdirs()) {
                throw new IOException("Unable to create temporary directory " 
                        + f.getName());
            }
        } else {
            InputStream inStream = new BufferedInputStream(in);
            OutputStream o = new BufferedOutputStream(new FileOutputStream(f));
            FileUtil.copy(inStream, o);
            o.close();
        }
    }

}
