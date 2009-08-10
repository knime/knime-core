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
import org.knime.core.data.date.DateTimeValueComparator.Type;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public interface DateTimeValue extends DateValue, TimeValue {
    
    /**
     * 
     * @return the time in milliseconds to be used by {@link Calendar}
     * 
     * @see Calendar#getTimeInMillis()
     */
    public long getTimeInMillis();
    
    /** Meta information utility. */
    public static final UtilityFactory UTILITY = new DateTimeUtilityFactory();

    
    /**
     * Meta information for {@link DateTimeValue}s.
     * 
     * @see DataValue#UTILITY
     * 
     * @author Fabian Dill, KNIME.com, Zurich, Switzerland
     */
    public static class DateTimeUtilityFactory extends UtilityFactory {
        
        private static final Icon ICON = loadIcon(DateTimeValue.class, 
                "icons/date_time.png");
        
        private static final DataValueComparator COMPARATOR 
            = new DateTimeValueComparator(Type.DateTime);
        
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
        protected DataValueComparator getComparator() {
            return COMPARATOR;
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        protected DataValueRendererFamily getRendererFamily(
                final DataColumnSpec spec) {
            return new DefaultDataValueRendererFamily(
                    new DateTimeValueRenderer(
                            DateValueRenderer.EU, 
                            TimeValueRenderer.EU_FULL
                    ),
                    new DateTimeValueRenderer(
                            DateValueRenderer.US, 
                            TimeValueRenderer.AM_PM_FULL
                    ),
                    new DateTimeValueRenderer(
                            DateValueRenderer.STANDARD, 
                            TimeValueRenderer.AM_PM_FULL
                            ),
                    new DateTimeValueRenderer(
                            DateValueRenderer.EU, 
                            TimeValueRenderer.EU_COMPACT
                    ),
                    new DateTimeValueRenderer(
                            DateValueRenderer.US, 
                            TimeValueRenderer.AM_PM_COMPACT
                            ),
                    new DateTimeValueRenderer(
                            DateValueRenderer.STANDARD, 
                            TimeValueRenderer.AM_PM_COMPACT
                            )
            );
        }
        
    }
}
