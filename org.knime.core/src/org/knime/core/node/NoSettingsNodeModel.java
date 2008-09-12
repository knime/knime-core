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
 *   04.03.2005 (georg): created
 */
package org.knime.core.node;



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
     * {@inheritDoc}
     */
    @Override
    protected final void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void loadValidatedSettingsFrom(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // default: do nothing
    }
}
