/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
