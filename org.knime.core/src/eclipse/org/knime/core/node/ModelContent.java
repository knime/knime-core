/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * This ModelContent is used to store XML-like model settings.
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
