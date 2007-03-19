/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   March 10, 2007 (sieb): created
 */
package org.knime.testing.node.differModelContent;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.testing.node.differNode.TestEvaluationException;

/**
 * Checks two models for equality.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class DiffModelContentModel extends NodeModel {

    private ModelContentRO m_modelContent1;

    private ModelContentRO m_modelContent2;

    /**
     * Creates a model with two model inports.
     */
    public DiffModelContentModel() {
        super(0, 0, 2, 0);
    }

    /**
     * @see org.knime.core.node.NodeModel#saveSettingsTo(
     *      org.knime.core.node.NodeSettings)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(
     *      org.knime.core.node.NodeSettings)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // do nothing yet
    }

    /**
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(
     *      org.knime.core.node.NodeSettings)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // do nothing yet
    }

    /**
     * 
     * @see org.knime.core.node.NodeModel#execute(
     *      org.knime.core.node.BufferedDataTable[],
     *      org.knime.core.node.ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        if (!m_modelContent1.equals(m_modelContent2)) {
            throw new TestEvaluationException("The models are not the same.");
        }

        return new BufferedDataTable[]{};
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        // do nothing yet
    }

    /**
     * @see org.knime.core.node.NodeModel#configure(
     *      org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{};
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // do nothing yet

    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // do nothing yet

    }

    /**
     * @see org.knime.core.node.NodeModel# loadModelContent(int,
     *      org.knime.core.node.ModelContentRO)
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO predParams) throws InvalidSettingsException {

        if (index == 0) {
            m_modelContent1 = predParams;
        } else if (index == 1) {
            m_modelContent2 = predParams;
        } else {
            throw new InvalidSettingsException("Only models at port 0 and "
                    + "1 are expected not at index: " + index);
        }
    }
}
