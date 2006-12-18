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
 * History
 *    03.11.2006 (Tobias Koetter): created
 */
package org.knime.base.node.preproc.rowkey;

import java.util.HashMap;
import java.util.Map;

import org.knime.base.data.append.column.AppendedCellFactory;
import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.MutableInteger;

/**
 * Provides methods to append a new row with the row key values or 
 * to replace the row key by the values of another column.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class RowKeyUtil implements AppendedCellFactory {
    private static final NodeLogger LOGGER = NodeLogger
        .getLogger(RowKeyUtil.class);
    
    /**This value is used instead of a missing value as new row key if the
     * replaceMissingVals variable is set to <code>true</code>.*/
    protected static final String MISSING_VALUE_REPLACEMENT = "?";
    
    private int m_duplicatesCounter = 0;
    
    private int m_missingValueCounter = 0;
    
    /**
     * Creates the <code>DataColumnSpec</code> of the row which contains
     * the row key as value. If the name already exists it appends (x)
     * to the name with x starting by 1 incrementing as long as a column
     * with the same name exists in the given table specification.
     * @param inSpec the <code>DataTableSpec</code> of the input data
     * @param colName the name of the new column
     * @param type the type of the new column
     * @return <code>DataColumnSpec[]</code> with the column specifications
     * of the result columns
     */
    public static DataColumnSpec[] getResultColSpecs(
            final DataTableSpec inSpec, final String colName, 
            final DataType type) {
        LOGGER.debug("Entering getResultColSpecs(inSpec) of class RowKeyUtil.");
        if (inSpec == null || colName == null || type == null) {
            throw new IllegalArgumentException(
                    "All arguments shouldn't be null.");
        }
        String newColName = colName;
        //check if the column name already exists
        if (inSpec.containsName(newColName)) {
            //if that's the case append (x) where x starts by 1 and gets 
            //incremented until the name doesn't exists
            int colNameAddOn = 1;
            String nameAddOn = colName;
            do {
                nameAddOn = colName + "(" + colNameAddOn + ")";
                LOGGER.debug("Column already exists changed to " + nameAddOn);
                colNameAddOn++;
            } while(inSpec.containsName(nameAddOn));
            newColName = nameAddOn;
        }
        final DataColumnSpecCreator colSpecCreater = 
            new DataColumnSpecCreator(newColName, type);
        final DataColumnSpec colSpec = colSpecCreater.createSpec();
        LOGGER.debug(
                "Exiting getResultColSpecs(inSpec, colName, type) " 
                + "of class RowKeyUtil.");
        return new DataColumnSpec[] {colSpec};
    }

    /**
     * @see org.knime.base.data.append.column.
     * AppendedCellFactory#getAppendedCell(org.knime.core.data.DataRow)
     */
    public DataCell[] getAppendedCell(final DataRow row) {
        return new DataCell[] {row.getKey().getId()};
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
     * @return the {@link BufferedDataTable} with the replaced row key and
     * the optional appended new column with the old row keys.
     * @throws Exception if the cancel button was pressed or the input data
     * isn't valid.
     */
    public BufferedDataTable changeRowKey(final BufferedDataTable inData,
            final ExecutionContext exec, final String selRowKeyColName, 
            final boolean appendColumn, final DataColumnSpec newColSpec,
            final boolean ensureUniqueness, final boolean replaceMissingVals)
    throws Exception {
        LOGGER.debug("Entering changeRowKey(inData, exec, selRowKeyColName, " 
                + "newColName) of class RowKeyUtil.");
        final DataTableSpec inSpec = inData.getDataTableSpec();
        DataTableSpec outSpec = inSpec;
        if (appendColumn) {
            outSpec = AppendedColumnTable.getTableSpec(inSpec, newColSpec);
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
        final int checkPoint = Math.max((totalNoOfRows  / 100), 1);
        int rowCounter = 0;
        exec.setProgress(0.0, "Processing data...");
        m_missingValueCounter = 0;
        m_duplicatesCounter = 0;
        for (DataRow row : inData) {
            rowCounter++;
            final DataCell[] cells = new DataCell[noOfCols];
            for (int i = 0, length = inSpec.getNumColumns(); i < length; i++) {
                cells[i] = row.getCell(i);
            }
            if (appendColumn) {
                cells[noOfCols - 1] = row.getKey().getId();
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
                    } else {
                        throw new InvalidSettingsException(
                                "Error in row " + rowCounter + ": "
                                + "Duplicate value: " + key 
                                + " already exists. Check the '" 
                                + RowKeyNodeDialog.ENSURE_UNIQUENESS_LABEL 
                                + "' option to handle duplicates.");
                    }
                }
            }
            //put the current key which is new into the values map
            final MutableInteger index = new MutableInteger(0);
            vals.put(key, index);
            final DataCell newKeyVal = new StringCell(key);
            final DefaultRow newRow = new DefaultRow(newKeyVal, cells);
            newContainer.addRowToTable(newRow);
            if (rowCounter % checkPoint == 0) {
                exec.checkCanceled();
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
}
