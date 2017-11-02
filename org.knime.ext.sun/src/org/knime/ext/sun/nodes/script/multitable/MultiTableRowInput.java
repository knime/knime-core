package org.knime.ext.sun.nodes.script.multitable;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.streamable.RowInput;

/**
 * RowInput that unifies two rows from two input tables into one virtual row.
 *
 * @author Stefano Woerner
 */
public class MultiTableRowInput extends RowInput {

    // The spec of the rows produced by this class
    private final DataTableSpec m_spec;

    // Iterator over the left table
    private final CloseableRowIterator m_leftIterator;

    // Iterator over the right table
    private CloseableRowIterator m_rightIterator;

    // Current row in the left table
    private DataRow m_currentLeftRow;

    // Number of rows, equals #rowsLeft * #rowsRight
    private final long m_rowCount;

    private BufferedDataTable m_leftTable;

    private BufferedDataTable m_rightTable;

    /**
     * Initialize with tables.
     *
     * @param leftTable First table
     * @param rightTable Second table
     */
    public MultiTableRowInput(final BufferedDataTable leftTable, final BufferedDataTable rightTable) {
        if (leftTable == null || rightTable == null) {
            throw new IllegalArgumentException("Error constructing MultiTableRowInput: one of the tables is null.");
        }
        m_leftTable = leftTable;
        m_rightTable = rightTable;
        m_spec = MultiSpecHandler.createJointSpec(leftTable.getDataTableSpec(), rightTable.getDataTableSpec());

        m_rowCount = leftTable.size() * rightTable.size();
        if (m_rowCount <= 0) {
            throw new IllegalArgumentException("The input tables for a MultiTableRowInput cannot be empty.");
        }
        m_leftIterator = leftTable.iterator();
        m_rightIterator = rightTable.iterator();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow poll() throws InterruptedException {
        DataRow rightRow;
        // For every left row, we go through the right table.
        // If the right iterator is done, we advance the left one.
        if (m_rightIterator.hasNext()) {
            rightRow = m_rightIterator.next();
        } else {
            m_currentLeftRow = null;
            m_rightIterator.close();
            m_rightIterator = m_rightTable.iterator();
            rightRow = m_rightIterator.next();
        }

        if (m_currentLeftRow != null) {
            return createJointRow(m_currentLeftRow, rightRow);
        } else if (m_leftIterator.hasNext()) {
            m_currentLeftRow = m_leftIterator.next();
            return createJointRow(m_currentLeftRow, rightRow);
        }

        return null;
    }

    /**
     * Creates a joint {@link DataRow} that contains all cells of both input rows.
     *
     * @param leftRow row of the left table
     * @param rightRow row of the right table
     * @return A {@link DataRow} containing the cells of both input rows
     */
    private DataRow createJointRow(final DataRow leftRow, final DataRow rightRow) {
        return new VirtualJointRow(leftRow, rightRow);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        m_leftIterator.close();
        m_rightIterator.close();
    }

    /**
     * Returns the row count if the tables passed during construction were {@link BufferedDataTable}s. Otherwise -1 is
     * returned.
     *
     * @return the number of rows in the table - or -1 if the underlying table is not a buffered data table.
     * @since 2.12
     */
    public long getRowCount() {
        return m_rowCount;
    }
    /**
     * Returns the row count if the tables passed during construction were {@link BufferedDataTable}s. Otherwise -1 is
     * returned.
     *
     * @return the number of rows in the table - or -1 if the underlying table is not a buffered data table.
     * @since 2.12
     */
    public long getLeftRowCount() {
        return m_leftTable.size();
    }
    /**
     * Returns the row count if the tables passed during construction were {@link BufferedDataTable}s. Otherwise -1 is
     * returned.
     *
     * @return the number of rows in the table - or -1 if the underlying table is not a buffered data table.
     * @since 2.12
     */
    public long getRightRowCount() {
        return m_rightTable.size();
    }

}
