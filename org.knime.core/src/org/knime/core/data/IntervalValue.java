/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
