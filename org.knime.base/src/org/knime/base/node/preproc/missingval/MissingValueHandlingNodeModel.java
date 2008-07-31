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
 */
package org.knime.base.node.preproc.missingval;

import java.io.File;
import java.io.IOException;

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
 * NodeModel for missing value node. 
 * @author wiswedel, University of Konstanz
 */
public class MissingValueHandlingNodeModel extends NodeModel {
    private ColSetting[] m_colSettings;

    /** One input, one output. */
    public MissingValueHandlingNodeModel() {
        super(1, 1);
        m_colSettings = new ColSetting[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        ColSetting.saveMetaColSettings(m_colSettings, settings);
        ColSetting.saveIndividualsColSettings(m_colSettings, settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ColSetting.loadMetaColSettings(settings);
        ColSetting.loadIndividualColSettings(settings);
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        StringBuffer warningMessageBuffer = new StringBuffer();
        BufferedDataTable out = MissingValueHandlingTable.
            createMissingValueHandlingTable(
                    inData[0], m_colSettings, exec, warningMessageBuffer);
        if (warningMessageBuffer.length() > 0) {
            setWarningMessage(warningMessageBuffer.toString());
        }
        return new BufferedDataTable[]{out};
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
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
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
