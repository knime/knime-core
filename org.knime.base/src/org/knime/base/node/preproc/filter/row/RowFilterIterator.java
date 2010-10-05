/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * ----------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.filter.row;

import java.util.NoSuchElementException;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * Row iterator of the row filter table. Wraps a given row iterator and forwards
 * only rows that are approved by a given
 * {@link RowFilter}. Also a
 * range of row numbers can be specified and a flag to only include or exclude
 * rows within that range. (The range feature is ANDed to the filter match
 * result. If another operation on the row number is required an appropreate
 * filter has to be created.) <br>
 * The order of which the conditions are evaluated is as follows: If a range is
 * specified, the row number is checked against the specified range, only if it
 * matches the filter is asked to do its match. If the row number range fails it
 * is also checked if the end of the result table is reached due to the range
 * restrictions. (This should speed up the atEnd() check as we don't have to
 * traverse through the entire input table - which is actually the reason we
 * handle the row number range not in a filter.)
 * 
 * <p>
 * Note: Iterating may be slow as the iterator must potentially skip many rows
 * until it encounters a row to be returned. This iterator does also support
 * cancelation/progress information using an
 * {@link org.knime.core.node.ExecutionMonitor}.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class RowFilterIterator extends RowIterator {
    // the filter
    private final RowFilter m_filter;

    // the original row iterator we are wrapping
    private final RowIterator m_orig;

    // always holds the next matching row.
    private DataRow m_nextRow;

    // the number of rows read from the original. If m_nextRow is not null it
    // is the row number of that row in the original table.
    private int m_rowNumber;

    // If true the filter will not be asked - every row will be included in the
    // result.
    private boolean m_includeRest;

    // the exec mon for cancel/progress, may use default one
    private final ExecutionMonitor m_exec;

    // the row count in the original table
    private final int m_totalCountInOrig;

    /**
     * Creates a new row iterator wrapping an existing one and delivering only
     * rows that match the specified conditions.
     * 
     * @param origTable the original table from which we get the iterator and
     *            the row count, if any
     * @param filter a filter object that will decide whether rows are included
     *            in the result or filtered out
     * @param exec to report progress to and to check for cancel status
     */
    public RowFilterIterator(final DataTable origTable, final RowFilter filter,
            final ExecutionMonitor exec) {
        m_filter = filter;
        m_orig = origTable.iterator();
        int count = -1;
        if (origTable instanceof BufferedDataTable) {
            count = ((BufferedDataTable)origTable).getRowCount();
        }
        m_totalCountInOrig = count;
        m_exec = exec == null ? new ExecutionMonitor() : exec;

        m_rowNumber = 0;
        m_nextRow = null;
        m_includeRest = false;

        // get the next row to return - for the next call to next()
        m_nextRow = getNextMatch();

    }

    /**
     * Creates a new row iterator wrapping an existing one and delivering only
     * rows that match the specified conditions. No progress info or canceled
     * status is available.
     * 
     * @param orig the original table from which we get the iterator and the row
     *            count, if any
     * @param filter a filter object that will decide whether rows are included
     *            in the result or filtered out
     */
    public RowFilterIterator(final DataTable orig, final RowFilter filter) {
        this(orig, filter, null);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return (m_nextRow != null);
    }

    /**
     * This implementation may throw an RuntimeCanceledExecutionException
     * if this class has been initialized with a non-null execution monitor.
     * 
     * {@inheritDoc}
     */
    @Override
    public DataRow next() throws RuntimeCanceledExecutionException {
        if (m_nextRow == null) {
            throw new NoSuchElementException(
                    "The row filter iterator proceeded beyond the last row.");
        }
        DataRow tmp = m_nextRow;
        // always keep the next row in m_nextRow.
        m_nextRow = getNextMatch();
        return tmp;

    }

    /*
     * returns the next row that is supposed to be returned or null if it met
     * the end of it before.
     */
    private DataRow getNextMatch() {
        while (true) {
            try {
                m_exec.checkCanceled();
            } catch (CanceledExecutionException cee) {
                throw new RuntimeCanceledExecutionException(cee);
            }
            // we must not cause any trouble.
            if (!m_orig.hasNext()) {
                return null;
            }
            m_exec.setProgress(m_rowNumber / (double)m_totalCountInOrig);

            DataRow next = m_orig.next();
            if (m_includeRest) {
                m_rowNumber++;
                return next;
            } else {
                // consult the filter whether to include this row
                try {
                    if (m_filter.matches(next, m_rowNumber)) {
                        return next;
                    }
                    // else fall through and get the next row from the orig
                    // table.
                } catch (EndOfTableException eote) {
                    // filter: there are now more matching rows. Reached our
                    // EOT.
                    m_nextRow = null;
                    return null;
                } catch (IncludeFromNowOn ifno) {
                    // filter: include all rows from now on
                    m_includeRest = true;
                    return next;
                } finally {
                    m_rowNumber++;
                }
            }

        }
    }

    /**
     * Runtime exception that's thrown when the execution monitor's
     * {@link ExecutionMonitor#checkCanceled} method throws a
     * {@link CanceledExecutionException}.
     */
    public static final class RuntimeCanceledExecutionException extends
            RuntimeException {

        /**
         * Inits object.
         * 
         * @param cee The exception to wrap.
         */
        private RuntimeCanceledExecutionException(
                final CanceledExecutionException cee) {
            super(cee.getMessage(), cee);
        }

        /**
         * Get reference to causing exception.
         * 
         * {@inheritDoc}
         */
        @Override
        public CanceledExecutionException getCause() {
            return (CanceledExecutionException)super.getCause();
        }
    }
}
