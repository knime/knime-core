/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 */
package org.knime.base.data.join;

import java.util.BitSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.node.NodeLogger;


/**
 * What is worth to notice about the order: Ideally, the order in both tables is
 * the same. If that is not the case, the order of the <code>left</code> table
 * is the one that counts, followed by the ones that are in the
 * <code>right</code> table but not in the <code>left</code> one.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
class JoinedTableRowIterator extends RowIterator {

    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(JoinedTableRowIterator.class);

    /**
     * Reference to underlying table. We need frequent access when data is not
     * ordererd
     */
    private final JoinedTable m_table;

    /** Iterator over left table. The iteration is carried out once */
    private final RowIterator m_leftIt;

    /** The iterator over the left part, multiple instantiations. */
    private RowIterator m_rightIt;

    /**
     * Last ID that was returned from the right table (yes, true, it's the same
     * of what next() returned ...). We keep it globally to make sure that we
     * traverse the right table only once when we look for an id. If the
     * iterator had a pushBack method that wouldn't be necessary.
     */
    private RowKey m_lastRightID;

    /**
     * Gets important when we have traversed the left table and now gather the
     * leftovers from the right one. This is the next to return.
     */
    private DataRow m_nextRightRow;

    /**
     * Counter in the right table. No the flag rows that have been returned,
     * i.e. where there was a counterpart in the left table
     */
    private int m_rightItCounter;

    /** See above. An index is set true when it occurs in both tables */
    private final BitSet m_rightSet;

    /** Store if we skipped some rows in the right table (needs second iter). */
    private boolean m_hasSkippedRight;

    /**
     * Cells that are used when there is a left row but not the right one.
     * Instantiated in a lazy way.
     */
    private DataCell[] m_rightMissingCells;

    /** See m_rightMissingCells. */
    private DataCell[] m_leftMissingCells;

    /**
     * Creates new Iterator based on <code>table</code>.
     * 
     * @param table the table to iterate over
     * @throws NullPointerException if argument is <code>null</code>
     */
    protected JoinedTableRowIterator(final JoinedTable table) {
        m_table = table;
        m_leftIt = m_table.getLeftTable().iterator();
        initNewRightIterator();
        m_rightSet = new BitSet();
        m_lastRightID = null;
        m_hasSkippedRight = false;
        if (!m_leftIt.hasNext()) { // left table is empty
            m_nextRightRow = findNextRightRow();
        } else {
            m_nextRightRow = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_leftIt.hasNext() || m_nextRightRow != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        if (m_leftIt.hasNext()) {
            final DataRow left = m_leftIt.next();
            final RowKey leftID = left.getKey();
            assert (!leftID.equals(m_lastRightID));
            DataRow right;
            RowKey rightID;
            boolean cont = true;
            do {
                right = null;
                rightID = null;
                if (!m_rightIt.hasNext() && initNewRightIterator()) {
                    break;
                }
                right = nextRight();
                rightID = right.getKey();
                boolean madeWholeLoop = (m_lastRightID == null && !m_rightIt
                        .hasNext())
                        || rightID.equals(m_lastRightID);
                cont = !madeWholeLoop && !rightID.equals(leftID);
                if (cont) {
                    if (!m_hasSkippedRight) {
                        if (!m_table.isPrintedErrorOnSorting()) {
                            LOGGER.warn(
                                    "Either both tables don't have all rows in "
                                    + "common or they are sorted differently.");
                            LOGGER.warn(
                                    "(Iteration may have quadratic complexity "
                                    + "to ensure that all matching rows are "
                                    + "found.");
                            LOGGER.warn("I'll suppress further warnings.");
                            m_table.setPrintedErrorOnSorting(true);
                        }
                        m_hasSkippedRight = true;
                    }
                }
            } while (cont);
            // no matching right row found
            if (!leftID.equals(rightID)) {
                right = getRightMissing(leftID);
            } else {
                m_lastRightID = rightID;
                assert (rightID.equals(leftID));
                m_rightSet.set(m_rightItCounter);
            }
            if (!m_leftIt.hasNext()) {
                // if it was the last row in the left iterator, we will
                // init (for the last time) the right iterator. It will
                // collect all those rows that haven't been returned yet
                // (according to the bit set m_rightSet)
                if (m_hasSkippedRight) {
                    initNewRightIterator();
                }
                m_lastRightID = null;
                m_nextRightRow = findNextRightRow();
            }
            return new JoinedRow(left, right);
        } else {
            // in a perfect world, you don't come here...
            DataRow maybeNext = findNextRightRow();
            DataRow left = getLeftMissing(m_nextRightRow.getKey());
            DataRow merged = new JoinedRow(left, m_nextRightRow);
            m_nextRightRow = maybeNext;
            return merged;
        }
    } // next()

    /* Iterates new "right iterator", resets counter. */
    private boolean initNewRightIterator() {
        m_rightItCounter = -1;
        m_rightIt = m_table.getRightTable().iterator();
        return !m_rightIt.hasNext();
    }

    /* Returns the next row in the current right iterator, updates counter. */
    private DataRow nextRight() {
        m_rightItCounter++;
        return m_rightIt.next();
    }

    /* Called when there is no corresponding right row to the left one. */
    private DataRow getRightMissing(final RowKey key) {
        DataTableSpec spec = m_table.getRightTable().getDataTableSpec();
        if (m_rightMissingCells == null) {
            LOGGER.debug("Creating missing values for right table on key \""
                    + key + "\"");
            if (!m_table.isPrintedErrorOnMissing()) {
                printMissingError(false);
                m_table.setPrintedErrorOnMissing(true);
            }
            m_rightMissingCells = JoinedTable.createMissingCells(spec);
        }
        return new DefaultRow(key, m_rightMissingCells);
    }

    /* Called when there is no corresponding left row to the right one. */
    private DataRow getLeftMissing(final RowKey key) {
        DataTableSpec spec = m_table.getLeftTable().getDataTableSpec();
        if (m_leftMissingCells == null) {
            LOGGER.debug("Creating missing values for left table on key \""
                    + key + "\"");
            if (!m_table.isPrintedErrorOnMissing()) {
                printMissingError(true);
                m_table.setPrintedErrorOnMissing(true);
            }
            m_leftMissingCells = JoinedTable.createMissingCells(spec);
        }
        return new DefaultRow(key, m_leftMissingCells);
    }

    /* Prints an error (so far to the screen) that some rows are missing. */
    private void printMissingError(final boolean isLeft) {
        String side = (isLeft ? "left" : "right");
        String oSide = (isLeft ? "right" : "left");
        LOGGER.warn("The _" + side + "_ table does not "
                + "contain all rows of the _" + oSide + "_ table");
        LOGGER.warn("I'll fill those with missing cells.");
        LOGGER.warn("I'll suppress further warnings.");
    }

    /*
     * Used when left table has been traversed and rows in the right table (that
     * have not been returned yet) are next...
     */
    private DataRow findNextRightRow() {
        DataRow nextToReturn = null;
        while (m_rightIt.hasNext()) {
            nextToReturn = nextRight();
            if (!m_rightSet.get(m_rightItCounter)) {
                break;
            }
            nextToReturn = null;
        }
        return nextToReturn;
    }
}
