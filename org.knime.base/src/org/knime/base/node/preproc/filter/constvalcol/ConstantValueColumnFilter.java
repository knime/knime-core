/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   5 Apr 2018 (Marc Bux): created
 */
package org.knime.base.node.preproc.filter.constvalcol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 *
 * @author Marc Bux, KNIME AG, Zurich, Switzerland
 */
public class ConstantValueColumnFilter {

    /**
     * The name of the settings tag which holds the names of the columns the user has selected in the dialog as
     * to-be-filtered
     */
    public static final String SELECTED_COLS = "filter-list";

    /**
     * A method to generate a new configuration to store the settings.
     *
     * @return the new configuration
     */
    public static final DataColumnSpecFilterConfiguration createDCSFilterConfiguration() {
        return new DataColumnSpecFilterConfiguration(SELECTED_COLS);
    }

    /**
     * A method that, from a selection of columns, determines the columns that contain only the same (duplicate /
     * constant) value over and over.
     *
     * @param inputTable the input table that is to be investigated for columns with constant values
     * @param colNamesToFilter the names of columns that potentially contain constant values only
     * @return the names of columns that provably contain constant values only
     */
    public static final String[] determineConstantValueColumns(final BufferedDataTable inputTable,
        final String[] colNamesToFilter) {
        // if (inputTable.size() < 1) {
        //     return new String[0];
        // }

        // if the inputTable only has a single row (or no rows at all), each column only contains constant values
        if (inputTable.size() < 2) {
            return colNamesToFilter;
        }

        Set<String> colNamesToFilterSet = new HashSet<>(Arrays.asList(colNamesToFilter));
        String[] colNames = inputTable.getDataTableSpec().getColumnNames();
        RowIterator rowIt = inputTable.iterator();
        DataRow firstRow = rowIt.next();

        // a map that maps the indices of columns that potentially contain only duplicate values to their last observed value
        Map<Integer, DataCell> indicesToCells = new HashMap<>();
        for (int i = 0; i < colNames.length; i++) {
            if (colNamesToFilterSet.contains(colNames[i])) {
                indicesToCells.put(i, firstRow.getCell(i));
            }
        }

        // across all columns, check if there are two (vertically) successive cells with different values
        while (rowIt.hasNext()) {
            DataRow currentRow = rowIt.next();
            for (Iterator<Entry<Integer, DataCell>> entryIt = indicesToCells.entrySet().iterator(); entryIt
                .hasNext();) {
                Entry<Integer, DataCell> e = entryIt.next();
                DataCell currentCell = currentRow.getCell(e.getKey());
                DataCell lastCell = e.getValue();
                // if successive cells with different values are found, this column is not constant and should be removed from the indicesToCells map
                if (currentCell != null && lastCell != null && !currentCell.equals(lastCell)) {
                    entryIt.remove();
                }
            }
        }

        // obtain the names of to-be-filtered columns from the indicesToCells map
        String[] colNamesToRemove = indicesToCells.keySet().stream().map(i -> colNames[i]).toArray(String[]::new);

        return colNamesToRemove;
    }
}
