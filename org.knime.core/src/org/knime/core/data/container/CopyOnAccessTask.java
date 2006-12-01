/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.util.FileUtil;

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
class CopyOnAccessTask {

    /** To read from. */
    private final File m_file;
    /** The spec corresponding to the table in m_file. */
    private final DataTableSpec m_spec;
    
    /**
     * Keeps reference, nothing else.
     * @param file To read from.
     * @param spec The spec to the table in <code>file</code>.
     */
    CopyOnAccessTask(final File file, final DataTableSpec spec) {
        m_file = file;
        m_spec = spec;
    }
    
    /**
     * Called to start the copy process. Is only called once.
     * @return The buffer instance reading from the temp file. 
     */
    final Buffer createBuffer() {
        try {
            ZipInputStream inStream = new ZipInputStream(
                    new BufferedInputStream(new FileInputStream(m_file)));
            ZipEntry entry;
            File binFile = DataContainer.createTempFile();
            // we only need to read from this file while being in 
            // this method; temp file is deleted later on.
            File metaTempFile = File.createTempFile("meta", ".xml");
            metaTempFile.deleteOnExit();
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
                }
                if (isMetaFound && isDataFound) {
                    break;
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
            InputStream metaIn = new BufferedInputStream(
                    new FileInputStream(metaTempFile));
            Buffer buffer = createBuffer(binFile, m_spec, metaIn);
            metaIn.close();
            metaTempFile.delete();
            return buffer;
        } catch (IOException i) {
            throw new RuntimeException("Exception while accessing file " 
                    + m_file.getAbsolutePath() + ": " + i.getMessage(), i);
        }
    }
    
    /**
     * Creates the buffer instance, called from {@link #createBuffer()} method. 
     * This method is overridden in an inner class in 
     * {@link RearrangeColumnsTable} to return a {@link NoKeyBuffer}.
     * @param tempFile Copied temp file.
     * @param spec The DTS.
     * @param metaIn The meta input stream.
     * @return The buffer instance.
     * @throws IOException If that fails.
     */
    Buffer createBuffer(final File tempFile, final DataTableSpec spec, 
            final InputStream metaIn) throws IOException {
        return new Buffer(tempFile, spec, metaIn);
    }
}
