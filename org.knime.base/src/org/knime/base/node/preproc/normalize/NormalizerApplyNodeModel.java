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
 *   Oct 17, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.normalize;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.normalize.AffineTransTable;
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

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class NormalizerApplyNodeModel extends NodeModel {

    private ModelContentRO m_content;
    
    /**
     * Constructor.
     */
    public NormalizerApplyNodeModel() {
        super(1, 1, 1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_content == null) {
            // if the model is not available yet, we still are ready.
            return new DataTableSpec[1];
        }
        DataTableSpec s = AffineTransTable.createSpec(inSpecs[0], m_content);
        return new DataTableSpec[]{s};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        AffineTransTable t = AffineTransTable.load(inData[0], m_content);
        BufferedDataTable bdt = exec.createBufferedDataTable(t, exec);
        if (t.getErrorMessage() != null) {
            setWarningMessage(t.getErrorMessage());
        }
        return new BufferedDataTable[]{bdt};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadModelContent(
            final int index, final ModelContentRO predParams) 
        throws InvalidSettingsException {
        if (predParams == null) {
            m_content = null;
        } else {
            m_content = predParams.getModelContent(
                    NormalizerNodeModel.CFG_MODEL_NAME);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec) 
    throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(
            final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

}
