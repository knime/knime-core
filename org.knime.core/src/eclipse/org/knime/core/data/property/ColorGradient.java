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
 *   1 Dec 2025 (Robin Gerling): created
 */
package org.knime.core.data.property;

import static org.knime.core.data.property.ColorGradientDefinitionUtil.CIVIDIS_COLORS;
import static org.knime.core.data.property.ColorGradientDefinitionUtil.INFERNO_COLORS;
import static org.knime.core.data.property.ColorGradientDefinitionUtil.MAGMA_COLORS;
import static org.knime.core.data.property.ColorGradientDefinitionUtil.MATPLOTLIB_TWILIGHT_COLORS;
import static org.knime.core.data.property.ColorGradientDefinitionUtil.MATPLOTLIB_TWILIGHT_SHIFTED_COLORS;
import static org.knime.core.data.property.ColorGradientDefinitionUtil.PLASMA_COLORS;
import static org.knime.core.data.property.ColorGradientDefinitionUtil.VIRIDIS_COLORS;

import java.util.Arrays;

/**
 * Some predefined color gradients to be used in the Color Gradient Designer node.
 *
 * @author Robin Gerling
 * @since 5.10
 * @noreference This enum is not intended to be referenced by clients.
 * @noextend This enum is not intended to be extended or implemented by clients.
 */
@SuppressWarnings("javadoc")
public enum ColorGradient {

        CUSTOM(),

        CIVIDIS(CIVIDIS_COLORS),

        VIRIDIS(VIRIDIS_COLORS),

        INFERNO(INFERNO_COLORS),

        MAGMA(MAGMA_COLORS),

        PLASMA(PLASMA_COLORS),

        GRAYSCALE(new String[]{"#FFFFFF", "#000000"}),

        SHORTENED_GRAYSCALE(new String[]{"#FFFFFF", "#D9D9D9", "#BDBDBD", "#636363"}),

        /** See ColorBrewer, https://colorbrewer2.org/ */
        PURPLE_ORANGE_5(new String[]{"#e66101", "#fdb863", "#f7f7f7", "#b2abd2", "#5e3c99"}),

        PURPLE_ORANGE_11(new String[]{"#7f3b08", "#b35806", "#e08214", "#fdb863", "#fee0b6", "#f7f7f7", "#d8daeb",
            "#b2abd2", "#8073ac", "#542788", "#2d004b"}),

        RED_BLUE_5(new String[]{"#ca0020", "#f4a582", "#f7f7f7", "#92c5de", "#0571b0"}),

        RED_BLUE_11(new String[]{"#67001f", "#b2182b", "#d6604d", "#f4a582", "#fddbc7", "#f7f7f7", "#d1e5f0", "#92c5de",
            "#4393c3", "#2166ac", "#053061"}),

        PURPLE_GREEN_5(new String[]{"#7b3294", "#c2a5cf", "#f7f7f7", "#a6dba0", "#008837"}),

        PURPLE_GREEN_11(new String[]{"#40004b", "#762a83", "#9970ab", "#c2a5cf", "#e7d4e8", "#f7f7f7", "#d9f0d3",
            "#a6dba0", "#5aae61", "#1b7837", "#00441b"}),

        MATPLOTLIB_TWILIGHT(MATPLOTLIB_TWILIGHT_COLORS),

        MATPLOTLIB_TWILIGHT_SHIFTED(MATPLOTLIB_TWILIGHT_SHIFTED_COLORS);

    private final double[][] m_gradientColorsCIELab;

    ColorGradient() {
        m_gradientColorsCIELab = null;
    }

    ColorGradient(final String[] gradientColorsHex) {
        m_gradientColorsCIELab =
            cloneGradientColors(ColorSpaceConversionUtil.convertHexColorsToCIELab(gradientColorsHex));
    }

    ColorGradient(final double[][] gradientColorsSRGB) {
        m_gradientColorsCIELab =
            cloneGradientColors(ColorSpaceConversionUtil.convertSRGBColorsToCIELab(gradientColorsSRGB));
    }

    /**
     * @return the paletteAsColor (or <code>null</code> for {@link ColorGradient#CUSTOM}).
     */
    double[][] getGradientColorsCIELab() {
        return m_gradientColorsCIELab;
    }

    private static double[][] cloneGradientColors(final double[][] gradientColors) {
        return Arrays.stream(gradientColors).map(double[]::clone).toArray(double[][]::new);
    }

}
