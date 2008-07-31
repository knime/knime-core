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

import java.awt.Component;

import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import org.knime.core.data.DataColumnSpec;


/**
 * Default implementation for a renderer for 
 * {@link org.knime.core.data.DataValue} objects. This class should be used
 * (better: derived from) when the rendering is only a string representation
 * of the <code>DataValue</code> object. It's recommended to derive this class
 * and overwrite the {@link DefaultTableCellRenderer#setValue(Object)} and
 * the {@link #getDescription()} methods. A correct implementation of 
 * <code>setValue(Object)</code> will test if the argument object is of the 
 * expected <code>DataValue</code> class and call 
 * <code>super.setValue(Object)</code> with the desired string representation.
 *   
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DefaultDataValueRenderer 
    extends DefaultTableCellRenderer implements DataValueRenderer {
    
    /** The spec to the column for which this renderer is being used. */
    private final DataColumnSpec m_colSpec;
    
    /** Creates new instance given a null column spec. */
    public DefaultDataValueRenderer() {
        this(null);
    }
    
    /**
     * Creates new renderer and memorizes the column spec. The argument may
     * be, however, null.
     * @param spec The column spec of the column for which this renderer is 
     * used. 
     */
    public DefaultDataValueRenderer(final DataColumnSpec spec) {
        m_colSpec = spec;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return "Default";
    }
    
    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public Component getRendererComponent(final Object val) {
        setValue(val);
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

    /**
     * Returns always <code>true</code>.
     * {@inheritDoc}
     */
    public boolean accepts(final DataColumnSpec spec) {
        return true;
    }
    
}
