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
class MissingValueHandling2TableIterator extends RowIterator {
    private static final ExecutionMonitor EMPTY_EXEC = 
        new ExecutionMonitor(); 
    
    private final RowIterator m_internIt;
    private final MissingValueHandling2Table m_table;
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
    MissingValueHandling2TableIterator(final MissingValueHandling2Table table, 
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
    MissingValueHandling2TableIterator(final MissingValueHandling2Table table) {
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
                        case MissingValueHandling2ColSetting.METHOD_NO_HANDLING:
                            break;
                        case MissingValueHandling2ColSetting.METHOD_IGNORE_ROWS:
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
            MissingValueHandling2ColSetting colset = m_table.getColSetting(i);
            DataCell oldCell = row.getCell(i);
            DataCell newCell;
            if (oldCell.isMissing()) {
                switch (colset.getMethod()) {
                    case MissingValueHandling2ColSetting.METHOD_NO_HANDLING:
                        newCell = oldCell;
                        break;
                    case MissingValueHandling2ColSetting.METHOD_FIX_VAL:
                        newCell = m_table.getColSetting(i).getFixCell();
                        assert (newCell != null);
                        break;
                    case MissingValueHandling2ColSetting.METHOD_MOST_FREQUENT:
                        newCell = m_table.getMostFrequent(i);
                        break;
                    case MissingValueHandling2ColSetting.METHOD_MAX:
                        newCell = m_table.getMax(i);
                        break;
                    case MissingValueHandling2ColSetting.METHOD_MIN:
                        newCell = m_table.getMin(i);
                        break;
                    case MissingValueHandling2ColSetting.METHOD_MEAN:
                        // in contrast to the above, it will return
                        // a newly generate value, thus, only a double
                        double mean = m_table.getMean(i);
                        if (colset.getType() == MissingValueHandling2ColSetting.TYPE_DOUBLE) {
                            newCell = new DoubleCell(mean);
                        } else {
                            assert colset.getType() == MissingValueHandling2ColSetting.TYPE_INT;
                            newCell = new IntCell((int)Math.round(mean));
                        }
                        break;
                    case MissingValueHandling2ColSetting.METHOD_IGNORE_ROWS:
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
