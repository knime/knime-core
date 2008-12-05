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
 *   05.08.2005 (bernd): created
 */
package org.knime.base.node.preproc.missingval;

import java.util.NoSuchElementException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;


/**
 * Iterator to MissingValueHandlingTable. 
 * @author Bernd Wiswedel, University of Konstanz
 */
class MissingValueHandlingTableIterator extends RowIterator {
    private static final ExecutionMonitor EMPTY_EXEC = 
        new ExecutionMonitor(); 
    
    private final RowIterator m_internIt;
    private final MissingValueHandlingTable m_table;
    private final ExecutionMonitor m_exec;
    private final int m_finalCount;
    private int m_count;
    // since we don't know if to skip the next row in the underlying table
    // (ColSetting.METHOD_IGNORE_ROWS), we save the next to return
    private DataRow m_next;
    
    /**
     * Creates new iterator from table <code>table</code>.
     * @param table the table to iterate on
     * @param exec the progress monitor for cancel/progress
     */
    MissingValueHandlingTableIterator(final MissingValueHandlingTable table, 
            final ExecutionMonitor exec) {
        m_internIt = table.getInternalIterator();
        m_table = table;
        m_count = 0;
        m_finalCount = m_table.getNrRowsInReference();
        m_exec = exec;
        push();
    }

    /**
     * Creates new iterator from table <code>table</code>.
     * @param table the table to iterate on
     */
    MissingValueHandlingTableIterator(final MissingValueHandlingTable table) {
        this(table, EMPTY_EXEC);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_next != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        DataRow result = m_next;
        push();
        return result;
    }
    
    /** pushes the internal iterator forward to the next row to return. */
    private void push() {
        DataRow row;
        boolean hasMissing;
        boolean skipRow;
        // make an iterative loop and look for the next row to return;
        // this method used to implement a recursion but with many rows 
        // containing missing values, you get a StackOverFlow, bug fix #350
        do {
            if (!m_internIt.hasNext()) {
                m_next = null;
                return;
            }
            row = m_internIt.next();
            if (m_finalCount > 0) {
                m_exec.setProgress(m_count / (double)m_finalCount);
            }
            try {
                m_exec.checkCanceled();
            } catch (CanceledExecutionException cee) {
                throw new RuntimeCanceledExecutionException(cee);
            }
            m_count++;
            // check once if we can get away easy
            hasMissing = false;
            skipRow = false;
            for (int i = 0; !skipRow && i < row.getNumCells(); i++) {
                if (row.getCell(i).isMissing()) {
                    switch (m_table.getColSetting(i).getMethod()) {
                        case ColSetting.METHOD_NO_HANDLING:
                            break;
                        case ColSetting.METHOD_IGNORE_ROWS:
                            skipRow = true;
                            break;
                        default:
                            hasMissing = true;
                        
                    }
                }
            }
        } while (skipRow);
        if (hasMissing) {
            m_next = handleMissing(row);
        } else {
            m_next = row;
        }
    }
    
    /* Does the missing value handling on a row. */
    private DataRow handleMissing(final DataRow row) {
        DataCell[] cells = new DataCell[row.getNumCells()];
        for (int i = 0; i < row.getNumCells(); i++) {
            ColSetting colset = m_table.getColSetting(i);
            DataCell oldCell = row.getCell(i);
            DataCell newCell;
            if (oldCell.isMissing()) {
                switch (colset.getMethod()) {
                    case ColSetting.METHOD_NO_HANDLING:
                        newCell = oldCell;
                        break;
                    case ColSetting.METHOD_FIX_VAL:
                        newCell = m_table.getColSetting(i).getFixCell();
                        assert (newCell != null);
                        break;
                    case ColSetting.METHOD_MOST_FREQUENT:
                        newCell = m_table.getMostFrequent(i);
                        break;
                    case ColSetting.METHOD_MAX:
                        newCell = m_table.getMax(i);
                        break;
                    case ColSetting.METHOD_MIN:
                        newCell = m_table.getMin(i);
                        break;
                    case ColSetting.METHOD_MEAN:
                        // in contrast to the above, it will return
                        // a newly generate value, thus, only a double
                        double mean = m_table.getMean(i);
                        if (colset.getType() == ColSetting.TYPE_DOUBLE) {
                            newCell = new DoubleCell(mean);
                        } else {
                            assert colset.getType() == ColSetting.TYPE_INT;
                            newCell = new IntCell((int)Math.round(mean));
                        }
                        break;
                    case ColSetting.METHOD_IGNORE_ROWS:
                        assert false : "That should have been filtered.";
                        newCell = oldCell;
                    default:
                        throw new RuntimeException("Invalid method!");
                }
            } else {
                newCell = oldCell;
            }
            cells[i] = newCell;
        }
        RowKey key = row.getKey();
        return new DefaultRow(key, cells);
    }
    
    
    /** Runtime exception that's thrown when the execution monitor's 
     * <code>checkCanceled</code> method throws an 
     * {@link CanceledExecutionException}.
     */ 
    static final class RuntimeCanceledExecutionException 
        extends RuntimeException {
        
        /** Inits object.
         * @param cee The exception to wrap.
         */
        private RuntimeCanceledExecutionException(
                final CanceledExecutionException cee) {
            super(cee.getMessage(), cee);
        }
        
        /** Get reference to causing exception.
         * {@inheritDoc}
         */ 
        @Override
        public CanceledExecutionException getCause() {
            return (CanceledExecutionException)super.getCause();
        }
    }
}
