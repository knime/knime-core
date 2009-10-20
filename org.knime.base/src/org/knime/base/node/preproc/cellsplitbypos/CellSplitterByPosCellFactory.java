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
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 19, 2007 (ohl): created
 */
package org.knime.base.node.preproc.cellsplitbypos;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;

/**
 * Creates the data cells for the new columns of the cell splitter.
 *
 * @author ohl, University of Konstanz
 */
class CellSplitterByPosCellFactory implements CellFactory {

    private final DataTableSpec m_inSpec;

    private final int m_colIdx;

    private final int[] m_splitPoints;

    private final String[] m_colNames;

    /**
     * Le Constucteur.
     *
     * @param inSpec the spec from the underlying input table
     * @param colName the name of the column to split
     * @param splitPoints the indices where the input column is supposed to be
     *            split
     * @param colNames the new column names of the created columns. Must be
     *            valid (i.e. unique and not existing in input table)
     * @throws InvalidSettingsException if the specified col name is not in the
     *             input spec, or if the number of colNames is different than
     *             required
     */
    public CellSplitterByPosCellFactory(final DataTableSpec inSpec,
            final String colName, final int[] splitPoints,
            final String[] colNames) throws InvalidSettingsException {

        m_inSpec = inSpec;

        m_colIdx = m_inSpec.findColumnIndex(colName);
        if (m_colIdx < 0) {
            throw new InvalidSettingsException("Specified column must be"
                    + " contained in input table");
        }

        m_splitPoints = splitPoints;
        m_colNames = colNames;

        if (m_colNames.length != m_splitPoints.length + 1) {
            throw new InvalidSettingsException(
                    "Number of new column names is different "
                            + "than specified splits (no. of new columns: "
                            + (m_splitPoints.length + 1) + ")");
        }
        int prev = 0;
        for (int p : m_splitPoints) {
            if (p <= prev) {
                throw new InvalidSettingsException("Split points must be "
                        + "strictly increasing numbers.");
            }
            prev = p;
        }

    }

    /**
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {

        DataCell[] result = new DataCell[m_splitPoints.length + 1];

        // preset the result to empty strings
        StringCell e = new StringCell("");
        for (int r = 0; r < result.length; r++) {
            result[r] = e;
        }

        if (row.getCell(m_colIdx).isMissing()) {
            // split string is missing - all result cells will be empty
            return result;
        }

        String splitString =
                ((StringValue)row.getCell(m_colIdx)).getStringValue();

        int lastSplit = 0;
        int s = 0;
        while (s < m_splitPoints.length) {
            int endIdx = m_splitPoints[s];
            if (endIdx > splitString.length()) {
                // string is shorter than the rest of the splits - done.
                break;
            }
            result[s] =
                    new StringCell(splitString.substring(lastSplit, endIdx));
            lastSplit = endIdx;
            s++;
        }

        // put the rest of the string in the last result cell
        if (lastSplit < splitString.length()) {
            result[s] =
                    new StringCell(splitString.substring(lastSplit));
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {

        DataColumnSpec[] result = new DataColumnSpec[m_colNames.length];

        for (int c = 0; c < result.length; c++) {
            DataColumnSpecCreator dcsc =
                    new DataColumnSpecCreator(m_colNames[c], StringCell.TYPE);
            result[c] = dcsc.createSpec();
        }
        return result;

    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress((double)curRowNr / (double)rowCount,
                "processing row #" + curRowNr + " of " + rowCount + " ("
                        + lastKey.getString() + ")");
    }

}
