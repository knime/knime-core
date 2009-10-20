/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {
        if (!m_dataTableCol.useRowID()) {
            final DataColumnSpec dataColSpec = inSpecs[0].getColumnSpec(
                    m_dataTableCol.getColumnName());
            if (dataColSpec == null) {
                throw new InvalidSettingsException("Invalid data table column");
            }
        }
        if (!m_referenceTableCol.useRowID()) {
            final DataColumnSpec refColSpec = inSpecs[1].getColumnSpec(
                    m_referenceTableCol.getColumnName());
            if (refColSpec == null) {
                throw new InvalidSettingsException(
                        "Invalid reference table column");
            }
        }
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
            final DataColumnSpec dataColSpec = inSpecs[0].getColumnSpec(
                    m_dataTableCol.getColumnName());
            final DataColumnSpec refColSpec = inSpecs[1].getColumnSpec(
                    m_referenceTableCol.getColumnName());
            if (!refColSpec.getType().equals(dataColSpec.getType())) {
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
            //Introduced in KNIME 2.0
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
        /*
         * No validation of the dataTableCol and referenceTableCol settings
         * since they were introduced in KNIME 2.0
         * m_dataTableCol.validateSettings(settings);
         * m_referenceTableCol.validateSettings(settings);
         * */
    }
}
