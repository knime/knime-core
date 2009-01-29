/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   12.01.2006 (gabriel): created
 */
package org.knime.base.node.preproc.nominal;

import static org.knime.core.node.util.ColumnFilterPanel.INCLUDED_COLUMNS;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.nominal.NominalTable;
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
 * Collects and sets all nominal values for a number of selected columns during
 * execute.
 * 
 * @see NominalTable
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class NominalValueModel extends NodeModel {

    /** Selected columns to collect nominal values for. */
    private String[] m_columns = new String[0];

    /**
     * Creates a new model with one in- and output.
     * 
     * @param ins Number of inputs.
     * @param outs Number of inputs.
     */
    NominalValueModel(final int ins, final int outs) {
        super(ins, outs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec nSpec = NominalTable.computeValues(inData[0], exec,
                m_columns);
        return new BufferedDataTable[]{exec.createSpecReplacerTable(inData[0],
                nSpec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_columns != null) {
            for (int i = 0; i < m_columns.length; i++) {
                if (m_columns[i] == null) {
                    throw new InvalidSettingsException(
                            "Column can not be null.");
                }
                if (!inSpecs[0].containsName(m_columns[i])) {
                    throw new InvalidSettingsException("Column " + m_columns[i]
                            + " not in spec.");
                }
            }
        }
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addStringArray(INCLUDED_COLUMNS, m_columns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getStringArray(INCLUDED_COLUMNS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columns = settings.getStringArray(INCLUDED_COLUMNS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to save here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }
}
