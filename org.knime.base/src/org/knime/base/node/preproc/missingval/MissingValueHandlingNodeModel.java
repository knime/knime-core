/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
package org.knime.base.node.preproc.missingval;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class MissingValueHandlingNodeModel extends NodeModel {
    private ColSetting[] m_colSettings;

    public MissingValueHandlingNodeModel() {
        super(1, 1);
        m_colSettings = new ColSetting[0];
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        ColSetting.saveMetaColSettings(m_colSettings, settings);
        ColSetting.saveIndividualsColSettings(m_colSettings, settings);
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ColSetting.loadMetaColSettings(settings);
        ColSetting.loadIndividualColSettings(settings);
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ColSetting[] def = ColSetting.loadMetaColSettings(settings);
        ColSetting[] ind = ColSetting.loadIndividualColSettings(settings);
        m_colSettings = new ColSetting[def.length + ind.length];
        System.arraycopy(def, 0, m_colSettings, 0, def.length);
        System.arraycopy(ind, 0, m_colSettings, def.length, ind.length);
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[], ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTable out = MissingValueHandlingTable
                .createMissingValueHandlingTable(inData[0], m_colSettings, exec);
        return new BufferedDataTable[]{exec.createBufferedDataTable(out, exec)};
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
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * @see NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec in = inSpecs[0];
        // column domain may changed when using fixed replacement
        DataTableSpec out = MissingValueHandlingTable.createTableSpec(in,
                m_colSettings);
        return new DataTableSpec[]{out};
    }
}
