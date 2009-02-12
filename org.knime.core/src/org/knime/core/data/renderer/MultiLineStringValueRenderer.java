/* Created on 20.02.2007 14:50:41 by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.core.data.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.SwingConstants;

/**
 * This class renders strings that consist of more than one line.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public final class MultiLineStringValueRenderer extends
        DefaultDataValueRenderer {
    private Font m_currentFont;

    private final String m_description;

    private static final int MAX_DEFAULT_HEIGHT = 90;
    
    /**
     * Instantiates new renderer.
     * 
     * @param description description for the renderer shown in the popup menu
     */
    public MultiLineStringValueRenderer(final String description) {
        m_description = description == null ? "Multi Line String" : description;
        setVerticalAlignment(SwingConstants.TOP);
        m_currentFont = 
            new Font("Monospaced", getFont().getStyle(), getFont().getSize());
        super.setFont(m_currentFont);
        setBackground(Color.WHITE);
        setUI(new MultiLineBasicLabelUI());
    }

    /**
     * Sets the string object for the cell being rendered.
     * 
     * @param value the string value for this cell; if value is
     *            <code>null</code> it sets the text value to an empty string
     * @see javax.swing.JLabel#setText
     * 
     */
    @Override
    protected void setValue(final Object value) {
        if (value != null) {
            super.setValue(value.toString());
        } else {
            super.setValue("?");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return m_description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        
        if (dim.height > MAX_DEFAULT_HEIGHT) {
            dim.height = MAX_DEFAULT_HEIGHT;
        }
        dim.width += 5; // small offset to avoid "..."
        return dim;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFont(final Font font) {
        // DefaultTableCellRenderer sets the font upon each paint to the
        // font of the JTable; we do not want this here so we overwrite it
        if (font == null) {
            super.setFont(m_currentFont);
        } else if (font.equals(m_currentFont)) {
            return;
        } else {
            m_currentFont =
                    new Font("Monospaced", font.getStyle(), font.getSize());
            super.setFont(m_currentFont);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getClass().hashCode() ^ m_description.hashCode();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MultiLineStringValueRenderer)) {
            return false;
        }
        MultiLineStringValueRenderer other = (MultiLineStringValueRenderer)obj;
        return other.getClass().equals(getClass()) 
            && m_description.equals(other.m_description);
    }
}
