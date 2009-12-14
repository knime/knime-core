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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   12.01.2007 (mb): created
 */
package org.knime.core.data;

import java.util.Date;

import javax.swing.Icon;


/**
 * Value interface of data cells holding day/time information.
 * 
 * @author M. Berthold, University of Konstanz
 * @deprecated Date and time in KNIME is represented by 
 * {@link org.knime.core.data.date.DateAndTimeValue} and
 * {@link org.knime.core.data.date.DateAndTimeCell}. This interface will be 
 * removed in future versions of KNIME.
 */
@Deprecated
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
            loadIcon(TimestampValue.class, "/icon/timeicon.png");

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
