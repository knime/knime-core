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
 *   24.10.2005 (gabriel): created
 */
package org.knime.core.node;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.node.config.Config;



/**
 * This ModelContent is used to stored models. This class needs to be
 * modified to provide more functions to store model parts PMML like.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ModelContent extends Config 
    implements ModelContentRO, ModelContentWO {

    /**
     * Creates new content object. 
     * @param key The key for this ModelContent.
     */
    public ModelContent(final String key) {
        super(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Config getInstance(final String key) {
        return new ModelContent(key);
    }

    /**
     * Reads <code>ModelContent</code> settings from the given XML stream 
     * and returns a new <code>ModelContent</code> object.
     * 
     * @param in XML input stream to read settings from.
     * @return A new settings object.
     * @throws IOException If the stream could not be read.
     * @throws NullPointerException If one of the arguments is 
     *         <code>null</code>.
     */
    public static synchronized ModelContentRO loadFromXML(
            final InputStream in) throws IOException {
        ModelContent tmpSettings = new ModelContent("ignored");
        return (ModelContent) Config.loadFromXML(tmpSettings, in);
    }
    
    /**
     * {@inheritDoc}
     */
    public void addModelContent(final ModelContent modelContent) {
        super.addConfig(modelContent);
    }

    /**
     * {@inheritDoc}
     */
    public ModelContentWO addModelContent(final String key) {
        return (ModelContent) super.addConfig(key);
    }

    /**
     * {@inheritDoc}
     */
    public ModelContent getModelContent(final String key)
            throws InvalidSettingsException {
        return (ModelContent) super.getConfig(key);
    }

    /** File name that's used for the save and load (object-) methods. */
    private static final String FILE_NAME = "model.xml.gz";
    
    /** Saves this object to a directory. This method is used when (derived)
     * objects represent a {@link PortObject}.
     * @param directory Where to save to.
     * @param exec To report progress to.
     * @throws IOException If saving fails for IO problems.
     * @throws CanceledExecutionException If canceled.
     * @see #load(InputStream)
     */
    final void save(final File directory, final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        exec.setMessage("Saving model container to file");
        OutputStream out = new GZIPOutputStream(new BufferedOutputStream(
                new FileOutputStream(new File(directory, FILE_NAME))));
        exec.checkCanceled();
        saveToXML(out);
    }
    
    /** Load this object from a directory. This method is used when (derived)
     * objects represent a {@link PortObject}.
     * @param directory Where to load from
     * @param exec To report progress to.
     * @throws IOException If loading fails for IO problems.
     * @throws CanceledExecutionException If canceled.
     * @see #save(File, ExecutionMonitor)
     */
    final void load(final File directory, final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        exec.setMessage("Loading model container from file");
        InputStream in = new GZIPInputStream(new BufferedInputStream(
                new FileInputStream(new File(directory, FILE_NAME))));
        exec.checkCanceled();
        load(in);
    }
    
}
