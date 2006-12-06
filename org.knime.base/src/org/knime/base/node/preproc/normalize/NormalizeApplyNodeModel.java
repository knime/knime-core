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
public class NormalizeApplyNodeModel extends NodeModel {

    private ModelContentRO m_content;
    
    /**
     * Constructor.
     */
    public NormalizeApplyNodeModel() {
        super(1, 1, 1, 0);
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
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
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        AffineTransTable t = AffineTransTable.load(inData[0], m_content);
        return new BufferedDataTable[]{
                exec.createBufferedDataTable(t, exec)
        };
    }
    
    /**
     * @see NodeModel#loadModelContent(int, ModelContentRO)
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
     * @see NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec) 
    throws IOException, CanceledExecutionException {
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(
            final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
    }

    /**
     * @see NodeModel#saveInternals(File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

}
