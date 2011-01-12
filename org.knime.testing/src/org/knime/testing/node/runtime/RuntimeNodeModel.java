/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Nov 27, 2008 (wiswedel): created
 */
package org.knime.testing.node.runtime;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Abstract node model whose execute method must be defined
 * inline.
 * @author wiswedel, University of Konstanz
 */
public abstract class RuntimeNodeModel extends NodeModel {

    /**
     * @param nrInDataPorts
     * @param nrOutDataPorts
     */
    protected RuntimeNodeModel(int nrInDataPorts, int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) {
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(NodeSettingsRO settings)
            throws InvalidSettingsException {
    }
    
    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return null;
    }
    
}
