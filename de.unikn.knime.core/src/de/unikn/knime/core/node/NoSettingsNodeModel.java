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
     * Creates a <code>NodeModel</code> based on data in- and outports.
     * @param nrInputs number of in ports
     * @param nrOutputs number of out ports
     */
    protected NoSettingsNodeModel(final int nrInputs,
            final int nrOutputs) {
        super(nrInputs, nrOutputs);
    }
    
    /**
     * Creates a new model with the given number of data, and predictor in- and
     * outputs.
     * 
     * @param nrDataIns The number of <code>DataTable</code> elements expected
     *            as inputs.
     * @param nrDataOuts The number of <code>DataTable</code> objects expected
     *            at the output.
     * @param nrPredParamsIns The number of <code>ModelContent</code>
     *            elements available as inputs.
     * @param nrPredParamsOuts The number of <code>ModelContent</code>
     *            objects available at the output.
     * @throws NegativeArraySizeException If the number of in- or outputs is
     *             smaller than zero.
     */
    protected NoSettingsNodeModel(final int nrDataIns, final int nrDataOuts,
            final int nrPredParamsIns, final int nrPredParamsOuts) {
        super(nrDataIns, nrDataOuts, nrPredParamsIns, nrPredParamsOuts);
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected final void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing

    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected final void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected final void loadValidatedSettingsFrom(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing

    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
        // default: do nothing
    }   
}
