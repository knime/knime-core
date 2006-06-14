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
 *   24.10.2005 (gabriel): created
 */
package de.unikn.knime.core.node;

import java.io.IOException;
import java.io.InputStream;

import de.unikn.knime.core.node.config.Config;


/**
 * This PredictorParams is used to stored models. This class needs to be
 * modified to provide more functions to store model parts PMML like.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class PredictorParams extends NodeSettings {

    /**
     * Hides public default constructor.
     * 
     * @param key The key for this PredictorParams.
     */
    public PredictorParams(final String key) {
        super(key);
    }

    /**
     * @see de.unikn.knime.core.node.config.Config#getInstance(java.lang.String)
     */
    @Override
    public PredictorParams getInstance(final String key) {
        return new PredictorParams(key);
    }

    /**
     * Creates and returns a new PredictorParams with the given key.
     * 
     * @param key The key for the new PredictorParams.
     * @return A <code>PredictorParams</code> object.
     * 
     * @see de.unikn.knime.core.node.NodeSettings#getConfig(String)
     */
    @Override
    public PredictorParams addConfig(final String key) {
        return (PredictorParams)super.addConfig(key);
    }

    /**
     * Returns the PredictorParams for the given key.
     * 
     * @param key The key to retrieve the PredictorParams for.
     * @return The PredictorParams object.
     * @throws InvalidSettingsException The PredictorParams could not be found.
     */
    @Override
    public PredictorParams getConfig(final String key)
            throws InvalidSettingsException {
        return (PredictorParams)super.getConfig(key);
    }
    
    /**
     * Reads <code>PredictorParams</code> settings from the given XML stream 
     * and returns a new <code>PredictorParams</code> object. The root will be
     * read.
     * 
     * @param in XML input stream to read settings from.
     * @return A new settings object.
     * @throws IOException If the stream could not be read.
     * @throws NullPointerException If one of the arguments is 
     *         <code>null</code>.
     */
    public static synchronized PredictorParams loadFromXML(
            final InputStream in) throws IOException {
        return PredictorParams.loadFromXML(in, true);
    }
    
    /**
     * Reads <code>PredictorParams</code> settings from the given XML stream 
     * and returns a new <code>PredictorParams</code> object.
     * 
     * @param in XML input stream to read settings from.
     * @param readRoot If the root element should be read from XML.
     * @return A new settings object.
     * @throws IOException If the stream could not be read.
     * @throws NullPointerException If one of the arguments is 
     *         <code>null</code>.
     */
    public static synchronized PredictorParams loadFromXML(
            final InputStream in, final boolean readRoot) throws IOException {
        PredictorParams tmpSettings = new PredictorParams("ignored");
        return (PredictorParams) Config.loadFromXML(tmpSettings, in, readRoot);
    }

}
