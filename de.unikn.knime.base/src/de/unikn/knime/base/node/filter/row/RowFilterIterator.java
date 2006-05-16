package de.unikn.knime.base.node.filter.row;

import java.util.NoSuchElementException;

import de.unikn.knime.base.node.filter.row.rowfilter.EndOfTableException;
import de.unikn.knime.base.node.filter.row.rowfilter.IncludeFromNowOn;
import de.unikn.knime.base.node.filter.row.rowfilter.RowFilter;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.RowIterator;

/**
 * Row iterator of the row filter table. Wraps a given row iterator and forwards
 * only rows that are approved by a given RowFilter. Also a range of row numbers
 * can be specified and a flag to only include or exclude rows within that
 * range. (The range feature is ANDed to the filter match result. If another
 * operation on the row number is required an appropreate filter has to be
 * created.) <br>
 * The order of which the conditions are evaluated is as follows: If a range is
 * specified, the row number is checked against the specified range, only if it
 * matches the filter is asked to do its match. If the row number range fails it
 * is also checked if the end of the result table is reached due to the range
 * restrictions. (This should speed up the atEnd() check as we don't have to
 * traverse through the entire input table - which is actually the reason we
 * handle the row number range not in a filter.)
 * 
 * @author ohl, University of Konstanz
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

    /**
     * Creates a new row iterator wrapping an existing one and delivering only
     * rows that match the specified conditions.
     * 
     * @param origIterator the original row iterator to wrap.
     * @param filter a filter object that will decide whether rows are included
     *            in the result or filtered out.
     */
    public RowFilterIterator(final RowIterator origIterator, 
            final RowFilter filter) {

        m_filter = filter;
        m_orig = origIterator;

        m_rowNumber = 0;
        m_nextRow = null;
        m_includeRest = false;

        // get the next row to return - for the next call to next()
        m_nextRow = getNextMatch();

    }

    /**
     * @see de.unikn.knime.core.data.RowIterator#hasNext()
     */
    public boolean hasNext() {
        return (m_nextRow != null);
    }

    /**
     * @see de.unikn.knime.core.data.RowIterator#next()
     */
    public DataRow next() {

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

            // we must not cause any trouble.
            if (!m_orig.hasNext()) {
                return null;
            }

            DataRow next = m_orig.next();
            if (m_includeRest) {
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

}
