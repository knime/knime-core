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
public class DateTimeValueRenderer extends DefaultDataValueRenderer {

    private final DateValueRenderer m_date;
    
    private final TimeValueRenderer m_time;
    
    /**
     * A combination of a {@link DateValueRenderer} and a 
     *  {@link TimeValueRenderer}.
     *  
     * @param dateRenderer renders the date
     * @param timeRenderer renders the time
     */
    public DateTimeValueRenderer(final DateValueRenderer dateRenderer,
            final TimeValueRenderer timeRenderer) {
        m_date = dateRenderer;
        m_time = timeRenderer;
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof DateTimeValue) {
            String dateTime = m_date.getStringRepresentation((DateValue)value)
                + " " + m_time.getStringRepresentation((TimeValue)value);
            super.setValue(dateTime);
        } else {
            super.setValue(value);
        }
    }
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return m_date.getDescription() + " " + m_time.getDescription();
    }
    
}
