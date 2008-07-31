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
 * ---------------------------------------------------------------------
 * 
 */
package org.knime.base.node.preproc.columnTrans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.ExecutionMonitor;

/**
 * Maps several original nominal columns to their possible values, creates a 
 * column for every possible value and when the rows are processed the value is 
 * set to 1 if the original column contains this value and to 0 otherwise.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class One2ManyCellFactory implements CellFactory {

    private Map<Integer/*colIdx*/, List<DataCell>/*possVal*/>m_possibleValues;
    private DataColumnSpec[] m_columnSpecs = new DataColumnSpec[0];
    
    /**
     * Creates for every possible value of one column given by the columnNames
     * an extra column with the values present(1) or absent(0).
     * @param inputSpec the input table spec.
     * @param columnNames the names of the columns to be transformed.
     * @param appendOrgColNames if true original column names will be appended
     *  to the newly generated column name: (possibleValue_originalColName)
     */
    public One2ManyCellFactory(final DataTableSpec inputSpec,
            final List<String>columnNames, final boolean appendOrgColNames) {
        m_possibleValues = new HashMap<Integer, List<DataCell>>();
        final List<DataColumnSpec>colSpecs = new ArrayList<DataColumnSpec>();
        final Set<DataCell>newDomainVals = new HashSet<DataCell>();
        newDomainVals.add(new IntCell(0));
        newDomainVals.add(new IntCell(1));
        final DataColumnDomainCreator domainCreator = 
            new DataColumnDomainCreator(newDomainVals, new IntCell(0), 
                    new IntCell(1));
        for (final String colName : columnNames) {
            final DataColumnSpec colSpec = inputSpec.getColumnSpec(colName); 
            if (colSpec.getDomain().hasValues()) {
                m_possibleValues.put(
                        inputSpec.findColumnIndex(colName),
                        new ArrayList<DataCell>(colSpec
                                .getDomain().getValues()));
                for (final DataCell newCol : colSpec.getDomain().getValues()) {
                    String newColumnName;
                    if (appendOrgColNames) {
                        newColumnName = newCol.toString() + "_" + colName;
                    } else {
                        newColumnName = newCol.toString();
                    }
                    final DataColumnSpecCreator creator 
                        = new DataColumnSpecCreator(newColumnName, 
                                IntCell.TYPE);
                    creator.setDomain(domainCreator.createDomain());
                    colSpecs.add(creator.createSpec());
                }
            } else {
                throw new IllegalArgumentException(
                        "No possible values found for column: " + colName);
            }
        }
        m_columnSpecs = colSpecs.toArray(m_columnSpecs);
    }
    
    /**
     * 
     * @return - the columnSpecs for the appended columns.
     */
    public DataColumnSpec[] getColumnSpecs() {
        return m_columnSpecs;
    }


    /**
     * 
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {
        final List<DataCell>appendedDataCells = new ArrayList<DataCell>();
        for (int col = 0; col < row.getNumCells(); col++) {
            if (m_possibleValues.containsKey(col)) {
                final List<DataCell>possVals = m_possibleValues.get(col);
                for (final DataCell value : possVals) {
                    if (row.getCell(col).equals(value)) {
                        appendedDataCells.add(new IntCell(1));
                    } else {
                        appendedDataCells.add(new IntCell(0));
                    }
                }    
            }
        }
        return appendedDataCells.toArray(new DataCell[]{});
    }

    /**
     * 
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount, 
            final RowKey lastKey, final ExecutionMonitor exec) {
       exec.setProgress((double)curRowNr / (double)rowCount); 
    }
    

}
