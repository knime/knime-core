/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
    PredictorParams(final String key) {
        super(key);
    }

    /**
     * @see de.unikn.knime.core.node.config.Config#getInstance(java.lang.String)
     */
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
    public PredictorParams getConfig(final String key)
            throws InvalidSettingsException {
        return (PredictorParams)super.getConfig(key);
    }

}
