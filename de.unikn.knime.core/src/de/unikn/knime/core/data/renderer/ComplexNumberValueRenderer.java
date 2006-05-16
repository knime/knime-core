/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   23.03.2006 (cebron): created
 */
package de.unikn.knime.core.data.renderer;

import java.text.NumberFormat;
import java.util.Locale;

import de.unikn.knime.core.data.ComplexNumberValue;

/**
 * Render to display a complex number value using a given
 * <code>NumberFormat</code>.
 * 
 * @see java.text.NumberFormat
 * @author ciobaca, University of Konstanz
 */
public class ComplexNumberValueRenderer extends DefaultDataValueRenderer {

    /**
     * Singleton for ordinary representation.
     */
    public static final DataValueRenderer STANDARD_RENDERER = 
            new ComplexNumberValueRenderer(
                    NumberFormat.getNumberInstance(Locale.US), 
                    "Standard Complex Number");

    /** disable grouping in renderer */
    static {
        NumberFormat.getNumberInstance(Locale.US).setGroupingUsed(false);
    }

    /** Used to get a string representation of the complex number value. */
    private final NumberFormat m_format;

    /** Description to the renderer. */
    private final String m_desc;

    /**
     * Instantiates a new object using a given format.
     * 
     * @param format To be used to render this object.
     * @param desc The description to the renderer
     * @throws NullPointerException If argument is <code>null</code>.
     */
    public ComplexNumberValueRenderer(final NumberFormat format,
            final String desc) {
        if (format == null || desc == null) {
            throw new NullPointerException(
                    "Format/Description must not be null.");
        }
        m_format = format;
        m_desc = desc;
    }

    /**
     * Formats the object. If <code>value</code> is instance of
     * <code>ComplexNumberValue</code>, the renderer's formatter is used to get
     * a string from the complex number value of the cell. Otherwise the
     * <code>value</code>'s <code>toString()</code> method is used.
     * 
     * @param value The value to be rendered.
     * @see javax.swing.table.DefaultTableCellRenderer#setValue(Object)
     */
    protected void setValue(final Object value) {
        Object newValue;
        if (value instanceof ComplexNumberValue) {
            ComplexNumberValue cell = (ComplexNumberValue)value;
            double real = cell.getRealValue();
            double imag = cell.getImaginaryValue();
            if (Double.isNaN(real) || Double.isNaN(imag)) {
                newValue = "NaN";
            } else {
                newValue = m_format.format(real);
                if (imag < 0) {
                    newValue = newValue + " - i*" + Math.abs(imag);
                } else {
                    newValue = newValue + " + i*" + imag;
                }
            }
        } else {
            // missing data cells will also end up here
            newValue = value;
        }
        super.setValue(newValue);
    }

    /**
     * @see DefaultDataValueRenderer#getDescription()
     */
    public String getDescription() {
        return m_desc;
    }
}
