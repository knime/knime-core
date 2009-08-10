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
 * 
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public interface TimeValue extends DataValue {
    

    /**
     * 
     * @return the hour of this time value
     * @see Calendar#HOUR_OF_DAY
     */
    public int getHour();

    /**
     * 
     * @return the minutes of this time value
     * @see Calendar#MINUTE
     */
    public int getMinute();
    
    /**
     * 
     * @return the seconds of this time value
     * @see Calendar#SECOND
     */
    public int getSecond();
    
    /**
     * 
     * @return the milliseconds of this time value
     * @see Calendar#MILLISECOND
     */
    public int getMilliSecond();

    /**
     * Meta information for time value. 
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new TimeUtilityFactory();
    
    /**
     * 
     * 
     * @author Fabian Dill, KNIME.com, Zurich, Switzerland
     */
    public static class TimeUtilityFactory extends UtilityFactory {
        
        private static final Icon ICON = loadIcon(
                TimeValue.class, "icons/time.png");
        
        private static final DataValueComparator COMPARATOR 
            = new DateTimeValueComparator(DateTimeValueComparator.Type.Time);
        
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
                    TimeValueRenderer.EU_FULL,
                    TimeValueRenderer.EU_COMPACT,
                    TimeValueRenderer.AM_PM_FULL,
                    TimeValueRenderer.AM_PM_COMPACT
            
            );
        }
        
    }
}
