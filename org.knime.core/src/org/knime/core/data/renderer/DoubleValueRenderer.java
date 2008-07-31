/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.data.renderer;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.knime.core.data.DoubleValue;


/**
 * Render to display a double value using a given {@link NumberFormat}.
 * If no number format is given, the full precision is used, i.e. 
 * {@link Double#toString(double)}.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DoubleValueRenderer extends DefaultDataValueRenderer {
    
    /**
     * Singleton for percentage.
     */
    public static final DataValueRenderer PERCENT_RENDERER = 
        new DoubleValueRenderer(
                new DecimalFormat("###0.0#%"), "Percentage");
    
    /**
     * Singleton for ordinary representation.
     */
    public static final DataValueRenderer STANDARD_RENDERER = 
        new DoubleValueRenderer(
                NumberFormat.getNumberInstance(Locale.US), "Standard Double");
    /**
     * Singleton for full precision representation.
     */
    public static final DataValueRenderer FULL_PRECISION_RENDERER = 
        new DoubleValueRenderer(null, "Full Precision");

    /** disable grouping in renderer */
    static {
        NumberFormat.getNumberInstance(Locale.US).setGroupingUsed(false);
    }
    
    /** Used to get a string representation of the double value. */
    private final NumberFormat m_format;
    /** Description to the renderer. */
    private final String m_desc;
    
    /** 
     * Instantiates a new object using a given format.
     * @param format To be used to render this object, may be <code>null</code>
     * to use full precision.
     * @param desc The description to the renderer
     * @throws NullPointerException 
     * If <code>desc</code> argument is <code>null</code>.
     */
    public DoubleValueRenderer(final NumberFormat format, final String desc) {
        if (desc == null) {
            throw new NullPointerException("Description must not be null.");
        }
        m_format = format;
        m_desc = desc;
    }
    
    /**
     * Formats the object. If <code>value</code> is instance of 
     * <code>DoubleValue</code>, the renderer's formatter is used to get a 
     * string from the double value of the cell. Otherwise the 
     * <code>value</code>'s <code>toString()</code> method is used. 
     * @param value The value to be rendered.
     * @see javax.swing.table.DefaultTableCellRenderer#setValue(Object)
     */
    @Override
    protected void setValue(final Object value) {
        Object newValue;
        if (value instanceof DoubleValue) {
            DoubleValue cell = (DoubleValue)value;
            double d = cell.getDoubleValue();
            if (Double.isNaN(d)) {
                newValue = "NaN";
            } else {
                newValue = m_format != null 
                    ? m_format.format(d) : Double.toString(d);
            }
        } else {
            // missing data cells will also end up here
            newValue = value;
        }
        super.setValue(newValue);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return m_desc;
    }
    
}
