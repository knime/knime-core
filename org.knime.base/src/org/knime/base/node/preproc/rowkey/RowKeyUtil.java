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
     * the row key
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
     * @return the {@link BufferedDataTable} with the replaced row key and
     * the optional appended new column with the old row keys.
     * @throws Exception if the cancel button was pressed or the input data
     * isn't valid.
     */
    public BufferedDataTable changeRowKey(final BufferedDataTable inData,
            final ExecutionContext exec, final String selRowKeyColName,
            final boolean appendColumn, final DataColumnSpec newColSpec,
            final boolean ensureUniqueness, final boolean replaceMissingVals,
            final boolean removeRowKeyCol)
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
        final int newRowKeyColIdx = inSpec.findColumnIndex(selRowKeyColName);
        if (newRowKeyColIdx < 0) {
            throw new InvalidSettingsException("Column name not found.");
        }
        final int totalNoOfRows = inData.getRowCount();
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
            if (vals.containsKey(key)) {
                if (!keyCell.isMissing()) {
                    m_duplicatesCounter++;
                }
                if (ensureUniqueness) {
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
                } else {
                    if (keyCell.isMissing()) {
                        throw new InvalidSettingsException(
                                "Error in row " + rowCounter + ": "
                                + "Multiple missing values found. Check the '"
                                + RowKeyNodeDialog.ENSURE_UNIQUENESS_LABEL
                                + "' option to handle multiple occurrences.");
                    }
                    throw new InvalidSettingsException(
                            "Error in row " + rowCounter + ": "
                            + "Duplicate value: " + key
                            + " already exists. Check the '"
                            + RowKeyNodeDialog.ENSURE_UNIQUENESS_LABEL
                            + "' option to handle duplicates.");
                }
            }
            //put the current key which is new into the values map
            final MutableInteger index = new MutableInteger(0);
            vals.put(key, index);
            final RowKey newKeyVal = new RowKey(key);
            final DefaultRow newRow = new DefaultRow(newKeyVal, cells);
            newContainer.addRowToTable(newRow);
            exec.checkCanceled();
            if (rowCounter % checkPoint == 0) {
                exec.setProgress(progressPerRow * rowCounter,
                        rowCounter + " rows of " + totalNoOfRows
                        + " rows processed.");
            }
        }
        exec.setProgress(1.0, "Finished");
        newContainer.close();
        LOGGER.debug("Exiting changeRowKey(inData, exec, selRowKeyColName, "
                + "newColName) of class RowKeyUtil.");
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
