/*
 * ------------------------------------------------------------------
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 */

package org.knime.base.node.viz.histogram;

import java.awt.Color;
import java.util.Comparator;

import org.knime.core.data.property.ColorAttr;


/**
 * Comparator comparing two <code>Color</code> objects using the HSB color 
 * space. It compares first the hue than the saturation and at the end the
 * brightness.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
class HSBColorAttrComparator implements Comparator<ColorAttr> {
    private final boolean m_useHilitColor;
    private static final int HUE_IDX = 0;
    private static final int SATURATION_IDX = 1;
    private static final int BRIGHTNESS_IDX = 2;
    //used to set the split of the hue circle in the blue area
    private static final float HUE_SPLITT = 0.5f;

    /**
     * creates a new comparator for color attributes.
     * 
     * @param useHilitColor if set <code>true</code> the hilight color will be
     *            used when comparing two colorattr objects, otherwise the
     *            "regular" color is looked at
     */
    HSBColorAttrComparator(final boolean useHilitColor) {
        m_useHilitColor = useHilitColor;
    }

    /**
     * @see java.util.Comparator#compare
     */
    public int compare(final ColorAttr o1, final ColorAttr o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return +1;
        }

        Color c1 = o1.getColor(false, m_useHilitColor);
        Color c2 = o2.getColor(false, m_useHilitColor);

        float[] hsbvals = new float[3];
        hsbvals = 
            Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), hsbvals);
        //we don't need to make modulo 1 because java takes care that the value
        //is below 1
        float hue1 = hsbvals[HUE_IDX] + HUE_SPLITT;
        float sat1 = hsbvals[SATURATION_IDX];
        float bright1 = hsbvals[BRIGHTNESS_IDX];
        
        hsbvals = 
            Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), hsbvals);
        //we don't need to make modulo 1 because java takes care that the value
        //is below 1        
        float hue2 = hsbvals[HUE_IDX] + HUE_SPLITT;
        float sat2 = hsbvals[SATURATION_IDX];
        float bright2 = hsbvals[BRIGHTNESS_IDX];
        
        if (hue1 > hue2) {
            return +1;
        } else if (hue1 < hue2) {
            return -1;
        } else {
            if (sat1 > sat2) {
                return +1;
            } else if (sat1 < sat2) {
                return -1;
            } else {
                if (bright1 > bright2) {
                    return +1;
                } else if (bright1 < bright2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
    }
}
