/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.data.property;

import java.awt.Color;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Color attribute class holding properties for view objects such as selection, 
 * hilite, selection-hilite, and border color.
 * There is only one instance for each <code>Color</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ColorAttr implements Serializable {
    
    /**
     * The default color.
     */
    public static final ColorAttr DEFAULT;

    /**
     * The object's color.
     */
    private final Color m_attrColor;
    
    /*
     * Color variations for different settings (selection/hiliting)
     */
    private final Color m_attrColorSelected;
    private final Color m_attrColorHilite;
    private final Color m_attrColorSelectedHilite;
    
    /**
     * The color for selection: some kind of light blue.
     */
    public static final Color SELECTED = new Color(204, 204, 255);
    
    /**
     * The color for hilite: yellow.
     */
    public static final Color HILITE = Color.YELLOW;
    
    /**
     * The color for hilite and selection: pink.
     */
    public static final Color SELECTED_HILITE = Color.PINK;
    
    /**
     * The color for inactive points ("grayed out"): light gray.
     */
    public static final Color INACTIVE = Color.LIGHT_GRAY;
    
    /**
     * The color of the border: dark gray.
     */
    public static final Color BORDER = Color.DARK_GRAY;
    
    /**
     * The preferred background color for the panel in which data is displayed.
     */
    public static final Color BACKGROUND = Color.WHITE;
    
    /**
     * Keeps a list of all instanciated ColorAttr with its Color as key.
     * Prevent from using multiple instances with the same color.
     */
    private static final HashMap<Color, ColorAttr> COLORATTRS;
    
    /*
     * Inits Color to ColorAttr map and the default ColorAttr which is also
     * added to this map.
     */
    static {
        COLORATTRS = new HashMap<Color, ColorAttr>();
        Color c = Color.DARK_GRAY;
        DEFAULT = new ColorAttr(c);
        COLORATTRS.put(c, DEFAULT);
    }    
    
    /*
     * Creates a new color attribute objects with its initial color.
     * @param attColor The color for this object.
     */
    private ColorAttr(final Color attColor) {
        assert (attColor != null);
        // create color variations for HiLite and/or Selected values
        float[] hsbVals = new float[3];
        Color.RGBtoHSB(attColor.getRed(), attColor.getGreen(),
                attColor.getBlue(), hsbVals);
        m_attrColor = attColor;
        m_attrColorSelected =
            Color.getColor(null, Color.HSBtoRGB(hsbVals[0], 0.6f, 0.6f));
        m_attrColorHilite = 
            Color.getColor(null, Color.HSBtoRGB(hsbVals[0] + 0.1f, 1.0f, 1.0f));
        m_attrColorSelectedHilite =
            Color.getColor(null, Color.HSBtoRGB(hsbVals[0] + 0.1f, 0.6f, 0.6f));
    }

    /**
     * Creates a new color attribute objects with its initial color.
     * @param c The color for this object.
     * @return The <code>ColorAttr</code> object for the given Color.
     * @throws IllegalArgumentException If the Color is <code>null</code>.
     */
    public static ColorAttr getInstance(final Color c) {
        if (c == null) {
            throw new IllegalArgumentException("Color can't be null.");
        }
        ColorAttr ca = COLORATTRS.get(c);
        if (ca == null) {
            ca = new ColorAttr(c);
            COLORATTRS.put(c, ca);
        }
        return ca;
    }
    
    /**
     * <code>getColor(false,false)</code>.
     * @return The color for this object.
     */
    public Color getColor() {
        return m_attrColor;
    }

    /**
     * Returns the color for this object under certain constrains.
     * @param selected If the border is selected.
     * @param hilite If the border is hilite.     
     * @return The color for this object under the given constrains.
     */
    public Color getColor(final boolean selected, final boolean hilite) {
        if (selected) {
            if (hilite) {
                return m_attrColorSelectedHilite;
            } else {
                return m_attrColorSelected;
            }
        } else {
            if (hilite) {
                return m_attrColorHilite;
            }
        }
        return m_attrColor;
    }
    
    /**
     * Returns the border color for this object under certain constrains.
     * @param selected If the border is selected.
     * @param hilite If the border is hilite.
     * @return The color for this objects's border under the given constrains.
     */
    public Color getBorderColor(final boolean selected, final boolean hilite) {
        if (selected) {
            if (hilite) {
                return SELECTED_HILITE;
            } else {
                return SELECTED;
            }
        } else {
            if (hilite) {
                return HILITE;
            }
        }
        return BORDER;
    }
    
    /**
     * @return preferred background color
     */
    public static Color getBackground() {
        return BACKGROUND;
    }
    
    /**
     * @return preferred inactive color
     */
    public static Color getInactiveColor() {
        return INACTIVE;
    }
    
    /**
     * @param catt The other <code>ColorAttr</code>.
     * @return <code>true</code> if the colors are equal.
     * 
     * @see Color#equals(java.lang.Object)
     */
    public boolean equals(final ColorAttr catt) {
        return m_attrColor.equals(catt.getColor());
    }
    
    /**
     * @param obj The other <code>ColorAttr</code>.
     * @return <code>true</code> if the colors are equal.
     * 
     * @see #equals(ColorAttr)
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof ColorAttr && equals(((ColorAttr) obj));
    }
    
    /**
     * @see Color#hashCode()
     * @return The object's color value.
     */
    @Override
    public int hashCode() {
        return m_attrColor.hashCode();
    }
    
    /**
     * A String representation for this color attribute including the 
     * attribute, hilite, selected, selected-hilite, and border color.
     * @return A String representation for this color properties.
     * 
     * @see Color#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer(getClass().getName());
        buf.append("attribute:"       + m_attrColor.toString());
        buf.append("hilite:"          + HILITE.toString());
        buf.append("selected:"        + SELECTED.toString());
        buf.append("selected-hilite:" + SELECTED_HILITE.toString());
        buf.append("border:"          + SELECTED_HILITE.toString());
        buf.append("background:"      + BACKGROUND.toString());
        return buf.toString(); 
    }
    
}   // ColorAttr
