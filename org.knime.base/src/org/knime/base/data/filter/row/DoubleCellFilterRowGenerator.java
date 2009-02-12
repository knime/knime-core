/*
 * ---------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 */
package org.knime.base.data.filter.row;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;

/**
 * This class implements the
 * {@link FilterRowGenerator} interface for
 * {@link org.knime.core.data.def.DoubleCell} objects and checks if they
 * are within a certain interval.
 * 
 * <p>
 * It provides two options, one uses a single border the other one a two border
 * interval. In general, each type allows to define if the border is included or
 * excluded, see <code>IN</code> and <code>OUT</code>. Furthermore, it is
 * possible to define if the range on the <code>LEFT</code> and/or on the
 * <code>RIGHT</code> is included to the interval. E.g., <code>LEFT+IN</code>,
 * <code>LEFT+OUT</code>, <code>RIGHT+IN</code>, <code>RIGHT+OUT</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class DoubleCellFilterRowGenerator implements FilterRowGenerator {

    /**
     * Values on the border are included.
     */
    public static final int IN = 1;

    /**
     * Values on the border are excluded.
     */
    public static final int OUT = 2;

    /**
     * Use interval on the left of the border.
     */
    public static final int LEFT = 4;

    /**
     * Use interval of the right of the border.
     */
    public static final int RIGHT = 8;

    /*
     * Flag for empty mask when using single border.
     */
    private static final int EMPTY_MASK = -1;

    /*
     * Keep column index.
     */
    private final int m_columnIndex;

    /*
     * Keeps double value borders, for single interval only the first element is
     * used.
     */
    private final double[] m_border;

    /*
     * Keeps mask for each border, for single interval only the first element is
     * used, the second is EMPTY_MASK. Each mask element consist of an
     * accumulated IN vs. OUT and LEFT vs. RIGHT value.
     */
    private final int[] m_mask;

    /**
     * @param columnIndex the column's index
     * @param doubleCell the double cell border
     * @param mask the interval mask
     * @see #DoubleCellFilterRowGenerator(int,double,int)
     * @throws NullPointerException if the double cell is <code>null</code>
     */
    public DoubleCellFilterRowGenerator(final int columnIndex,
            final DoubleValue doubleCell, final int mask) {
        this(columnIndex, doubleCell.getDoubleValue(), mask);
    }

    /**
     * Creates a new single border row filter.
     * 
     * @param columnIndex the column's index
     * @param doubleValue the double value border
     * @param mask the interval mask
     * @throws IllegalArgumentException if the column index is negative or the
     *             mask can not composed from <code>IN + LEFT</code>,
     *             <code>IN + RIGHT</code>, <code>OUT + LEFT</code>, or
     *             <code>OUT + RIGHT</code>
     */
    public DoubleCellFilterRowGenerator(final int columnIndex,
            final double doubleValue, final int mask) {
        // check for negative column index
        if (columnIndex < 0) {
            throw new IllegalArgumentException("Column index " + columnIndex
                    + " can not be negative.");
        }
        m_columnIndex = columnIndex;
        // init and set double array whereby the second value is not obmitted
        m_border = new double[]{doubleValue, Double.NaN};
        // check mask
        if (!((mask == (IN + LEFT)) ^ (mask == (IN + RIGHT))
                ^ (mask == (OUT + LEFT)) ^ (mask == (OUT + RIGHT)))) {
            throw new IllegalArgumentException("Row filter mask is invalid: "
                    + mask + ".");
        }
        // init and set mask whereby the second one is empty
        m_mask = new int[]{mask, EMPTY_MASK};
    }

    /**
     * @param columnIndex the column's index
     * @param doubleCellLeft the left border value
     * @param maskLeft the left mask
     * @param doubleCellRight the right border value
     * @param maskRight the right mask
     * @see #DoubleCellFilterRowGenerator(int,double,int,double,int)
     * @throws NullPointerException if one of the double cells is
     *             <code>null</code>
     */
    public DoubleCellFilterRowGenerator(final int columnIndex,
            final DoubleValue doubleCellLeft, final int maskLeft,
            final DoubleValue doubleCellRight, final int maskRight) {
        this(columnIndex, doubleCellLeft.getDoubleValue(), maskLeft,
                doubleCellRight.getDoubleValue(), maskRight);
    }

    /**
     * 
     * @param columnIndex the column's index
     * @param doubleLeft the left border value
     * @param maskLeft the left mask
     * @param doubleRight the right border value
     * @param maskRight the right mask
     * @throws IllegalArgumentException if the column index is negative, right
     *             and left border overlap, left or right mask can not be
     *             composed, or the mask conflict by referring to the same
     *             direction
     */
    public DoubleCellFilterRowGenerator(final int columnIndex,
            final double doubleLeft, final int maskLeft,
            final double doubleRight, final int maskRight) {
        // check for negative column index
        if (columnIndex < 0) {
            throw new IllegalArgumentException("Column index " + columnIndex
                    + " can not be negative.");
        }
        // keep column index
        m_columnIndex = columnIndex;

        // check borders
        if (doubleLeft >= doubleRight) {
            throw new IllegalArgumentException("Left and right boundaries "
                    + "do overlap: " + doubleLeft + " >= " + doubleRight + ".");
        }
        // keep left and right border value
        m_border = new double[]{doubleLeft, doubleRight};

        // check mask left
        if (!((maskLeft == (IN + LEFT)) ^ (maskLeft == (IN + RIGHT))
                ^ (maskLeft == (OUT + LEFT)) ^ (maskLeft == (OUT + RIGHT)))) {
            throw new IllegalArgumentException(
                    "Row filter mask left is invalid: " + maskLeft + ".");
        }
        // check mask right
        if (!((maskRight == (IN + LEFT)) ^ (maskRight == (IN + RIGHT))
                ^ (maskRight == (OUT + LEFT)) ^ (maskRight == (OUT + RIGHT)))) {
            throw new IllegalArgumentException(
                    "Row filter mask right is invalid: " + maskRight + ".");
        }

        // if both border include the same side, left, left or right, rigtht
        if ((maskLeft & LEFT) == LEFT && (maskRight & LEFT) == LEFT
                || (maskLeft & RIGHT) == RIGHT && (maskRight & RIGHT) == RIGHT) {
            throw new IllegalArgumentException(
                    "Conflicting boundary masks. Can not be the same side.");
        }
        m_mask = new int[]{maskLeft, maskRight};
    }

    /**
     * Checks if the given row lies within the define interval borders.
     * 
     * @param row the row which should be checked for being inside the interval
     * @return <code>true</code> if inside the define interval
     * @throws NullPointerException if the given row is <code>null</code>
     * @throws ClassCastException if the row's cell is not of type
     *             {@link org.knime.core.data.def.DoubleCell}
     */
    public boolean isIn(final DataRow row) {
        DataCell cell = row.getCell(m_columnIndex);
        if (cell.isMissing()) {
            return false;
        }
        // retrieve double value at column index
        double dbl = ((DoubleValue)cell).getDoubleValue();
        // if single border
        if (m_mask[1] == EMPTY_MASK) {
            return check(dbl, 0);
        } else { // two borders
            // if closed intervall
            if ((m_mask[0] & RIGHT) == RIGHT && (m_mask[1] & LEFT) == LEFT) {
                // has to be betweem both borders
                return check(dbl, 0) && check(dbl, 1);
            } else {
                // has to be at exactly one
                return check(dbl, 0) ^ check(dbl, 1);
            }
        }
    }

    /*
     * Checks if the double value is inside the interval at mask[b] with b={0,1}
     */
    private boolean check(final double dbl, final int b) {
        assert (b == 0 || b == 1);
        // if LEFT
        if ((m_mask[b] & LEFT) == LEFT) {
            // if IN
            if ((m_mask[b] & IN) == IN) {
                // less equal
                return dbl <= m_border[b];
            } else {
                // less
                return dbl < m_border[b];
            }
        } else { // RIGHT
            // if IN
            if ((m_mask[b] & IN) == IN) {
                // greater equal
                return dbl >= m_border[b];
            } else {
                // greater
                return dbl > m_border[b];
            }
        }
    }
}
