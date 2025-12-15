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
 *   11 Dec 2025 (robin): created
 */
package org.knime.core.data.property;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.awt.Color;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
final class ColorSpaceConversionUtilTest {

    final static double[][] SRGB_TEST_COLORS_0_1 = new double[][]{ //
        {0.47058823529411764, 0.7843137254901961, 0.3137254901960784, 1.0}, //
        {1.0, 0.1803921568627451, 0.9725490196078431, 128.0 / 255.0}, //
        {0.0784313725490196, 0.0784313725490196, 0.7607843137254902, 64.0 / 255.0}, //
        {0.7607843137254902, 0.8431372549019608, 0.11372549019607843, 0.0}};

    final static double[][] SRGB_TEST_COLORS_0_255 = new double[][]{ //
        {120.0, 200.0, 80.0, 255.0}, //
        {255.0, 46.0, 248.0, 128.0}, //
        {20.0, 20.0, 194.0, 64.0}, //
        {194.0, 215.0, 29.0, 0.0}};

    final static double[][] CIELAB_TEST_COLORS = new double[][]{ //
        {73.59149716807372, -41.52176104621647, 50.479410478266786, 1.0},
        {61.56702057297285, 87.95093870605919, -54.40657638349404, 128.0 / 255.0},
        {23.429609558208412, 50.7541093579105, -87.27791267802432, 64.0 / 255.0},
        {82.26738074274408, -22.220797955100902, 76.68615888571323, 0.0}};

    final static Color[] JAVA_COLOR_TEST_COLORS = new Color[]{ //
        new Color(120, 200, 80, 255), //
        new Color(255, 46, 248, 128), //
        new Color(20, 20, 194, 64), //
        new Color(194, 215, 29, 0)};

    final static String[] HEX_TEST_COLORS = new String[]{ //
        "#FF78C850", //
        "#80FF2EF8", //
        "#401414C2", //
        "#00C2D71D"};

    private static final double EPSILON = 1e-6;

    @Test
    final void testSRGBColorsToAndFromCIELabConversion() {
        final var colorsCIELab = ColorSpaceConversionUtil.convertSRGBColorsToCIELab(SRGB_TEST_COLORS_0_1);
        IntStream.range(0, colorsCIELab.length).forEach(i -> {
            final var expected = CIELAB_TEST_COLORS[i];
            final var actual = colorsCIELab[i];
            assertArrayEquals(expected, actual, EPSILON);
        });
    }

    @Test
    final void testJavaColorsToAndFromCIELabConversion() {
        final var colorsCIELab = ColorSpaceConversionUtil.convertJavaColorsToCIELab(JAVA_COLOR_TEST_COLORS);
        IntStream.range(0, colorsCIELab.length).forEach(i -> {
            final var expected = CIELAB_TEST_COLORS[i];
            final var actual = colorsCIELab[i];
            assertArrayEquals(expected, actual, EPSILON);
        });
        final var javaColors =
            Arrays.stream(colorsCIELab).map(ColorSpaceConversionUtil::convertCIELabToJavaColor).toArray(Color[]::new);
        assertArrayEquals(JAVA_COLOR_TEST_COLORS, javaColors);
    }

    @Test
    final void testHexColorsToAndFromCIELabConversion() {
        final var colorsCIELab = ColorSpaceConversionUtil.convertHexColorsToCIELab(HEX_TEST_COLORS);
        IntStream.range(0, colorsCIELab.length).forEach(i -> {
            final var expected = CIELAB_TEST_COLORS[i];
            final var actual = colorsCIELab[i];
            assertArrayEquals(expected, actual, EPSILON);
        });
        final var hexColors = Arrays.stream(colorsCIELab).map(ColorSpaceConversionUtil::convertCIELabColorToHexString)
            .toArray(String[]::new);
        assertArrayEquals(HEX_TEST_COLORS, hexColors);
    }

}
