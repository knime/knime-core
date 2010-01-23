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
 * -------------------------------------------------------------------
 *
 * History
 *    03.11.2006 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.rowkey;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.MutableInteger;

import org.knime.base.data.append.column.AppendedColumnTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides methods to append a new row with the row key values or
 * to replace the row key by the values of another column.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class RowKeyUtil {
    private static final NodeLogger LOGGER = NodeLogger
        .getLogger(RowKeyUtil.class);

    /**This value is used instead of a missing value as new row key if the
     * replaceMissingVals variable is set to <code>true</code>.*/
    protected static final String MISSING_VALUE_REPLACEMENT = "?";

    private int m_duplicatesCounter = 0;

    private int m_missingValueCounter = 0;

    private Map<RowKey, Set<RowKey>> m_hiliteMapping = null;


    /**
     * Creates the {@link ColumnRearranger} that appends a new column with the
     * values of the row id to a data table.
     *
     * @param inSpec the <code>DataTableSpec</code> of table were the column
     * should be appended
     * @param newColName the name of the added column
     * @param type the <code>DataType</code> of the new column
     * @return the {@link ColumnRearranger} to use
     */
    public static ColumnRearranger createColumnRearranger(
            final DataTableSpec inSpec, final String newColName,
            final DataType type) {
        final ColumnRearranger c = new ColumnRearranger(inSpec);
        // column specification of the appended column
        final DataColumnSpecCreator colSpecCreater =
            new DataColumnSpecCreator(newColName, type);
        final DataColumnSpec newColSpec = colSpecCreater.createSpec();
        // utility object that performs the calculation
        final CellFactory factory = new SingleCellFactory(newColSpec) {
            @Override
            public DataCell getCell(final DataRow row) {
                return new StringCell(row.getKey().getString());
            }
        };
        c.append(factory);
        return c;
    }


    /**
     * <p>Replaces the row key by the values of the column with the given name
     * and appends a new column with the old key values if the
     * <code>newColName</code> variable is a non empty <code>String</code>.</p>
     * <p>
     * Call the {@link RowKeyUtil#getDuplicatesCounter()} and
     * {@link RowKeyUtil#getMissingValueCounter()}
     * methods to get information about the replaced duplicates and missing
     * values after this method is completed.
     * </p>
     * @param inData The {@link BufferedDataTable} with the input data
     * @param exec the {@link ExecutionContext} to check for cancel and to
     * provide status messages
     * @param selRowKeyColName the name of the column which should replace
     * the row key or <code>null</code> if a new one should be created
     * @param appendColumn <code>true</code> if a new column should be created
     * @param newColSpec the {@link DataColumnSpec} of the new column or
     * <code>null</code>  if no column should be created at all
     * @param ensureUniqueness if set to <code>true</code> the method ensures
     * the uniqueness of the row key even if the values of the selected row
     * aren't unique
     * @param replaceMissingVals if set to <code>true</code> the method
     * replaces missing values with ?
     * @param removeRowKeyCol removes the selected row key column if set
     * to <code>true</code>
     * @param hiliteMap <code>true</code> if a map should be maintained that
     * maps the new row id to the old row id
     * @return the {@link BufferedDataTable} with the replaced row key and
     * the optional appended new column with the old row keys.
     * @throws Exception if the cancel button was pressed or the input data
     * isn't valid.
     */
    public BufferedDataTable changeRowKey(final BufferedDataTable inData,
            final ExecutionContext exec, final String selRowKeyColName,
            final boolean appendColumn, final DataColumnSpec newColSpec,
            final boolean ensureUniqueness, final boolean replaceMissingVals,
            final boolean removeRowKeyCol, final boolean hiliteMap)
    throws Exception {
        LOGGER.debug("Entering changeRowKey(inData, exec, selRowKeyColName, "
                + "newColName) of class RowKeyUtil.");
        final DataTableSpec inSpec = inData.getDataTableSpec();
        DataTableSpec outSpec = inSpec;
        if (removeRowKeyCol) {
            outSpec = createTableSpec(outSpec, selRowKeyColName);
        }
        if (appendColumn) {
            if (newColSpec == null) {
                throw new NullPointerException(
                        "NewColumnSpec must not be null");
            }
            outSpec = AppendedColumnTable.getTableSpec(outSpec, newColSpec);
        }
        final BufferedDataContainer newContainer =
            exec.createDataContainer(outSpec, false);
        final int noOfCols = outSpec.getNumColumns();
        final int newRowKeyColIdx;
        if (selRowKeyColName != null) {
            newRowKeyColIdx = inSpec.findColumnIndex(selRowKeyColName);
            if (newRowKeyColIdx < 0) {
                throw new InvalidSettingsException("Column name not found.");
            }
        } else {
            newRowKeyColIdx = -1;
        }
        final int totalNoOfRows = inData.getRowCount();
        if (hiliteMap) {
            m_hiliteMapping = new HashMap<RowKey, Set<RowKey>>(totalNoOfRows);
        }
        final Map<String, MutableInteger> vals =
            new HashMap<String, MutableInteger>(totalNoOfRows);
        final double progressPerRow = 1.0 / totalNoOfRows;
        //update the progress monitor every percent
        final int checkPoint = Math.max((totalNoOfRows  / 1000), 1);
        int rowCounter = 0;
        exec.setProgress(0.0, "Processing data...");
        m_missingValueCounter = 0;
        m_duplicatesCounter = 0;
        for (final DataRow row : inData) {
            rowCounter++;
            final DataCell[] cells = new DataCell[noOfCols];
            int newCellCounter = 0;
            for (int i = 0, length = inSpec.getNumColumns(); i < length; i++) {
                if (removeRowKeyCol && i == newRowKeyColIdx) {
                    continue;
                }
                cells[newCellCounter++] = row.getCell(i);
            }
            if (appendColumn) {
                cells[noOfCols - 1] = new StringCell(row.getKey().getString());
            }
            final RowKey newKeyVal;
            if (newRowKeyColIdx >= 0) {
                final DataCell keyCell = row.getCell(newRowKeyColIdx);
                String key = null;
                if (keyCell.isMissing()) {
                    if (replaceMissingVals) {
                        key = MISSING_VALUE_REPLACEMENT;
                        m_missingValueCounter++;
                    } else {
                        throw new InvalidSettingsException(
                                "Missing value found in row " + rowCounter);
                    }
                } else {
                    key = keyCell.toString();
                }
                if (ensureUniqueness) {
                    if (vals.containsKey(key)) {
                        if (!keyCell.isMissing()) {
                            m_duplicatesCounter++;
                        }
                        StringBuilder uniqueKey = new StringBuilder(key);
                        final MutableInteger index = vals.get(uniqueKey.toString());
                        while (vals.containsKey(uniqueKey.toString())) {
                            index.inc();
                            uniqueKey = new StringBuilder(key);
                            uniqueKey.append("(");
                            uniqueKey.append(index.toString());
                            uniqueKey.append(")");
                        }
                        key = uniqueKey.toString();
                    }
                    //put the current key which is new into the values map
                    final MutableInteger index = new MutableInteger(0);
                    vals.put(key, index);
                } 
                newKeyVal = new RowKey(key);
            } else {
                newKeyVal = RowKey.createRowKey(rowCounter);
            }

            final DefaultRow newRow = new DefaultRow(newKeyVal, cells);
            newContainer.addRowToTable(newRow);
            if (hiliteMap) {
                final Set<RowKey> oldKeys = new HashSet<RowKey>(1);
                oldKeys.add(row.getKey());
                m_hiliteMapping.put(newKeyVal, oldKeys);
            }
            exec.checkCanceled();
            if (rowCounter % checkPoint == 0) {
                exec.setProgress(progressPerRow * rowCounter,
                        rowCounter + " rows of " + totalNoOfRows
                        + " rows processed.");
            }
        }
        exec.setProgress(1.0, "Finished");
        newContainer.close();
        return newContainer.getTable();
    }

    /**
     * @return the number of missing values which were found while changing
     * the row key
     */
    public int getMissingValueCounter() {
        return m_missingValueCounter;
    }

    /**
     * @return the number of duplicates which were found while changing
     * the row key
     */
    public int getDuplicatesCounter() {
        return m_duplicatesCounter;
    }

    /**
     * The hilite translation <code>Map</code> or <code>null</code> if
     * the enableHilte flag in the constructor was set to <code>false</code>.
     * The key of the <code>Map</code> is the row key of the new row and
     * the corresponding value is the <code>Set</code> with the corresponding
     * old row key.
     * @return the hilite translation <code>Map</code> or <code>null</code> if
     * the enableHilte flag in the constructor was set to <code>false</code>.
     */
    public Map<RowKey, Set<RowKey>> getHiliteMapping() {
        return m_hiliteMapping;
    }

    /**
     * @param spec the original {@link DataTableSpec}
     * @param columnNames2Drop the names of the column to remove from the
     * original table specification
     * @return the original table specification without the column
     * specifications of the given names
     */
    public static DataTableSpec createTableSpec(final DataTableSpec spec,
            final String... columnNames2Drop) {
        if (spec == null) {
            return null;
        }
        if (columnNames2Drop == null || columnNames2Drop.length < 1) {
            return spec;
        }
        final int numColumns = spec.getNumColumns();
        if (columnNames2Drop.length > numColumns) {
            throw new IllegalArgumentException("Number of skipped columns is "
                    + "greater than total number of columns.");
        }
        final Set<String> names2Drop =
            new HashSet<String>(Arrays.asList(columnNames2Drop));
        final Collection<DataColumnSpec> newColSpecs =
            new ArrayList<DataColumnSpec>(numColumns - columnNames2Drop.length);
        for (final DataColumnSpec columnSpec : spec) {
            if (names2Drop.contains(columnSpec.getName())) {
                continue;
            }
            newColSpecs.add(columnSpec);
        }
        return new DataTableSpec(newColSpecs.toArray(new DataColumnSpec[0]));
    }
}
