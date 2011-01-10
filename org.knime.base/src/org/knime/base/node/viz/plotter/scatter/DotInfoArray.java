/*
 *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.plotter.scatter;

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
         * The ordering of the dots depends on the sort policy where the dots 
         * may be sorted first on the x and then on the y value or vice versa.
         * 
         * @param o1 one dot
         * @param o2 the other dot to be compared with o1
         * @return 0 if both dots are the same, -1 if o1 < o2 and 1 if o1 > o2.
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
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
