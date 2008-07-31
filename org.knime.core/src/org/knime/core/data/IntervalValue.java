/*
 * ------------------------------------------------------------------
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
 */
package org.knime.core.data;

import javax.swing.Icon;

/**
 * Interface supporting interval cells holding minimum and maximum boundaries.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public interface IntervalValue extends DataValue {

    /**
     * Meta information to this value type.
     * 
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new IntervalUtilityFactory();

    /**
     * @return minimum border
     */
    public double getLeftBound();

    /**
     * @return maximum border
     */
    public double getRightBound();

    /**
     * @return whether the left bound is included in the interval
     */
    public boolean leftBoundIncluded();

    /**
     * @return whether the right bound is included in the interval
     */
    public boolean rightBoundIncluded();

    /**
     * Determines if the given double value is contained in this interval, to
     * the left or to the right.
     * 
     * @param value the value to check
     * 
     * @return -1 if value is left to the interval, 0 if it is included an 1 if
     *         it is to the right of the interval
     */
    public int compare(double value);

    /**
     * Determines if the given double value is contained in this interval, to
     * the left or to the right.
     * 
     * @param value the value to check
     * 
     * @return -1 if value is left to the interval, 0 if it is included an 1 if
     *         it is to the right of the interval
     */
    public int compare(DoubleValue value);

    /**
     * Determines if the given {@link IntervalValue} is contained in this
     * interval.
     * 
     * @param value the interval to check
     * 
     * @return true if the value is completely contained in the interval
     */
    public boolean includes(IntervalValue value);

    /** Implementations of the meta information of this value class. */
    public static class IntervalUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON = 
            loadIcon(IntervalValue.class, "/icon/intervalicon.png");

        private static final IntervalValueComparator COMPARATOR =
                new IntervalValueComparator();

        /** Only subclasses are allowed to instantiate this class. */
        protected IntervalUtilityFactory() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Icon getIcon() {
            return ICON;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueComparator getComparator() {
            return COMPARATOR;
        }

    }

}
