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
public abstract class DateValueRenderer extends DefaultDataValueRenderer {
    
    /**
     * Different kinds of string representations.
     * 
     * @author Fabian Dill, KNIME.com, Zurich, Switzerland
     */
        /** US presentation of a date as yyyy/dd/mm. */
        public static final DateValueRenderer US  = new DateValueRenderer() {
            /** {@inheritDoc} */
            @Override
            public String getStringRepresentation(final DateValue v) {
                // yyyy/dd/mm
                return v.getYear() + "/" 
                    + TimeRenderUtil.getStringForDateField(v.getDay())
                    + "/" + TimeRenderUtil.getStringForDateField(v.getMonth());
            }
            
            @Override
            public String getDescription() {
                return "2009/30/5";
            }
        };
        
        /** European world presentation of date as dd.mm.yyyy. */
        public static final DateValueRenderer EU  = new DateValueRenderer() {
            /** {@inheritDoc} */
            @Override
            public String getStringRepresentation(final DateValue v) {
                return TimeRenderUtil.getStringForDateField(v.getDay()) 
                    + "." + TimeRenderUtil.getStringForDateField(v.getMonth())
                    + "." + v.getYear();
            }
          
            @Override
            public String getDescription() {
                return "30.05.2009";
            }
        };
        
        /** Standard presentation of a date as yyyy-mm-dd. */
        public static final DateValueRenderer STANDARD 
            = new DateValueRenderer() {
            /** {@inheritDoc} */
            @Override
            public String getStringRepresentation(final DateValue v) {
                return v.getYear() + "-" 
                + TimeRenderUtil.getStringForDateField(v.getMonth()) 
                + "-" + TimeRenderUtil.getStringForDateField(v.getDay());
            }
            
            @Override
            public String getDescription() {
                return "2009-05-30";
            }
        };
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof DateValue) {
            super.setValue(getStringRepresentation((DateValue)value));
        } else {
            super.setValue(value);
        }
    }
    

    
    /**
     * 
     * @param v date value
     * @return the string representation of the date
     */
    public abstract String getStringRepresentation(final DateValue v);
}
