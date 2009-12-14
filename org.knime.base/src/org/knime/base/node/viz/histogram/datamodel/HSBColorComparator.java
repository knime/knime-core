/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
