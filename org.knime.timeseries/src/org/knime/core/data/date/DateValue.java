/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2009
 * KNIME.com, Zurich, Switzerland
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
 * 
 * History
 *   26.07.2009 (Fabian Dill): created
 */
package org.knime.core.data.date;

import java.util.Calendar;

import javax.swing.Icon;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;

/**
 * Representation of a date with year, month and day.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public interface DateValue extends DataValue {
    
    /**
     * 
     * @return the year value of this date 
     * @see Calendar#YEAR
     */
    public int getYear();
    
    /**
     * 
     * @return the month field of this date
     * @see Calendar#MONTH
     */
    public int getMonth();
    
    /**
     * 
     * @return the day of month of this date
     * @see Calendar#DAY_OF_MONTH
     */
    public int getDay();
    
    /**
     * Meta information for this data type.
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new DateUtilityFactory();

    /**
     * 
     * 
     * @author Fabian Dill, KNIME.com, Zurich, Switzerland
     */
    public static class DateUtilityFactory extends UtilityFactory {
        
        private static final DateTimeValueComparator COMPARATOR 
            = new DateTimeValueComparator(DateTimeValueComparator.Type.Date);
        
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON = loadIcon(
                DateValue.class, "icons/date.png");
        
        /**
         * Only subclasses are intended to instantiate this class.
         */
        protected DateUtilityFactory() {
            // empty
        }
        
        /**
         * 
         * {@inheritDoc}
         */
        @Override
        protected DataValueComparator getComparator() {
            return COMPARATOR;
        }
        
        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public Icon getIcon() {
            return ICON;
        }
        
        /**
         * 
         * {@inheritDoc}
         */
        @Override
        protected DataValueRendererFamily getRendererFamily(
                final DataColumnSpec spec) {
            return new DefaultDataValueRendererFamily(
                    DateValueRenderer.EU,
                    DateValueRenderer.US,
                    DateValueRenderer.STANDARD);
        }
    }
    
}
