/*
 * ------------------------------------------------------------------ *
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   12.01.2007 (mb): created
 */
package org.knime.timeseries.types;

import java.util.Date;

import javax.swing.Icon;

import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;

/**
 * Value interface of data cells holding day/time information.
 * 
 * @author M. Berthold, University of Konstanz
 */
public interface TimestampValue extends DataValue {
    
    /** Meta information to <code>TimestampValue</code>.
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new TimestampUtilityFactory();
    
    /**
     * @return date representation
     */
    Date getDate();
    
    /** Implementations of the meta information of this value class. */
    public static class TimestampUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON = 
            loadIcon(TimestampValue.class, "/Timestamp.png");

        private static final TimestampValueComparator COMPARATOR =
            new TimestampValueComparator();
        
        /** Only subclasses are allowed to instantiate this class. */
        protected TimestampUtilityFactory() {
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
    
    /**
     * Comparator returned by the {@link TimestampValue} interface.
     */
    public class TimestampValueComparator extends DataValueComparator {

        /**
         * Compares two {@link TimestampValue}s based on their <code>Date</code>
         * value.
         * 
         * @param v1 the first {@link TimestampValue} to compare the other with
         * @param v2 the other {@link TimestampValue} to compare the first with
         * @return what a comparator is supposed to return.
         * 
         * @throws ClassCastException If one of the arguments is 
         *          not <code>TimestampValue</code> type.
         * @throws NullPointerException If any argument is <code>null</code>.
         * 
         * @see Date#compareTo(Date)
         */
        @Override
        public int compareDataValues(final DataValue v1, final DataValue v2) {
            Date d1 = ((TimestampValue)v1).getDate();
            Date d2 = ((TimestampValue)v2).getDate();
            return d1.compareTo(d2);
        }

    }
}
