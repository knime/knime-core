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
