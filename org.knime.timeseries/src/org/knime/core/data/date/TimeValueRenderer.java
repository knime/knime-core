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

import org.knime.core.data.renderer.DefaultDataValueRenderer;

/**
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public abstract class TimeValueRenderer extends DefaultDataValueRenderer {
    
    /** A 24 hour based time representation of time. */
    public static final TimeValueRenderer EU_FULL = new TimeValueRenderer() {

        /** Returns the 24 hour based time representation. */
        @Override
        public String getStringRepresentation(final TimeValue v) {
            return TimeRenderUtil.getStringForDateField(v.getHour()) 
                + ":" + TimeRenderUtil.getStringForDateField(v.getMinute()) 
                + ":" + TimeRenderUtil.getStringForDateField(v.getSecond())
                + "." + TimeRenderUtil.getStringForDateField(
                        v.getMilliSecond());
        }
        
        @Override
        public String getDescription() {
            return "13:24:30.5";
        }
    };
    
    /** A 24 hour based time representation of time. */
    public static final TimeValueRenderer EU_COMPACT = new TimeValueRenderer() {

        /** Returns the 24 hour based time representation. */
        @Override
        public String getStringRepresentation(final TimeValue v) {
            return TimeRenderUtil.getStringForDateField(v.getHour()) 
                + ":" + TimeRenderUtil.getStringForDateField(v.getMinute());
        }
        
        @Override
        public String getDescription() {
            return "13:24";
        }
    };

    /**
     * A 12 hour based representation of time with "am" and "pm" suffix.
     */
    public static final TimeValueRenderer AM_PM_FULL = new TimeValueRenderer() {
        @Override
        public String getStringRepresentation(final TimeValue v) {
            String amPm = v.getHour() < 12 ? "am" : "pm";
            return TimeRenderUtil.getStringForDateField(v.getHour() % 12) 
                + ":" + TimeRenderUtil.getStringForDateField(v.getMinute()) 
                + ":" + TimeRenderUtil.getStringForDateField(v.getSecond()) 
                + "." + TimeRenderUtil.getStringForDateField(v.getMilliSecond())
                + " " + amPm;
        }
        
        @Override
        public String getDescription() {
            return "1:24:30.5 pm";
        }
    };

    /**
     * A 12 hour based representation of time with "am" and "pm" suffix.
     */
    public static final TimeValueRenderer AM_PM_COMPACT 
        = new TimeValueRenderer() {
        @Override
        public String getStringRepresentation(final TimeValue v) {
            String amPm = v.getHour() < 12 ? "am" : "pm";
            return TimeRenderUtil.getStringForDateField(v.getHour() % 12) 
                + ":" + TimeRenderUtil.getStringForDateField(v.getMinute())
                + amPm;
        }
        
        @Override
        public String getDescription() {
            return "1:24pm";
        }
    };
    
    /**
     * 
     */
    public TimeValueRenderer() {
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof TimeValue) {
            super.setValue(
                    getStringRepresentation((TimeValue)value));
        } else {
            super.setValue(value);
        }
    }

    /**
     * 
     * @param v the time value which should be presented as a string
     * @return the referring string representation of the time value
     */
    public abstract String getStringRepresentation(final TimeValue v);
}
