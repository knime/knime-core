/* 
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
 *   12.08.2005 (bernd): created
 */
package de.unikn.knime.core.data.renderer;

import de.unikn.knime.core.data.StringValue;

/**
 * Renderer for DataCells that are compatible with 
 * <code>StringValue</code> classes.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class StringValueRenderer extends DefaultDataValueRenderer {
    
    /** Singleton to be used. */
    public static final StringValueRenderer INSTANCE = 
        new StringValueRenderer(); 

    private StringValueRenderer() {
    }
    
    /**
     * Formats the object. If <code>value</code> is instance of 
     * <code>StringValue</code>, the object's <code>getStringValue</code>
     * is used. Otherwise the fallback: <code>value.toString()</code> 
     * @param value The value to be rendered.
     * @see javax.swing.table.DefaultTableCellRenderer#setValue(Object)
     */
    @Override
    protected void setValue(final Object value) {
        Object newValue;
        if (value instanceof StringValue) {
            StringValue cell = (StringValue)value;
            newValue = cell.getStringValue();
        } else {
            // missing data cells will also end up here
            newValue = value;
        }
        super.setValue(newValue);
    }    
    /**
     * @see DefaultDataValueRenderer#getDescription()
     */
    @Override
    public String getDescription() {
        return "String";
    }
}
