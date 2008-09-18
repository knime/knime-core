/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   06.05.2008 (gabriel): created
 */
package org.knime.base.node.preproc.filter.rowref;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * The Reference Row Filter node allow the filtering of row IDs based
 * on a second reference table. Two modes are possible, either the corresponding
 * row IDs of the first table are included or excluded in the resulting
 * output table.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class RowFilterRefNodeModel extends NodeModel {

    /** Settings model for include/exclude option. */
    private final SettingsModelString m_inexcludeRows =
        RowFilterRefNodeDialogPane.createInExcludeModel();

    /** Settings model for the reference column of the data table to filter. */
    private final SettingsModelColumnName m_dataTableCol =
        RowFilterRefNodeDialogPane.createDataTableColModel();

    /** Settings model for the reference column of the reference table. */
    private final SettingsModelColumnName m_referenceTableCol =
        RowFilterRefNodeDialogPane.createReferenceTableColModel();

    /**
     * Creates a new reference row filter node model with two inputs and
     * one filtered output.
     */
    public RowFilterRefNodeModel() {
        super(2, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) {
        //check if the user uses the rowkey with a column
        if (m_dataTableCol.useRowID() != m_referenceTableCol.useRowID()) {
            if (m_dataTableCol.useRowID()) {
                setWarningMessage("Using string representation of column "
                            + m_referenceTableCol.getColumnName()
                            + " for RowKey comparison");
            } else {
                setWarningMessage("Using string representation of column "
                            + m_dataTableCol.getColumnName()
                            + " for RowKey comparison");
            }
        } else if (!m_dataTableCol.useRowID()) {
            final DataColumnSpec refColSpec = inSpecs[0].getColumnSpec(
                    m_referenceTableCol.getColumnName());
            final DataColumnSpec datColSpec = inSpecs[1].getColumnSpec(
                    m_dataTableCol.getColumnName());
            if (!refColSpec.getType().equals(datColSpec.getType())) {
                setWarningMessage("Different column types using string "
                        + "representation for comparison");
            }
        }
        return new DataTableSpec[]{inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final boolean exclude = m_inexcludeRows.getStringValue().equals(
                RowFilterRefNodeDialogPane.EXCLUDE);
        final BufferedDataTable dataTable = inData[0];
        final String dataColName = m_dataTableCol.getColumnName();
        final boolean useDataRowKey = m_dataTableCol.useRowID();
        final DataTableSpec dataTableSpec = dataTable.getSpec();
        final int dataColIdx = dataTableSpec.findColumnIndex(dataColName);
        if (!useDataRowKey && dataColIdx < 0) {
            throw new InvalidSettingsException("Column " + dataColName
                    + " not found in table to be filtered");
        }
        final BufferedDataTable refTable = inData[1];
        final String refColName = m_referenceTableCol.getColumnName();
        final boolean useRefRowKey = m_referenceTableCol.useRowID();
        final DataTableSpec refTableSpec = refTable.getSpec();
        final int refColIdx = refTableSpec.findColumnIndex(refColName);
        if (!useRefRowKey && refColIdx < 0) {
            throw new InvalidSettingsException("Column " + refColName
                    + " not found in reference table");
        }
        //check if we have to use String for comparison
        boolean filterByString = false;
        if (useDataRowKey != useRefRowKey) {
            filterByString = true;
        } else if (!useDataRowKey) {
            final DataColumnSpec refColSpec = refTableSpec.getColumnSpec(
                    refColName);
            final DataColumnSpec datColSpec = dataTableSpec.getColumnSpec(
                    dataColName);
            if (!refColSpec.getType().equals(datColSpec.getType())) {
                filterByString = true;
            }
        }

        //create the set to filter by
        final Set<Object> keySet = new HashSet<Object>();
        if (filterByString) {
            if (useRefRowKey) {
                for (final DataRow row : refTable) {
                    keySet.add(row.getKey().getString());
                }
            } else {
                for (final DataRow row : refTable) {
                    keySet.add(row.getCell(refColIdx).toString());
                }
            }
        } else {
            if (useRefRowKey) {
                for (final DataRow row : refTable) {
                    keySet.add(row.getKey());
                }
            } else {
                for (final DataRow row : refTable) {
                    keySet.add(row.getCell(refColIdx));
                }
            }
        }
        //Filter the data table
        final BufferedDataContainer buf =
            exec.createDataContainer(dataTableSpec);
        double rowCnt = 1;
        for (final DataRow row : dataTable) {
            exec.checkCanceled();
            exec.setProgress(
                    rowCnt++ / dataTable.getRowCount(), "Filtering...");
            //get the right value to check for...
            final Object val2Compare;
            if (filterByString) {
                if (useDataRowKey) {
                    val2Compare = row.getKey().getString();
                } else {
                    val2Compare = row.getCell(dataColIdx).toString();
                }
            } else {
                if (useDataRowKey) {
                    val2Compare = row.getKey();
                } else {
                    val2Compare = row.getCell(dataColIdx);
                }
            }
            //...include/exclude matching rows by checking the val2Compare
            if (exclude) {
                if (!keySet.contains(val2Compare)) {
                    buf.addRowToTable(row);
                }
            } else {
                if (keySet.contains(val2Compare)) {
                    buf.addRowToTable(row);
                }
            }
        }
        buf.close();
        return new BufferedDataTable[]{buf.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inexcludeRows.loadSettingsFrom(settings);
        try {
            m_dataTableCol.loadSettingsFrom(settings);
            m_referenceTableCol.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            //the previous version had no column options use the rowkey for both
            m_dataTableCol.setSelection(null, true);
            m_referenceTableCol.setSelection(null, true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //nothing to reset
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) {
        //nothing to save
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inexcludeRows.saveSettingsTo(settings);
        m_dataTableCol.saveSettingsTo(settings);
        m_referenceTableCol.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_inexcludeRows.validateSettings(settings);
        m_dataTableCol.validateSettings(settings);
        m_referenceTableCol.validateSettings(settings);
    }
}
