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
 *   5 Dec 2025 (Robin Gerling): created
 */
package org.knime.core.data.property;

import java.awt.Color;
import java.util.Arrays;

/**
 * Utility class for color space conversions between the sRGB, Hex, and CIELab color spaces. Transformations are based
 * on https://observablehq.com/@mbostock/lab-and-rgb.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
final class ColorSpaceConversionUtil {

    private ColorSpaceConversionUtil() {
        // utility
    }

    private static final double[][] LRGB_CIEXYZ = new double[][]{ //
        {0.4360747, 0.3850649, 0.1430804}, //
        {0.2225045, 0.7168786, 0.0606169}, //
        {0.0139322, 0.0971045, 0.7141733}};

    private static final double[] TRISTIMULUS_D50 =
        Arrays.stream(LRGB_CIEXYZ).mapToDouble(arr -> Arrays.stream(arr).sum()).toArray();

    private static final double CIELAB_DELTA = (6.0 / 29.0);

    private static final double[][] CIEXYZ_LRGB = new double[][]{ //
        {3.1338561, -1.6168667, -0.4906146}, //
        {-0.9787684, 1.9161415, 0.0334540}, //
        {0.0719453, -0.2289914, 1.4052427}};

    /**
     * Handles sRGB colors with 3 (RGB) or 4 (RGBA) components by adding a default alpha of 1. Values in range 0-1, not
     * 0-255.
     */
    static double[][] convertSRGBColorsToCIELab(final double[][] sRGBColors) {
        return Arrays.stream(sRGBColors).map(ColorSpaceConversionUtil::convertSRGBColorToCIELab)
            .toArray(double[][]::new);
    }

    static double[][] convertHexColorsToCIELab(final String[] hexColors) {
        return Arrays.stream(hexColors).map(ColorSpaceConversionUtil::convertHexColorToCIELab).toArray(double[][]::new);
    }

    static double[][] convertJavaColorsToCIELab(final Color[] colors) {
        return Arrays.stream(colors).map(ColorSpaceConversionUtil::convertColorToCIELab).toArray(double[][]::new);
    }

    static Color convertCIELabToJavaColor(final double[] cieLab) {
        final var rgbColor = ColorSpaceConversionUtil.convertCIELabColorToSRGB(cieLab);
        final var color = ColorSpaceConversionUtil.clampToByte(rgbColor);
        return new Color(color[0], color[1], color[2], color[3]);
    }

    static String convertCIELabColorToHexString(final double[] cieLab) {
        final var color = ColorSpaceConversionUtil.convertCIELabToJavaColor(cieLab);
        return convertJavaColorToHexWithAlpha(color);
    }

    static double[] convertHexColorToCIELab(final String hex) {
        final var color = convertHexToJavaColor(hex);
        return convertColorToCIELab(color);
    }

    private static Color convertHexToJavaColor(final String hexWithPossibleHash) {
        final var hex = hexWithPossibleHash.startsWith("#") ? hexWithPossibleHash.substring(1) : hexWithPossibleHash;
        return switch (hex.length()) {
            case 6 -> new Color(Integer.parseInt(hex, 16));
            case 8 -> new Color ((int) Long.parseLong(hex, 16), true);
            default -> throw new IllegalArgumentException(
                String.format("Hex color must consist of 6 or 8 digits but had %d: %s",
                    hex.length(), hex));
        };
    }

    private static String convertJavaColorToHexWithAlpha(final Color color) {
        return String.format("#%08X", color.getRGB());
    }

    private static double[] convertSRGBColorToCIELab(final double[] sRGB) {
        final var sRGBA = createSRGBA(sRGB);
        final var linearRGB = sRGBtoLinearRGB(sRGBA);
        final var ciexyz = convertLinearRGBtoCIEXYZ50(linearRGB);
        return convertCIEXYZ50toCIELAB(ciexyz);
    }

    private static double[] convertCIELabColorToSRGB(final double[] lab) {
        final var ciexyz = convertCIELABtoCIEXYZ50(lab);
        final var linearRGB = convertCIEXYZ50toLinearRGB(ciexyz);
        return linearRGBtoSRGB(linearRGB);
    }

    private static double[] createSRGBA(final double[] sRGBColor) {
        return switch (sRGBColor.length) {
            case 4 -> sRGBColor;
            case 3 -> new double[]{sRGBColor[0], sRGBColor[1], sRGBColor[2], 1.0};
            default -> throw new IllegalArgumentException("Expected 3 or 4 components but got " + sRGBColor.length);
        };
    }

    private static double[] sRGBtoLinearRGB(final double[] sRGBA) {
        return new double[]{inverseCompanding(sRGBA[0]), inverseCompanding(sRGBA[1]), inverseCompanding(sRGBA[2]),
            sRGBA[3]};
    }

    private static double[] linearRGBtoSRGB(final double[] lRGBA) {
        return new double[]{companding(lRGBA[0]), companding(lRGBA[1]), companding(lRGBA[2]), lRGBA[3]};
    }

    private static double inverseCompanding(final double sRGBComponent) {
        return sRGBComponent <= 0.04045 //
            ? (sRGBComponent / 12.92) //
            : Math.pow(((sRGBComponent + 0.055) / 1.055), 2.4);
    }

    private static double companding(final double lRGBComponent) {
        return lRGBComponent <= 0.0031308 //
            ? (12.92 * lRGBComponent) //
            : (1.055 * Math.pow(lRGBComponent, 1.0 / 2.4) - 0.055);
    }

    private static double[] convertLinearRGBtoCIEXYZ50(final double[] lRGB) {
        return new double[]{ //
            lRGB[0] * LRGB_CIEXYZ[0][0] + lRGB[1] * LRGB_CIEXYZ[0][1] + lRGB[2] * LRGB_CIEXYZ[0][2],
            lRGB[0] * LRGB_CIEXYZ[1][0] + lRGB[1] * LRGB_CIEXYZ[1][1] + lRGB[2] * LRGB_CIEXYZ[1][2],
            lRGB[0] * LRGB_CIEXYZ[2][0] + lRGB[1] * LRGB_CIEXYZ[2][1] + lRGB[2] * LRGB_CIEXYZ[2][2], lRGB[3]};
    }

    private static double computeCIELabF(final double t) {
        return t > Math.pow(CIELAB_DELTA, 3) //
            ? Math.cbrt(t) //
            : (t / (3 * Math.pow(CIELAB_DELTA, 2)) + (4.0 / 29.0));
    }

    private static double[] convertCIEXYZ50toCIELAB(final double[] xyz) {
        final var fx = computeCIELabF(xyz[0] / TRISTIMULUS_D50[0]);
        final var fy = computeCIELabF(xyz[1] / TRISTIMULUS_D50[1]);
        final var fz = computeCIELabF(xyz[2] / TRISTIMULUS_D50[2]);
        return new double[]{//
            116 * fy - 16, //
            500 * (fx - fy), //
            200 * (fy - fz), //
            xyz[3]};
    }

    private static double computeInverseCIELabF(final double t) {
        return t > CIELAB_DELTA //
            ? Math.pow(t, 3) //
            : (3 * Math.pow(CIELAB_DELTA, 2) * (t - (4.0 / 29.0)));
    }

    private static double[] convertCIELABtoCIEXYZ50(final double[] lab) {
        final var fl = (lab[0] + 16) / 116.0;
        final var fa = lab[1] / 500.0;
        final var fb = lab[2] / 200.0;
        return new double[]{//
            TRISTIMULUS_D50[0] * computeInverseCIELabF(fl + fa), //
            TRISTIMULUS_D50[1] * computeInverseCIELabF(fl), //
            TRISTIMULUS_D50[2] * computeInverseCIELabF(fl - fb), //
            lab[3]};
    }

    private static double[] convertCIEXYZ50toLinearRGB(final double[] xyz) {
        return new double[]{ //
            xyz[0] * CIEXYZ_LRGB[0][0] + xyz[1] * CIEXYZ_LRGB[0][1] + xyz[2] * CIEXYZ_LRGB[0][2],
            xyz[0] * CIEXYZ_LRGB[1][0] + xyz[1] * CIEXYZ_LRGB[1][1] + xyz[2] * CIEXYZ_LRGB[1][2],
            xyz[0] * CIEXYZ_LRGB[2][0] + xyz[1] * CIEXYZ_LRGB[2][1] + xyz[2] * CIEXYZ_LRGB[2][2], xyz[3]};
    }

    private static double[] convertColorToCIELab(final Color color) {
        final var normalizedRGBColor =
            normalizeToUnitInterval(new int[]{color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()});
        return convertSRGBColorToCIELab(normalizedRGBColor);
    }

    private static int[] clampToByte(final double[] rgb) {
        return Arrays.stream(rgb).mapToInt(c -> (int)Math.max(0, Math.min(255, Math.round(c * 255)))).toArray();
    }

    private static double[] normalizeToUnitInterval(final int[] rgb) {
        return Arrays.stream(rgb).mapToDouble(c -> c / 255.0).toArray();
    }
}
