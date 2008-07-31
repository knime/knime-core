/*
 * ------------------------------------------------------------------
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
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.io.Serializable;
import java.util.Comparator;


/**
 * Comparator comparing two <code>Color</code> objects using the HSB color 
 * space. It compares first the hue than the saturation and at the end the
 * brightness.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public final class HSBColorComparator implements Comparator<Color>, 
Serializable {
    
    private static final long serialVersionUID = -2017351242977651036L;
    
    private static final int HUE_IDX = 0;
    private static final int SATURATION_IDX = 1;
    private static final int BRIGHTNESS_IDX = 2;
    //used to set the split of the hue circle in the blue area
    private static final float HUE_SPLITT = 0.5f;
    
    private static HSBColorComparator instance;
    
    private HSBColorComparator() {
        //nothing to do
    }

    /**
     * @return the only instance of this comparator
     */
    public static HSBColorComparator getInstance() {
        if (instance == null) {
            instance = new HSBColorComparator();
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    public int compare(final Color o1, final Color o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return +1;
        }

        float[] hsbvals = new float[3];
        hsbvals = 
            Color.RGBtoHSB(o1.getRed(), o1.getGreen(), o1.getBlue(), hsbvals);
        //we don't need to make modulo 1 because java takes care that the value
        //is below 1
        final float hue1 = hsbvals[HUE_IDX] + HUE_SPLITT;
        final float sat1 = hsbvals[SATURATION_IDX];
        final float bright1 = hsbvals[BRIGHTNESS_IDX];
        
        hsbvals = 
            Color.RGBtoHSB(o2.getRed(), o2.getGreen(), o2.getBlue(), hsbvals);
        //we don't need to make modulo 1 because java takes care that the value
        //is below 1        
        final float hue2 = hsbvals[HUE_IDX] + HUE_SPLITT;
        final float sat2 = hsbvals[SATURATION_IDX];
        final float bright2 = hsbvals[BRIGHTNESS_IDX];
        
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
