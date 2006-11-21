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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.preproc.colappender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.colappender.ColAppenderSettings.DuplicateHandling;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This node model is used for appending the columns of a table to another
 * table. The user can choose two columns from the two tables and the columns
 * from the second table are appended to the mathing rows of the first table.
 * The selected column from the first table may contain the same value in
 * multiple cell whereas the values in the column from the second table must be
 * unique. The user can also choose, if an "inner join" should be performed,
 * i.e. only rows from the left table for which there is a matching row in the
 * right table are output. Otherwise, rows without a match get filled up with
 * missing values in the appended columns.<br />
 * For joining efficiently, the two tables are sorted on the selected columns.
 * This means that also the output table will be sorted.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ColAppenderNodeModel extends NodeModel {
    private final ColAppenderSettings m_settings = new ColAppenderSettings();
    
    /**
     * Creates a new model for the column appender node.
     */
    public ColAppenderNodeModel() {
        super(2, 1);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createJoinedSpec(inSpecs)};
    }

    
    private static List<String> getSurvivers(final DataTableSpec left,
            final DataTableSpec right) {
        // hash column names from right table
        HashSet<String> hash = new HashSet<String>();
        for (int i = 0; i < left.getNumColumns(); i++) {
            String name = left.getColumnSpec(i).getName();
            hash.add(name);
        }
        
        // determine the "survivers"
        ArrayList<String> survivers = new ArrayList<String>();
        for (int i = 0; i < right.getNumColumns(); i++) {
            String name = right.getColumnSpec(i).getName();
            if (!hash.contains(name)) {
                survivers.add(name);
            }
        }
        return survivers;
    }
    
    private DataTableSpec createJoinedSpec(final DataTableSpec[] inSpecs)
    throws InvalidSettingsException {
        DataTableSpec left = inSpecs[0];
        DataTableSpec right = inSpecs[1];
        
        if (m_settings.duplicateHandling() == DuplicateHandling.FAIL) {
            return new DataTableSpec(left, right);
        } else if (m_settings.duplicateHandling() == DuplicateHandling.FILTER) {
            List<String> survivers = getSurvivers(left, right);
            DataTableSpec newRight = FilterColumnTable.createFilterTableSpec(
                    right, survivers.toArray(new String[survivers.size()]));
            return new DataTableSpec(left, newRight);
        } else if (m_settings.duplicateHandling()
                == DuplicateHandling.APPEND_SUFFIX) {
            final int rightColCount = right.getNumColumns();
            HashSet<String> newInvented = new HashSet<String>();
            DataColumnSpec[] newCols = new DataColumnSpec[rightColCount];
            for (int i = 0; i < rightColCount; i++) {
                DataColumnSpec col = right.getColumnSpec(i);
                String name = col.getName();
                boolean invented = false;
                while (left.containsName(name) || newInvented.contains(name)) {
                    invented = true;
                    do {
                        name = name.toString() + m_settings.duplicateSuffix();
                        // we need also the keep track that we don't "invent" a
                        // name that is used in the right table already
                    } while (right.containsName(name));
                }
                if (invented) {
                    newInvented.add(name);
                    DataColumnSpecCreator creator = new DataColumnSpecCreator(
                            col);
                    creator.setName(name);
                    newCols[i] = creator.createSpec();
                } else {
                    newCols[i] = col;
                }
            }
            DataTableSpec newRight = new DataTableSpec(newCols);
            return new DataTableSpec(left, newRight);
        }
        throw new IllegalStateException("Unknown duplicate handling method: "
                + m_settings.duplicateHandling());
    }
    
    /**
     * @see org.knime.core.node.NodeModel#execute(
     *      org.knime.core.node.BufferedDataTable[],
     *      org.knime.core.node.ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable leftTable = inData[0], rightTable = inData[1];

        final DataTableSpec leftTableSpec = leftTable.getDataTableSpec();
        final DataTableSpec rightTableSpec = rightTable.getDataTableSpec();
        
        DataTableSpec joinedSpec = createJoinedSpec(new DataTableSpec[] {
                leftTableSpec, rightTableSpec});
        
        if ((leftTable.getRowCount() < 1) || (rightTable.getRowCount() < 1)) {
            return new BufferedDataTable[] {
                  exec.createDataContainer(joinedSpec).getTable()
            };
        }
        
        exec.setMessage("Sorting left table");
        SortedTable leftSorted = new SortedTable(leftTable,
                Collections.singletonList(m_settings.leftColumn()),
                new boolean[] {true}, m_settings.sortInMemory(), exec);
        exec.setMessage("Sorting right table");
        SortedTable rightSorted = new SortedTable(rightTable,
                Collections.singletonList(m_settings.rightColumn()),
                new boolean[] {true}, m_settings.sortInMemory(), exec);
        
        BufferedDataContainer dc = exec.createDataContainer(joinedSpec);

        
        List<String> survivors = getSurvivers(leftTableSpec, rightTableSpec);
        final boolean[] appendColumn =
            new boolean[rightTableSpec.getNumColumns()];
        for (int i = 0; i < appendColumn.length; i++) {
            if (survivors.contains(rightTableSpec.getColumnSpec(i).getName())) {
                appendColumn[i] = true;
            }
        }
        
        final int leftIndex = leftTableSpec.findColumnIndex(
                m_settings.leftColumn());
        final int rightIndex = rightTableSpec.findColumnIndex(
                m_settings.rightColumn());

        DataValueComparator comp = leftTableSpec.getColumnSpec(leftIndex)
            .getType().getComparator();
        
        exec.setMessage("Joining tables");
        RowIterator leftIt = leftSorted.iterator();
        RowIterator rightIt = rightSorted.iterator();
        DataCell lastRightCell = null;

        DataRow leftRow = leftIt.next();
        DataRow rightRow = rightIt.next();
        DataCell leftCell = leftRow.getCell(leftIndex);
        DataCell rightCell = rightRow.getCell(rightIndex);

        DataCell[] missingCells = new DataCell[rightRow.getNumCells()];
        for (int k = 0; k < missingCells.length; k++) {
            missingCells[k] = DataType.getMissingCell();
        }
        final DataRow missingRow = new DefaultRow(new StringCell("TEMP"),
                missingCells);
        
        int count = 0;
        final double max = leftTable.getRowCount();
        List<DataCell> duplicateCells = new ArrayList<DataCell>();
        while (true) {
            if (count++ % 1000 == 0) {
                exec.checkCanceled();
                exec.setProgress(count / max);
            }
            
            if (leftCell.equals(rightCell)) {
                DataRow joinedRow = new AppendedColumnRow(leftRow, rightRow,
                        appendColumn);
                dc.addRowToTable(joinedRow);
                if (leftIt.hasNext()) {
                    leftRow = leftIt.next();
                    leftCell = leftRow.getCell(leftIndex);
                } else {
                    break;
                }
            } else if (comp.compare(leftCell, rightCell) < 0) {
                if (!m_settings.innerJoin()) {
                    DataRow joinedRow = new AppendedColumnRow(leftRow,
                            missingRow, appendColumn);
                    dc.addRowToTable(joinedRow);                    
                }
                
                if (leftIt.hasNext()) {
                    leftRow = leftIt.next();
                    leftCell = leftRow.getCell(leftIndex);
                } else {
                    break;
                }
            } else {
                if (rightIt.hasNext()) {
                    rightRow = rightIt.next();
                    rightCell = rightRow.getCell(rightIndex);

                    if ((lastRightCell != null)
                            && (lastRightCell.equals(rightCell))) {
                        duplicateCells.add(lastRightCell);
                    }
                    lastRightCell = rightCell;
                } else {
                    break;
                }
            }            
        }
        
        dc.close();
        
        if (duplicateCells.size() > 0) {
            setWarningMessage("Right table contained " + duplicateCells.size() 
                    + " duplicate values in join column: "
                    + (duplicateCells.size() <= 15 ? duplicateCells
                            : duplicateCells.subList(0, 15) + "..."));
        }
        return new BufferedDataTable[] {dc.getTable()};
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    /**
     * @see org.knime.core.node.NodeModel
     *  #loadValidatedSettingsFrom(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    /**
     * @see org.knime.core.node.NodeModel#saveSettingsTo(
     *      org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(
     *      org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do here
    }
}
