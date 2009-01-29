/* 
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.data.property;

import java.awt.Color;
import java.util.HashMap;

/**
 * This class holds a <code>Color</code> value as property for view objects and
 * supports colors for selection, hilite, selection-hilite, border, and 
 * background. A <code>ColorAttr</code> is only created once for each color.
 * 
 * @see java.awt.Color
 * 
 * @author Thomas Gabriel, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
public final class ColorAttr {
    
    /** The default color attribute value, used when no color attribute set. */
    public static final ColorAttr DEFAULT;

    /** This attribute's color. */
    private final Color m_attrColor;
    
    /** This attribute's color for selection. */
    private final Color m_attrColorSelected;
    
    /** This attribute's color for hilite. */
    private final Color m_attrColorHilite;
    
    /** This attribute's color for selection and hilite. */
    private final Color m_attrColorSelectedHilite;
    
    /**
     * The color for selection as <code>new Color(179, 168, 143)</code>.
     */
    public static final Color SELECTED = new Color(179, 168, 143); 
    
    /**
     * The color for hilite as <code>new Color(255, 181, 0)</code>.
     */
    public static final Color HILITE = new Color(255, 181, 0);
    
    /**
     * The color for selection as <code>new Color(255, 240, 204)</code>.
     */
    public static final Color SELECTED_HILITE = new Color(255, 240, 204);   
    
    /**
     * The color for inactive points, "grayed out", as 
     * <code>Color.LIGHT_GRAY</code>.
     */
    public static final Color INACTIVE = Color.LIGHT_GRAY;
    
    /**
     * The color for inactive points, "grayed out", but selected as 
     * <code>Color.GRAY</code>.
     */
    public static final Color INACTIVE_SELECTED = Color.GRAY;
    
    /**
     * The color for border as <code>Color.DARK_GRAY</code>.
     */
    public static final Color BORDER = Color.DARK_GRAY;
    
    /**
     * The color for background as <code>Color.WHITE</code>.
     */
    public static final Color BACKGROUND = Color.WHITE;
    
    /**
     * A map of all instanciated <code>Color</code> objects to 
     * <code>ColorAttr</code> values to prevent multiple objects for the same
     * color.
     */
    private static final HashMap<Color, ColorAttr> COLORATTRS;
    
    /**
     * Inits the default color attribute and adds it to the <code>Color</code> 
     * to <code>ColorAttr</code> map.
     */
    static {
        COLORATTRS = new HashMap<Color, ColorAttr>();
        DEFAULT = new ColorAttr();
        COLORATTRS.put(DEFAULT.getColor(), DEFAULT);
    }  
    
    /**
     * Creates the default color attribute using default color settings.
     */
    private ColorAttr() {
        m_attrColor = Color.DARK_GRAY;
        m_attrColorSelected = SELECTED;
        m_attrColorSelectedHilite = SELECTED_HILITE;
        m_attrColorHilite = HILITE;
    }
    
    /**
     * Creates a new color attribute with the given <code>color</code>
     * as attribute value, hilite, selected, selected and hilite color.
     * @param color the color for this color attribute
     */
    private ColorAttr(final Color color) {
        assert (color != null);
        m_attrColor = color;
        m_attrColorHilite = color;
        m_attrColorSelected = color;
        m_attrColorSelectedHilite = color;
    }

    /**
     * Creates a new color attribute with the given color.
     * @param color the color for this object
     * @return the <code>ColorAttr</code> object for the given 
     *         <code>Color</code>
     * @throws IllegalArgumentException if the <code>Color</code> is 
     *         <code>null</code>
     */
    public static ColorAttr getInstance(final Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Color can't be null.");
        }
        ColorAttr ca = COLORATTRS.get(color);
        if (ca == null) {
            ca = new ColorAttr(color);
            COLORATTRS.put(color, ca);
        }
        return ca;
    }
    
    /**
     * Returns this attribute's color value.
     * @return color of this attribute
     * 
     * @see #getColor(boolean, boolean)
     */
    public Color getColor() {
        return m_attrColor;
    }

    /**
     * Returns the color value for this object under certain constrains.
     * @param selected if selected property is set
     * @param hilite if hilite property is set     
     * @return the color for this object under the given constrains
     * @see #getColor()
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
            } else {
                return m_attrColor;
            }
        }
    }
    
    /**
     * Returns the border color for this object under certain constrains.
     * @param selected if the border is selected
     * @param hilite if the border is hilite
     * @return the color for this objects's border under the given constrains
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
            } else {
                return BORDER;
            }
        }
    }
    
    /**
     * Returns the preferred background color.
     * @return preferred background color
     */
    public static Color getBackground() {
        return BACKGROUND;
    }
    
    /**
     * Returns the preferred color for inactive points.
     * @return preferred inactive color
     */
    public static Color getInactiveColor() {
        return INACTIVE;
    }
    
    /**
     * Compares this <code>ColorAttr</code> with the given one and returns 
     * <code>true</code> if both have the same color value.
     * @param ca the other <code>ColorAttr</code> to compare this one with
     * @return <code>true</code> if the color values are equal otherwise
     *         <code>false</code>
     * 
     * @see Color#equals(java.lang.Object)
     */
    public boolean equals(final ColorAttr ca) {
        return ca != null && m_attrColor.equals(ca.getColor());
    }
    
    /**
     * Compares this <code>ColorAttr</code> with the given 
     * <code>Object</code> and returns 
     * <code>true</code> if the other is an instance of <code>ColorAttr</code>
     * and both have the same color value.
     * @param obj the other <code>ColorAttr</code> to compare this one with
     * @return <code>true</code> if the color values are equal otherwise
     *         <code>false</code>
     * 
     * @see #equals(ColorAttr)
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof ColorAttr && equals(((ColorAttr) obj));
    }
    
    /**
     * Hash code of the underlying color value.
     * @see Color#hashCode()
     * @return the color value's hash code 
     */
    @Override
    public int hashCode() {
        return m_attrColor.hashCode();
    }
    
    /**
     * A String representation for this color attribute including the 
     * simple class name, attribute color, hilite, selected, selected-hilite, 
     * border, and background color.
     * @return a String representation for this color attribute
     * 
     * @see Color#toString()
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName() + ":");
        buf.append("color="           + m_attrColor.toString() + ",");
        buf.append("hilite="          + HILITE.toString() + ",");
        buf.append("selected="        + SELECTED.toString() + ",");
        buf.append("selected-hilite=" + SELECTED_HILITE.toString() + ",");
        buf.append("border="          + SELECTED_HILITE.toString() + ",");
        buf.append("background="      + BACKGROUND.toString());
        return buf.toString(); 
    }
    
}
