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
 */
package de.unikn.knime.core.data.renderer;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import de.unikn.knime.core.data.DataColumnSpec;

/**
 * Default renderer to be used as to render a <code>DataCell</code>. It will 
 * simply use the <code>DataCell</code>'s <code>toString()</code> method and 
 * display this String. 
 *  
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DefaultDataCellRenderer 
    extends DefaultTableCellRenderer implements DataCellRenderer {
    
    /** The spec to the column for which this renderer is being used. */
    private final DataColumnSpec m_colSpec;
    
    /** Creates new instance given a null column spec. */
    public DefaultDataCellRenderer() {
        this(null);
    }
    
    /**
     * Creates new renderer and memorizes the column spec. The argument may
     * be, however null.
     * @param spec The column spec of the column for which this renderer is 
     * used. 
     */
    public DefaultDataCellRenderer(final DataColumnSpec spec) {
        m_colSpec = spec;
    }
    
    /**
     * @see DataCellRenderer#getDescription()
     */
    public String getDescription() {
        return "Default";
    }
    
    /**
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent(
     * javax.swing.JList, java.lang.Object, int, boolean, boolean)
     */
    public Component getListCellRendererComponent(
            final JList list, final Object value, final int index, 
            final boolean isSelected, final boolean cellHasFocus) {
        /* Copied almost all code from DefaultListCellRenderer */
        setComponentOrientation(list.getComponentOrientation());
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setValue(value);
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setBorder((cellHasFocus) 
                ? UIManager.getBorder("List.focusCellHighlightBorder") 
                : noFocusBorder);
        return this;
    }
    
    /**
     * Get reference to the constructor's argument. The return value may be
     * null (in particular if the empty constructor has been used).
     * @return The column spec for this renderer.
     */
    protected DataColumnSpec getColSpec() {
        return m_colSpec;
    }
    
}
