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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.knime.core.node.config.Config;
import org.knime.core.node.port.PortObject;



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

    /** Saves this object to an output stream. This method is used when 
     * (derived) objects represent a {@link PortObject}.
     * @param out Where to save to.
     * @param exec To report progress to.
     * @throws IOException If saving fails for IO problems.
     * @throws CanceledExecutionException If canceled.
     * @see #load(InputStream)
     */
    final void save(final OutputStream out, final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        exec.setMessage("Saving model container to file");
        exec.checkCanceled();
        saveToXML(out);
    }
    
    /** Load this object from a directory. This method is used when (derived)
     * objects represent a {@link PortObject}.
     * @param in Where to load from
     * @param exec To report progress to.
     * @throws IOException If loading fails for IO problems.
     * @throws CanceledExecutionException If canceled.
     * @see #save(OutputStream, ExecutionMonitor)
     */
    final void load(final InputStream in, final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        exec.setMessage("Loading model container from file");
        exec.checkCanceled();
        load(in);
    }
    
}
