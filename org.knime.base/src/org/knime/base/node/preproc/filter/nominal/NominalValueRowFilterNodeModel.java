/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.filter.nominal;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of PossibleValueRowFilter. For a nominal
 * column one or more possible values can be selected. If the value in the
 * selected column of a row matches the included possible values the row is
 * added to the included rows at first out port, else to the excluded at second
 * outport.
 *
 *
 * @author KNIME GmbH
 */
public class NominalValueRowFilterNodeModel extends NodeModel {

    private String m_selectedColumn;

    private int m_selectedColIdx;

    private final Set<String> m_selectedAttr = new HashSet<String>();

    /**
     * One inport (data to be filtered) two out ports (included and excluded).
     */
    protected NominalValueRowFilterNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // include data container
        DataContainer positive =
                exec.createDataContainer(inData[0].getDataTableSpec());
        double currentRow = 0;
        for (DataRow row : inData[0]) {
            // if row matches to included...
            if (matches(row)) {
                positive.addRowToTable(row);
            }
            exec.setProgress(currentRow / inData[0].getRowCount(),
                    "filtering row # " + currentRow);
            currentRow++;
            exec.checkCanceled();
        }
        positive.close();
        BufferedDataTable positiveTable =
                exec.createBufferedDataTable(positive.getTable(), exec);
        if (positiveTable.getRowCount() <= 0) {
            setWarningMessage("No rows matched!");
        }
        return new BufferedDataTable[]{positiveTable};
    }

    /*
     * Check if the value in the selected column is in the selected possible
     * values.
     */
    private boolean matches(final DataRow row) {
        DataCell dc = row.getCell(m_selectedColIdx);
        return m_selectedAttr.contains(dc.toString());
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
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check if possible values are available
        int nrValidCols = 0;
        for (DataColumnSpec colSpec : inSpecs[0]) {
            if (colSpec.getType().isCompatible(NominalValue.class)
                    && colSpec.getDomain().hasValues()) {
                nrValidCols++;
            }
        }
        // are there some valid columns (nominal and with possible values)
        if (nrValidCols == 0) {
            throw new InvalidSettingsException(
                    "No nominal columns with possible values found! "
                            + "Execute predecessor or check input table.");
        }
        // all values excluded?
        if (m_selectedColumn != null && m_selectedAttr.size() == 0) {
            setWarningMessage("All values are excluded!"
                    + " Input data will be mirrored at out-port 1 (excluded)");
        }
        if (m_selectedColumn != null && m_selectedColumn.length() > 0) {
            m_selectedColIdx = inSpecs[0].findColumnIndex(m_selectedColumn);
            // selected attribute not found in possible values
            if (m_selectedColIdx < 0) {
                throw new InvalidSettingsException("Column " + m_selectedColumn
                        + " not found in in spec!");
            }
            // all values included?
            boolean validAttrVal = false;
            if (inSpecs[0].getColumnSpec(m_selectedColIdx).getDomain()
                    .hasValues()) {
                if (inSpecs[0].getColumnSpec(m_selectedColIdx).getDomain()
                        .getValues().size() == m_selectedAttr.size()) {
                    setWarningMessage("All values are included! Input will be "
                            + "mirrored at out-port 0 (included)");
                }
                // if attribute value isn't found in domain also throw exception
                for (DataCell dc : inSpecs[0].getColumnSpec(m_selectedColIdx)
                        .getDomain().getValues()) {
                    if (m_selectedAttr.contains(dc.toString())) {
                        validAttrVal = true;
                        break;
                    }
                }
            }
            if (!validAttrVal && m_selectedAttr.size() > 0) {
                throw new InvalidSettingsException("Selected attribute value "
                        + m_selectedAttr + " not found!");
            }

            // return original spec,
            // only the rows are affected
        }
        return new DataTableSpec[]{inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(NominalValueRowFilterNodeDialog.CFG_SELECTED_COL,
                m_selectedColumn);
        settings.addStringArray(
                NominalValueRowFilterNodeDialog.CFG_SELECTED_ATTR,
                m_selectedAttr.toArray(new String[m_selectedAttr.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedColumn = settings.getString(
                        NominalValueRowFilterNodeDialog.CFG_SELECTED_COL);
        String[] selected = settings.getStringArray(
                NominalValueRowFilterNodeDialog.CFG_SELECTED_ATTR);
        m_selectedAttr.clear();
        for (String s : selected) {
            m_selectedAttr.add(s);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_selectedColumn = settings.getString(
                NominalValueRowFilterNodeDialog.CFG_SELECTED_COL);
        String[] selected = settings.getStringArray(
                NominalValueRowFilterNodeDialog.CFG_SELECTED_ATTR);
        m_selectedAttr.clear();
        for (String s : selected) {
            m_selectedAttr.add(s);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

}
