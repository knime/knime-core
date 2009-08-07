/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.node;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.knime.core.node.config.Config;


/**
 * This class overwrites the general <code>Config</code> object and
 * specializes some method to access <code>NodeSettings</code> object. This
 * object is used within the node packages.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class NodeSettings extends Config 
        implements NodeSettingsRO, NodeSettingsWO {

    /**
     * Creates a new instance of this object with the given key.
     * 
     * @param key An identifier.
     */
    public NodeSettings(final String key) {
        super(key);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Config getInstance(final String key) {
        return new NodeSettings(key);
    }

    /**
     * @see Config#readFromFile(java.io.ObjectInputStream)
     */
    public static NodeSettings readFromFile(
            final ObjectInputStream ois) throws IOException {
        return (NodeSettings)Config.readFromFile(ois);
    }

    /**
     * Reads <code>NodeSettings</code> object from a given XML input stream and 
     * writes them into the given <code>NodeSettings</code> object.
     *
     * @param in XML input stream to read settings from.
     * @return A new settings object.
     * @throws IOException If the stream could not be read.
     * @throws NullPointerException If one of the arguments is 
     *         <code>null</code>.
     */
    public static NodeSettingsRO loadFromXML(
            final InputStream in) throws IOException {
        NodeSettings tmp = new NodeSettings("ignored");
        return (NodeSettingsRO) Config.loadFromXML(tmp, in);
    }


    /**
     * {@inheritDoc}
     */
    public NodeSettingsWO addNodeSettings(final String key) {
        return (NodeSettings)super.addConfig(key);
    }

    /**
     * {@inheritDoc}
     */
    public void addNodeSettings(final NodeSettings settings) {
        super.addConfig(settings);
    }


    /**
     * {@inheritDoc}
     */
    public NodeSettings getNodeSettings(final String key)
            throws InvalidSettingsException {
        return (NodeSettings)super.getConfig(key);
    }
    
}
