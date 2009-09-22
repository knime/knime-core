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
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 * 
 * History
 *   14.09.2009 (Fabian Dill): created
 */
package org.knime.core.data.date;

import org.knime.core.data.renderer.DefaultDataValueRenderer;

/**
 * Renders the a {@link TimestampValue}.
 * 
 * @author Fabian Dill, KNIME.com, Zurich, Switzerland
 */
public abstract class TimestampValueRenderer extends DefaultDataValueRenderer {
    
    /**
     * Renders the timestamp as yyyy/dd/mm and hh:mm:ss.S am/pm.
     */
    public static final TimestampValueRenderer US 
        = new TimestampValueRenderer() {

        @Override
        protected String getDateString(final TimestampValue value) {
            return TimeRenderUtil.getStringForDateField(value.getYear()) 
                + "/" + TimeRenderUtil.getStringForDateField(
                        value.getDayOfMonth()) + "/" 
                + TimeRenderUtil.getStringForDateField(value.getMonth());
        }

        @Override
        protected String getTimeString(final TimestampValue value) {
            String withoutMillis = TimeRenderUtil.getStringForDateField(
                    value.getHourOfDay() % 12) + ":" 
                    + TimeRenderUtil.getStringForDateField(value.getMinute())
                    + ":" + TimeRenderUtil.getStringForDateField(
                            value.getSecond());
            if (value.hasMillis()) {
                withoutMillis += "." + value.getMillis();
            }
            if (value.getHourOfDay() > 12) {
                withoutMillis += " pm";
            } else {
                withoutMillis += " am";
            }
            return withoutMillis;
        }
        
        @Override
            public String getDescription() {
                return "US: yyyy/dd/mm hh:mm:ss.S am/pm";
            }
        
    };
    
    /**
     * Renders the timestamp as yyyy/dd/mm and hh:mm:ss.S am/pm.
     */
    public static final TimestampValueRenderer EU 
        = new TimestampValueRenderer() {

        @Override
        protected String getDateString(final TimestampValue value) {
            return TimeRenderUtil.getStringForDateField(value.getDayOfMonth()) 
                + "." + TimeRenderUtil.getStringForDateField(
                        value.getMonth()) + "." 
                + TimeRenderUtil.getStringForDateField(value.getYear());
        }

        @Override
        protected String getTimeString(final TimestampValue value) {
            String withoutMillis = TimeRenderUtil.getStringForDateField(
                    value.getHourOfDay()) + ":" 
                    + TimeRenderUtil.getStringForDateField(value.getMinute())
                    + ":" + TimeRenderUtil.getStringForDateField(
                            value.getSecond());
            if (value.hasMillis()) {
                withoutMillis += "." + value.getMillis();
            }
            return withoutMillis;
        }
        
        @Override
            public String getDescription() {
                return "EU: hh:mm:ss.S dd.mm.yyyy";
            }
        
    };
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof TimestampValue) {
            super.setValue(getStringRepresentationFor((TimestampValue)value));
        } else {
            super.setValue(value);
        }
    }
    
    /**
     * 
     * @param value the timestamp to render 
     * @return a string representation of the passed timestamp
     */
    protected String getStringRepresentationFor(final TimestampValue value) {
        String timestamp = "";
        if (value.hasDate()) {
            timestamp = getDateString(value); 
        }
        if (value.hasTime()) {
            if (value.hasDate()) {
                // separate date from time using "T", see ISO 8601
                timestamp += "T";
            }
            timestamp += getTimeString(value);
        }
        if (value.hasMillis()) {
            timestamp += "." + value.getMillis();
        }
        return timestamp;
    }
    
    /**
     * Return a string representation of the date. One can safely assume that 
     * the date fields are set when this method is called.
     * 
     * @param value a timestamp value
     * @return a string representation of the date fields of that value
     */
    protected abstract String getDateString(final TimestampValue value);
    
    /**
     * Return a string representation of the passed time. One can safely assume
     * that the time fields are set.
     * 
     * @param value a timestamp value
     * @return a string representation of the time fields
     */
    protected abstract String getTimeString(final TimestampValue value);

}
