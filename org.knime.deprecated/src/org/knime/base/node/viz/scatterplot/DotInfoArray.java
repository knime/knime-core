/*
 *
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.scatterplot;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Class that holds all <code>DotInfo</code>s. You can modify the DotInfos if
 * you need to. Just sort them after the modifications are done. Some functions
 * of this class depend on the fact that the Array is sorted.
 * 
 * @author ohl University of Konstanz
 * @author Christoph Sieb University of Konstanz
 */
public class DotInfoArray {
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//                DotInfoArray.class);

    /**
     * To be passed in the sort method to determine the first and second order
     * criteria. Here: first x then y
     */
    public static final int X_THEN_Y_SORT_POLICY = 1;

    /**
     * To be passed in the sort method to determine the first and second order
     * criteria. Here: first y then x
     */
    public static final int Y_THEN_X_SORT_POLICY = 2;

    // keeps a list of DotInfos.
    private DotInfo[] m_dots;

    /**
     * Creates a new empty container for <code>DotInfo</code>s. Reserving
     * space for size elements (cannot be resized!)
     * 
     * @param size the number of elements to be stored in this array.
     */
    public DotInfoArray(final int size) {
        m_dots = new DotInfo[size];
    }

    /**
     * Creates a new array container for <code>DotInfo</code>s. The reference
     * to the array passed in will be taken over.
     * 
     * @param dots A reference to an array that will become part of this object
     */
    public DotInfoArray(final DotInfo[] dots) {
        m_dots = dots;
    }

    /**
     * Sorts (or re-sorts) the array of DotInfos by the X and Y coordinates.
     * 
     * @param sortPolicy determines which coordinate is the first and second
     *            order cirteria. This results in two order criterias: first x
     *            then y and vice versa. The following two policies are
     *            available <code>DotInfoArray.X_THEN_Y_SORT_POLICY</code> and
     *            <code>DotInfoArray.Y_THEN_X_SORT_POLICY</code>.
     */
    public void sort(final int sortPolicy) {

        // create the comparator for the given sort policy
        Comparator<DotInfo> comparator = new CoordComp(sortPolicy);

        Arrays.sort(m_dots, comparator);
    }

    /**
     * @return the number of DotInfos stored in this structure.
     */
    public int length() {
        return m_dots.length;
    }

    /**
     * @return an array of DotInfo. This is not a copy of the array but a
     *         reference to the internal datastructure of this object. It is
     *         shared by many. Don't mess with it!
     */
    public DotInfo[] getDots() {
        return m_dots;
    }

    /**
     * Returns a list of dots that sit on the specified coordinates. It takes
     * into account the size of each dot, so if the rectangle drawn covers the
     * coordinates the dot will be included. This function really depends on the
     * array being sorted!!
     * 
     * @param x the X coordinate.
     * @param y the Y coordinate.
     * @param dotSize the size (width and height) of one dot.
     * @return always a non-null ArrayList of DotInfos. It is of length null, if
     *         no dots are set yet or cover the coordinates.
     */
    public List<DotInfo> getDotsAt(final int x, 
                                   final int y, 
                                   final int dotSize) {
        List<DotInfo> result = new ArrayList<DotInfo>();
        for (int i = 0; i < m_dots.length; i++) {
            DotInfo dot = m_dots[i];
            int size = (int)Math.round(dotSize * (dot.getSize() + 0.5));
            int halfSize = (int)Math.round(size / 2.0);
            Rectangle rect = new Rectangle(
                    dot.getXCoord() - halfSize,
                    dot.getYCoord() - halfSize,
                    size, size);
            if (rect.contains(x, y)) {
                result.add(m_dots[i]);    
            }
            
        }
        return result;
//        return getDotsContainedIn(x, y, x, y, dotSize);
    }

    /**
     * Returns a list of dots that sit in the specified rectangle (including the
     * boundaries of the rectangle). It takes into account the size of each dot.
     * 
     * @param x1 the X coordinate of one corner of the rectangle
     * @param y1 the Y coordinate of one corner of the rectangle
     * @param x2 the X coordinate of the opposite corner
     * @param y2 the Y coordinate of the opposite corner
     * @param dotSize the width and height of each dot
     * @return always a non-null ArrayList of DotInfos. It is of length null, if
     *         no dots are set yet or sit on/in the specified rectangle.
     */
    public List<DotInfo> getDotsContainedIn(final int x1, final int y1,
            final int x2, final int y2, final int dotSize) {
        /*
         * TODO: Must be changed if we start drawing different shapes. Also, 
         * this function assumes that the real x/y coord. is not in the middle 
         * of the drawn rectangle but at the upper left corner. And - the 
         * orientation has not changed from the default which is (0,0) sits in 
         * the upper left corner. This function really depends on the array 
         * being sorted!!
         */

        Rectangle dragRect = new Rectangle(x1, y1, x2 - x1, y2 - y1);

        ArrayList<DotInfo> result = new ArrayList<DotInfo>();
        if ((m_dots == null) || (m_dots.length == 0)) {
            return result;
        }
        
        for (int i = 0; i < m_dots.length; i++) {
            int size = calculateDotSize(dotSize, m_dots[i].getSize());
            int halfSize = (int)Math.round(size / 2.0);
            Rectangle dotRect = new Rectangle(
                    m_dots[i].getXCoord() - halfSize,
                    m_dots[i].getYCoord() - halfSize,
                    size, size);
            if (dragRect.intersects(dotRect)) {
                result.add(m_dots[i]);
            }
        }
        return result;
    }
    
    /**
     * Returns one side of the square making up a dot.
     * Every dot may have a individual size [0,1] which is also considered as 
     * well as the general user-defined size of the dots [1, 150]. 
     * A dot with individual size 0.5 is considered to have the general dot 
     * size.
     * @param basicSize the user defined dot size.
     * @param dotSize the individual and relative dot size [0,1].
     * @return the side length of the square making up a dot.
     */
    public static final int calculateDotSize(final int basicSize, 
            final double dotSize) {
        // calculate the size of the dot
        // the percentage value must be converted in a dot size
        // (square length). 50% is the current dot size m_dotSize
        return (int)Math.round(basicSize * (dotSize + 0.5));
    }

    /**
     * The comparator for <code>DotInfo</code>s. It will decide by looking at
     * the X and Y coordinates which of the dots is the larger one. The sorting
     * order is determined by the sort policy.
     * 
     * @author ohl University of Konstanz
     * @author Christoph Sieb University of Konstanz
     */
    private static class CoordComp 
                    implements Comparator<DotInfo>, Serializable {

        /**
         * Sorting order of the coordinates.
         */
        private int m_sortPolicy;

        /**
         * Constructs a comparator for <code>DotInfo</code>s with the given
         * sort policy (first x then y or first y then x).
         * 
         * @param sortPolicy the sort policy
         */
        public CoordComp(final int sortPolicy) {
            m_sortPolicy = sortPolicy;
        }

        /**
         * {@inheritDoc}
         */
        public int compare(final DotInfo o1, final DotInfo o2) {

            if ((o1 == null) || (o2 == null)) {
                throw new NullPointerException("At least one of the DotInfo "
                        + "Objects to compare is null.");
            }

            int firstOrderCriteria1 = 0;
            int firstOrderCriteria2 = 0;
            int secondOrderCriteria1 = 0;
            int secondOrderCriteria2 = 0;

            if (m_sortPolicy == DotInfoArray.X_THEN_Y_SORT_POLICY) {
                firstOrderCriteria1 = o1.getXCoord();
                firstOrderCriteria2 = o2.getXCoord();
                secondOrderCriteria1 = o1.getYCoord();
                secondOrderCriteria2 = o2.getYCoord();
            } else if (m_sortPolicy == DotInfoArray.Y_THEN_X_SORT_POLICY) {
                firstOrderCriteria1 = o1.getYCoord();
                firstOrderCriteria2 = o2.getYCoord();
                secondOrderCriteria1 = o1.getXCoord();
                secondOrderCriteria2 = o2.getXCoord();
            }
            if (firstOrderCriteria1 < firstOrderCriteria2) {
                return -1;
            } else if (firstOrderCriteria1 > firstOrderCriteria2) {
                return 1;
            } else if (firstOrderCriteria1 == firstOrderCriteria2) {
                // in case the first sort criteria is equal
                // check the second second criteria

                if (secondOrderCriteria1 < secondOrderCriteria2) {
                    return -1;
                } else if (secondOrderCriteria1 > secondOrderCriteria2) {
                    return 1;
                } else if (secondOrderCriteria1 == secondOrderCriteria2) {
                    // at this stage all values are equal
                    return 0;
                }
            }

            throw new IllegalStateException("During sorting DotInfo a state "
                    + "was reached which should never occure.");
        }
    }

}
