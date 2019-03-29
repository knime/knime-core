/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 28, 2019 (loki): created
 */
package org.knime.core.util;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

/**
 * A class of utilities that operate on Color (usually SWT) instances.
 *
 * @author loki der quaeler
 * @since 3.8
 */
public class ColorUtilities {
    /**
     * Returns an integer with 24bit color info. Bits 23 to 16 contain the red value, 15 to 8 contain the green value,
     * and 7 to 0 contain the blue.
     *
     * @param c the color to translate
     * @return an integer with 24bit color info
     */
    public static int colorToRGBint(final Color c) {
        return ((c.getRed() & 0x0FF) << 16) | ((c.getGreen() & 0x0FF) << 8) | ((c.getBlue() & 0x0FF));
    }

    /**
     * Returns the red, green, blue value of the specified 24bit int as separate values in a new object.
     *
     * @param rgb the 24bit color integer to convert
     * @return a new RGB object with separated rgb values
     */
    public static RGB RGBintToRGBObj(final int rgb) {
        return new RGB((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * Returns a new Color for the passed rgb values.
     *
     * @param rgb
     * @return a new Color for the passed RGB object
     */
    public static Color RGBintToColor(final int rgb) {
        return RGBtoColor(RGBintToRGBObj(rgb));
    }

    /**
     * Returns a new Color for the passed RGB object.
     *
     * @param rgb
     * @return a new Color for the passed RGB object
     */
    public static Color RGBtoColor(final RGB rgb) {
        return new Color(null, rgb);
    }

    /**
     * Changes the color to its CIE 1931 linear luminance grayscale representation; it also does some nudging on
     * already monochromatic colors, nudging the value towards the center (so if it's black - go to dark gray; if
     * it's white, go to light gray)
     *
     * @param c a presumed non-gray color.
     * @param alpha a 0-255 value representing the opacity (255 == opaque)
     * @return the grayscale representation of the passed value.
     */
    public static Color convertToGrayscale(final Color c, final int alpha) {
        if ((c.getRed() == c.getGreen()) && (c.getGreen() == c.getBlue())) {
            final int delta = 12 * ((c.getRed() > 127) ? -2 : 10);
            int kInt = c.getRed() + delta;

            if (kInt < 60) {
                kInt = 60;
            } else if (kInt > 190) {
                kInt = 190;
            }

            return new Color(null, kInt, kInt, kInt, alpha);
        } else {
            final double y = (0.2126 * c.getRed()) + (0.7152 * c.getGreen()) + (0.0722 * c.getBlue());
            final int kInt = (int)y;

            return new Color(null, kInt, kInt, kInt, alpha);
        }
    }

    /**
     * This reduces the saturation and increases the lightness of the argument <code>Color</code> returning a new
     * instance of <code>Color</code> containing the altered content.
     *
     * @param c the color to lighten up
     * @return a new color derived from the argument, or the original color if a color processing exception occurs
     */
    public static Color lightenColor(final Color c) {
        try {
            final float[] hsb = new float[3];
            java.awt.Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);

            // bring down the saturation
            hsb[1] = Math.max(0, hsb[1] * 0.5f);

            // push up the lightness
            hsb[2] = Math.min(1.0f, hsb[2] + ((1.0f - hsb[2]) * 0.5f));

            final int rgb = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
            return new Color(null, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (Throwable t) {
            return c;
        }
    }

    /**
     * Given a hex string <b>without a CSS-style prefixed "#"</b>, return an <code>RGB</code> instance representing it;
     * another very low hanging fruit that SWT is too crappy to provide itself.
     *
     * @param hex a #-less string, e.g "CDE280"
     * @return an instance of <code>RGB</code>
     */
    public static RGB fromHex(final String hex) {
        final int rgb = Integer.parseInt(hex, 16);

        return new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * "Blend," in a manner of speaking, two <code>RGB</code> instances based on a specified ratio. This takes each
     * color component and weights the component of the first by <code>ratio</code> amount and the second by
     * <code>(100 - ratio)</code> amount.
     *
     * @param c1
     * @param c2
     * @param ratio a value between 0 and 100 representing the percentage of <code>c1</code> in the blend
     * @return an instance of <code>RGB</code> which is the
     */
    public static RGB blend(final RGB c1, final RGB c2, final int ratio) {
        return new RGB(blend(c1.red, c2.red, ratio), blend(c1.green, c2.green, ratio), blend(c1.blue, c2.blue, ratio));
    }

    //
    // Private methods
    //

    private static int blend(final int v1, final int v2, final int ratio) {
        final int blend = ((ratio * v1) + ((100 - ratio) * v2)) / 100;
        return Math.max(0, Math.min(255, blend));
    }
}
