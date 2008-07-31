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
 * History
 *   14.05.2007 (Fabian Dill): created
 */
package org.knime.base.node.preproc.columnTrans;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class AbstractMany2OneCellFactory implements CellFactory {
    
    private String m_appendedColumnName;
    private int[] m_includedColsIndices;
    private Set<DataCell> m_columnNames;
    private DataTableSpec m_inputSpec;
    
    /**
     * 
     * @param inputSpec input spec of the whole table
     * @param appendedColumnName name of the new column
     * @param includedColsIndices indices of columns to condense
     */
    public AbstractMany2OneCellFactory(final DataTableSpec inputSpec,
            final String appendedColumnName, final int[]includedColsIndices) {
        m_inputSpec = inputSpec;
        m_appendedColumnName = appendedColumnName;
        m_includedColsIndices = includedColsIndices;
        m_columnNames = new HashSet<DataCell>();
        for (int i : m_includedColsIndices) {
            m_columnNames.add(new StringCell(
                    m_inputSpec.getColumnSpec(i).getName()));
        }
    }
    
    
    /**
     * 
     * @return name of the appended column
     */
    public String getAppendedColumnName() {
        return m_appendedColumnName;
    }
    
    
    /**
     * 
     * @return the indices of the condensed columns
     */
    public int[] getIncludedColIndices() {
        return m_includedColsIndices;
    }

    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {
        // new column
        DataColumnSpecCreator appendedColumnCreator 
            = new DataColumnSpecCreator(m_appendedColumnName, StringCell.TYPE);
        // possible values depend on allow multi occurences
            DataColumnDomainCreator possibleValuesCreator 
                = new DataColumnDomainCreator(m_columnNames);
            appendedColumnCreator.setDomain(
                    possibleValuesCreator.createDomain());
        return new DataColumnSpec[] {appendedColumnCreator.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, 
            final int rowCount, final RowKey lastKey,
            final ExecutionMonitor exec) {
        exec.setProgress((double)curRowNr / (double)rowCount); 
    }
    
    /**
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {
        // find matching values
        int matchingValue = findColumnIndex(row);
        DataCell newCell;
        if (matchingValue == -1) {
            newCell = DataType.getMissingCell();
        } else {
            newCell = new StringCell(
                    m_inputSpec.getColumnSpec(matchingValue).getName());
        }
        return new DataCell[] {newCell};
    }

    /**
     * Find the column names to put in the condensed column.
     * 
     * @param row row to search for matching columns
     * @return matching  column names as StringCell array
     */
    public abstract int findColumnIndex(final DataRow row);
    
}
