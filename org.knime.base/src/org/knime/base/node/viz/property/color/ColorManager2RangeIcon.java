/* 
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
 * -------------------------------------------------------------------
 * 
 * History
 *   09.02.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import org.knime.core.data.property.ColorAttr;


/**
 * An icon which background is painted in colors which are linear interpolated
 * between the two borders.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColorManager2RangeIcon extends JPanel {
    
    private Color m_min;

    private Color m_max;

    /**
     * Creates a new icon with default colors.
     * 
     * @see ColorAttr#DEFAULT
     */
    public ColorManager2RangeIcon() {
        this(ColorAttr.DEFAULT.getColor(), ColorAttr.DEFAULT.getColor());
    }

    /**
     * Creates a new icon with the given colors.
     * 
     * @param min the left color
     * @param max the right color
     * @throws NullPointerException if on the colors is <code>null</code>
     */
    public ColorManager2RangeIcon(final Color min, final Color max) {
        super(null);
        super.setPreferredSize(new Dimension(super.getWidth(), 15));
        if (min == null || max == null) {
            throw new NullPointerException();
        }
        m_min = min;
        m_max = max;
    }

    /**
     * Sets a new minimum color and triggers a repaint.
     * 
     * @param min the left color
     */
    public void setMinColor(final Color min) {
        m_min = min;
        super.validate();
        super.repaint();
    }
    
    /**
     * @return current minimum color of this range icon
     */
    public Color getMinColor() {
        return m_min;
    }

    /**
     * Sets a new maximum color and triggers a repaint.
     * 
     * @param max the right color
     */
    public void setMaxColor(final Color max) {
        m_max = max;
        super.validate();
        super.repaint();
    }
    
    /**
     * @return current maximum color of this range icon
     */
    public Color getMaxColor() {
        return m_max;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(final Graphics gx) {
        super.paintComponent(gx);
        int width = super.getWidth();
        int height = super.getHeight();
        Graphics2D gx2 = (Graphics2D)gx;
        gx2.setPaint(new GradientPaint(0, 0, m_min, width, 0, m_max));
        gx2.fillRect(0, 0, width, height);
    }
}
