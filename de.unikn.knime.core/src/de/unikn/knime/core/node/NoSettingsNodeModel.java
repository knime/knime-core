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
 *   04.03.2005 (georg): created
 */
package de.unikn.knime.core.node;



/**
 * Convenience model class that make some empty stub implementations of methods
 * that are not used by nodes that are not configurable (e.g. have no settings)
 * 
 * @author Florian Georg, University of Konstanz
 */
public abstract class NoSettingsNodeModel extends NodeModel {

    /**
     * @param nrInputs number of in ports
     * @param nrOutputs number of out ports
     */
    protected NoSettingsNodeModel(final int nrInputs,
            final int nrOutputs) {
        super(nrInputs, nrOutputs);

    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettings)
     */
    protected final void saveSettingsTo(final NodeSettings settings) {
        // nothing

    }

    /**
     * @see NodeModel#validateSettings(NodeSettings)
     */
    protected final void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
        // nothing
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettings)
     */
    protected final void loadValidatedSettingsFrom(
            final NodeSettings settings) throws InvalidSettingsException {
        // nothing

    }

    /**
     * @see NodeModel#reset()
     */
    protected void reset() {
        // default: do nothing
    }

   
}
