/* Created on 20.02.2007 14:50:41 by thor
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
public class MultiLineStringValueRenderer extends
        DefaultDataValueRenderer {
    private final boolean m_monoSpaceFont;

    private Font m_currentFont;

    private static final int MAX_DEFAULT_HEIGHT = 90;

    /**
     * Instantiates new renderer.
     *
     * @param description description for the renderer shown in the popup menu
     */
    public MultiLineStringValueRenderer(final String description) {
        this(description, true);
    }

    /**
     * Instantiates new renderer.
     *
     * @param description description for the renderer shown in the popup menu
     * @param monoSpaceFont true if a monospace font should be used,
     *            <code>false</code> if the standard font of the parent component
     *            should be used
     * @since 2.7
     */
    public MultiLineStringValueRenderer(final String description, final boolean monoSpaceFont) {
        super(description == null ? "Multi Line String" : description);
        setVerticalAlignment(SwingConstants.TOP);
        m_currentFont =
            new Font("Monospaced", getFont().getStyle(), getFont().getSize());
        m_monoSpaceFont = monoSpaceFont;
        if (m_monoSpaceFont) {
            super.setFont(m_currentFont);
        }
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
        } else if (m_monoSpaceFont) {
            m_currentFont =
                    new Font("Monospaced", font.getStyle(), font.getSize());
            super.setFont(m_currentFont);
        } else {
            super.setFont(font);
        }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getClass().hashCode() ^ getDescription().hashCode();
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
            && getDescription().equals(other.getDescription());
    }
}
