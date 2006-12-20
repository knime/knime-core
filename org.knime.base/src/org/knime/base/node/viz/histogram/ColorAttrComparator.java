/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 */

package org.knime.base.node.viz.histogram;

import java.awt.Color;
import java.util.Comparator;

import org.knime.core.data.property.ColorAttr;


/**
 * Comparator comparing two <code>Color</code> objects. With this the color
 * red is smaller than green is smaller than blue is smaller than null. Only the
 * strongest color component (R, G, or B) is used to compare the colors.
 * 
 * @author Peter Ohl, University of Konstanz
 */
class ColorAttrComparator implements Comparator<ColorAttr> {
    private final boolean m_useHilitColor;

    /**
     * creates a new comparator for color attributes.
     * 
     * @param useHilitColor if set <code>true</code> the hilight color will be
     *            used when comparing two colorattr objects, otherwise the
     *            "regular" color is looked at
     */
    ColorAttrComparator(final boolean useHilitColor) {
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
            return +1;
        }
        if (o2 == null) {
            return -1;
        }

        Color c1 = o1.getColor(false, m_useHilitColor);
        Color c2 = o2.getColor(false, m_useHilitColor);

        int strong1;
        int c1Value;
        int strong2;
        int c2Value;

        // figure out which color is the strongest
        if ((c1.getRed() >= c1.getGreen()) && (c1.getRed() >= c1.getBlue())) {
            strong1 = 0; // red has the highest value;
            c1Value = c1.getRed();
        } else if ((c1.getGreen() >= c1.getRed())
                && (c1.getGreen() >= c1.getBlue())) {
            strong1 = 1; // green has the highest value;
            c1Value = c1.getGreen();
        } else {
            strong1 = 2; // it must be blue then.
            c1Value = c1.getBlue();
        }
        // same with c2:
        if ((c2.getRed() >= c2.getGreen()) && (c2.getRed() >= c2.getBlue())) {
            strong2 = 0; // red has the highest value;
            c2Value = c2.getRed();
        } else if ((c2.getGreen() >= c2.getRed())
                && (c2.getGreen() >= c2.getBlue())) {
            strong2 = 1; // green has the highest value;
            c2Value = c2.getGreen();
        } else {
            strong2 = 2; // it must be blue then.
            c2Value = c2.getBlue();
        }

        if (strong1 < strong2) {
            return -1;
        } else if (strong1 > strong2) {
            return +1;
        } else {
            if (c1Value < c2Value) {
                return -1;
            } else if (c1Value > c2Value) {
                return +1;
            } else {
                return 0;
            }
        }
    }
}
