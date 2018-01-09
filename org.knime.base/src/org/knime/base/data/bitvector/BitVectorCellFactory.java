/*
 * ------------------------------------------------------------------------
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
 *   13.02.2007 (Fabian Dill): created
 */
package org.knime.base.data.bitvector;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.NodeLogger;

/**
 * Base class for all cell factories converting strings to bitvectors.
 *
 * @author Fabian Dill, University of Konstanz
 * @author Tobias Koetter
 */
public abstract class BitVectorCellFactory extends SingleCellFactory {

    private AtomicInteger m_nrOfProcessedRows = new AtomicInteger(0);
    private AtomicInteger m_printedErrors = new AtomicInteger(0);
    private boolean m_wasSuccessfull = true;
    private String m_lastError = "";
    private boolean m_failOnError = false;

    /**
     * @param failOnError set to <code>true</code> if the factory should fail on errors or set to <code>false</code>
     * to have only an error message printed
     * @since 2.10
     */
    public void setFailOnError(final boolean failOnError) {
        m_failOnError = failOnError;
    }

    /**
     * @return <code>true</code> if the factory fails on parse errors
     * @since 2.10
     */
    public boolean failsOnError() {
        return m_failOnError;
    }

    /**
     * @param colIndices {@link List} with {@link Integer}s to convert
     * @return int array with the integers
     */
    static int[] convert2Array(final List<Integer> colIndices) {
        int[] idxs = new int[colIndices.size()];
        int i = 0;
        for (Integer idx : colIndices) {
            idxs[i++] = idx;
        }
        return idxs;
    }

    /**
     *
     * @param columnSpec the column spec of the new column
     */
    public BitVectorCellFactory(final DataColumnSpec columnSpec) {
        super(columnSpec);
    }

    /**
     *@param processConcurrently If to process the rows concurrently (must
     * only be true if there are no interdependency between the rows).
     * @param columnSpec the column spec of the new column
     * @see #setParallelProcessing(boolean)
     * @since 2.10
     */
    public BitVectorCellFactory(final boolean processConcurrently, final DataColumnSpec columnSpec) {
        super(processConcurrently, columnSpec);
    }

    /**
     * Increments the number of processed rows.
     *
     */
    public void incrementNrOfRows() {
        m_nrOfProcessedRows.getAndIncrement();
    }

    /**
     * Returns the number of processed rows.
     *
     * @return the number of processed rows.
     */
    public int getNrOfProcessedRows() {
        return m_nrOfProcessedRows.get();
    }

    /**
     *
     * @return the number of set bits.
     */
    public abstract int getNumberOfSetBits();

    /**
     *
     * @return the number of not set bits.
     */
    public abstract int getNumberOfNotSetBits();

    /**
     *
     * @return true if no error occurred otherwise false
     * @see #getLastErrorMessage()
     */
    public boolean wasSuccessful() {
        return m_wasSuccessfull;
    }

    /**
     * @return an error message if {@link #wasSuccessful()} returns <code>false</code> otherwise <code>null</code>
     * @since 2.10
     */
    public String getLastErrorMessage() {
        return m_lastError;
    }

    /**
     * @return the number of printed warnings
     * @since 2.10
     */
    public int getNoOfPrintedErrors() {
        return m_printedErrors.get();
    }

    /**
     * Logs the provided message to the provided logger. It suppresses messages
     * after 20 messages have been printed.
     *
     * @param logger messages are send to this instance
     * @param msg the message to print
     * @since 2.10
     */
    protected void printError(final NodeLogger logger, final String msg) {
        printError(logger, null, msg);
    }

    /**
     * Logs the provided message to the provided logger. It suppresses messages
     * after 20 messages have been printed.
     *
     * @param logger messages are send to this instance
     * @param row the {@link DataRow} the error occurred in or <code>null</code> if not available
     * @param msg the message to print
     * @since 2.10
     */
    protected void printError(final NodeLogger logger, final DataRow row, final String msg) {
        m_wasSuccessfull = false;
        final int noOfErrors = m_printedErrors.getAndIncrement();
        final String errMsg;
        if (row != null) {
            errMsg = "Error in row " + row.getKey() + ": " + msg;
        } else {
            errMsg = msg;
        }
        m_lastError = errMsg;
        if (m_failOnError) {
            throw new RuntimeException(m_lastError + (m_lastError.endsWith(".") ? "" : ".")
                + " Unselect the fail on parse error option to ignore parsing errors.");
        }
        if (noOfErrors < 20) {
            logger.error(errMsg);
            return;
        }
        if (noOfErrors == 20) {
            logger.error("Suppressing further error messages...");
        }
    }

}
