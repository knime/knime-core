/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   02.06.2006 (gabriel): created
 */
package org.knime.core.data.property;

import java.awt.Color;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;

/**
 * Computes colors based on stop values assigned to certain colors which are interpolated between the closest minimum
 * and maximum stop value.
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @author Thomas Gabriel, University of Konstanz
 */
public final class ColorModelRange implements ColorModel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ColorModelRange.class);

    /**
     * Identifiers for special colors used in the color model.
     *
     * @since 5.10
     * @noreference This enum is not intended to be referenced by clients.
     * @noextend This enum is not intended to be extended or implemented by clients.
     */
    @SuppressWarnings("javadoc")
    public enum SpecialColorType {
            MISSING, NAN, NEGATIVE_INFINITY, BELOW_MIN, ABOVE_MAX, POSITIVE_INFINITY
    }

    private final Map<SpecialColorType, Color> m_specialColors;

    private final double[] m_stopValues;

    private final double[][] m_stopColorsCIELab;

    private final ColorGradient m_gradient;

    private final boolean m_stopValuesArePercentages;

    /**
     * this is basically a flag to distinguish between old color models created with the Color Manager and models
     * created with the Color Gradient node, added with 5.10. The manager interpolates in RGB space whereas the gradient
     * node interpolates in CIELab space.
     */
    private final boolean m_interpolateCIELab;

    private static final Map<Predicate<Double>, SpecialColorType> SPECIAL_VALUE_PREDICATES =
        Map.of(value -> Double.isNaN(value), SpecialColorType.NAN, //
            value -> Double.isInfinite(value) && value > 0, SpecialColorType.POSITIVE_INFINITY, //
            value -> Double.isInfinite(value) && value < 0, SpecialColorType.NEGATIVE_INFINITY);

    /**
     * Creates new ColorHandler based on a mapping of a min value and color and a max value and color.
     *
     * @param lower Lower bound.
     * @param min Color of lower bound.
     * @param upper Upper bound.
     * @param max Color of upper bound.
     * @throws IllegalArgumentException If lower is greater upper bound
     */
    public ColorModelRange(final double lower, final Color min, final double upper, final Color max) {
        this(Map.of(), ColorGradient.CUSTOM, new double[]{lower, upper},
            ColorSpaceConversionUtil.convertJavaColorsToCIELab(new Color[]{min, max}), false, false);
    }

    /**
     * Mapping based on stop values and colors for custom gradients.
     *
     * @param specialColors a map of special colors for special values
     * @param stopValues the values at which the corresponding stop colors are defined and between which the colors are
     *            interpolated
     * @param stopColors the colors corresponding to the stop values
     * @param stopValuesArePercentages whether the stop values are given as percentages (0-100) and need to be
     *            determined before further use (via {@link #applyToDomain(double, double)}) of the model or absolute
     *            values.
     * @throws IllegalArgumentException if stop values are not sorted in non-decreasing order, or if there are less than
     *             two stop colors, or if the stop values are absolute and the length of stop values and stop colors
     *             differs
     * @since 5.10
     */
    public ColorModelRange(final Map<SpecialColorType, Color> specialColors, final double[] stopValues,
        final Color[] stopColors, final boolean stopValuesArePercentages) {
        this(specialColors, ColorGradient.CUSTOM, stopValues,
            ColorSpaceConversionUtil.convertJavaColorsToCIELab(stopColors), stopValuesArePercentages, true);
    }

    /**
     * Mapping based on a color gradient and special colors. The stop values are assumed to be percentages (0-100) and
     * can be applied to a domain by {@link #applyToDomain(double, double)}.
     *
     * @param specialColors a map of special colors for special values
     * @param gradient the color gradient to use
     * @throws IllegalArgumentException if stop values are not sorted in non-decreasing order or if there are less than
     *             two stop colors
     * @since 5.10
     */
    public ColorModelRange(final Map<SpecialColorType, Color> specialColors, final ColorGradient gradient) {
        this(specialColors, gradient, new double[0], gradient.getGradientColorsCIELab(), true, true);

    }

    private ColorModelRange(final Map<SpecialColorType, Color> specialColors, final ColorGradient gradient,
        final double[] stopValues, final double[][] stopColorsCIELab, final boolean stopValuesArePercentages,
        final boolean interpolateLab) {
        CheckUtils.check(stopColorsCIELab.length >= 2, IllegalArgumentException::new,
            () -> "At least two stop colors are required.");
        if (interpolateLab) {
            CheckUtils.check(Arrays.stream(SpecialColorType.values()).allMatch(specialColors::containsKey),
                IllegalArgumentException::new,
                () -> "All special colors must be defined when interpolating in CIELab color space.");
            CheckUtils.check(IntStream.range(1, stopValues.length).allMatch(i -> stopValues[i - 1] <= stopValues[i]),
                IllegalArgumentException::new, () -> "Stop values must be sorted in non-decreasing order.");
            if (stopValuesArePercentages) {
                CheckUtils.check(Arrays.stream(stopValues).allMatch(v -> v >= 0.0 && v <= 100.0),
                    IllegalArgumentException::new,
                    () -> "All stop values must be between 0 and 100 when using percentage values.");
            } else {
                CheckUtils.check(stopValues.length == stopColorsCIELab.length, IllegalArgumentException::new,
                    () -> "The length of stopValues and stopColors must be equal.");
            }
        } else {
            if (Arrays.stream(stopValues).allMatch(Objects::isNull)) {
                throw new IllegalArgumentException();
            }
        }

        m_specialColors = specialColors;
        m_gradient = gradient;
        m_stopValuesArePercentages = stopValuesArePercentages;
        m_stopValues = stopValues;
        m_stopColorsCIELab = stopColorsCIELab;
        m_interpolateCIELab = interpolateLab;
    }

    /**
     * Returns a ColorAttr for the given DataCell value, or <code>ColorAttr.DEFAULT</code> if not set.</br>
     * For models created with the Color Manager node, the colors red, green, and blue are merged in the same ratio from
     * the original spread of the lower and upper bounds.</br>
     * For models created with the Color Gradient node (since 5.10), the colors are interpolated between the defined
     * stop colors in the CIELab color space.
     *
     * @param dc A DataCell value to get color for.
     * @return A ColorAttr for a DataCell value or the DEFAULT ColorAttr.
     */
    @Override
    public ColorAttr getColorAttr(final DataCell dc) {
        if (dc == null || !dc.getType().isCompatible(DoubleValue.class)) {
            return ColorAttr.DEFAULT;
        }
        if (!m_interpolateCIELab) {
            return interpolateRGB(dc);
        }
        if (m_stopValuesArePercentages) {
            LOGGER.warn("The used color model was not applied to a domain yet. Cannot interpolate percentage "
                + "stop values. Returning default color.");
            return ColorAttr.DEFAULT;
        }
        if (dc.isMissing()) {
            return ColorAttr.getInstance(m_specialColors.get(SpecialColorType.MISSING));
        }
        return interpolateLab(dc);
    }

    private ColorAttr interpolateLab(final DataCell dc) {
        final var value = ((DoubleValue)dc).getDoubleValue();
        for (final var entry : SPECIAL_VALUE_PREDICATES.entrySet()) {
            if (entry.getKey().test(value)) {
                return ColorAttr.getInstance(m_specialColors.get(entry.getValue()));
            }
        }
        if (value < m_stopValues[0]) {
            return ColorAttr.getInstance(m_specialColors.get(SpecialColorType.BELOW_MIN));
        }
        final var numValues = m_stopValues.length;
        if (value > m_stopValues[numValues - 1]) {
            return ColorAttr.getInstance(m_specialColors.get(SpecialColorType.ABOVE_MAX));
        }

        var i = 0;
        while (i < numValues - 1 && !(value >= m_stopValues[i] && value <= m_stopValues[i + 1])) {
            i++;
        }

        final var value0 = m_stopValues[i];
        final var color0 = m_stopColorsCIELab[i];
        final var value1 = m_stopValues[i + 1];
        final var color1 = m_stopColorsCIELab[i + 1];
        final var step = (value - value0) / (value1 - value0);

        final var l = color0[0] + step * (color1[0] - color0[0]);
        final var a = color0[1] + step * (color1[1] - color0[1]);
        final var b = color0[2] + step * (color1[2] - color0[2]);
        final var alpha = (color0[3] + step * (color1[3] - color0[3]));
        return ColorAttr.getInstance(ColorSpaceConversionUtil.convertCIELabToJavaColor(new double[]{l, a, b, alpha}));
    }

    private ColorAttr interpolateRGB(final DataCell dc) {
        if (dc.isMissing()) {
            return ColorAttr.DEFAULT;
        }
        final var minValue = getMinValue();
        final var maxValue = getMaxValue();
        final var minColor = getMinColor();
        final var maxColor = getMaxColor();
        final var range = maxValue - minValue;
        double ratio;
        // if lower and upper bound are equal
        if (range == 0.0) {
            // take the "middle" of both colors
            ratio = 0.5;
        } else {
            double value = ((DoubleValue)dc).getDoubleValue();
            ratio = ((value - minValue) / range);
        }
        int r = (int)((maxColor.getRed() - minColor.getRed()) * ratio) + minColor.getRed();
        int g = (int)((maxColor.getGreen() - minColor.getGreen()) * ratio) + minColor.getGreen();
        int b = (int)((maxColor.getBlue() - minColor.getBlue()) * ratio) + minColor.getBlue();
        // check color ranges first
        if (r < 0) {
            r = 0;
        }
        if (r > 255) {
            r = 255;
        }
        if (g < 0) {
            g = 0;
        }
        if (g > 255) {
            g = 255;
        }
        if (b < 0) {
            b = 0;
        }
        if (b > 255) {
            b = 255;
        }
        int alpha = (minColor.getAlpha() + maxColor.getAlpha()) / 2;
        return ColorAttr.getInstance(new Color(r, g, b, alpha));
    }

    /** @return minimum double value. */
    public double getMinValue() {
        return m_stopValues[0];
    }

    /** @return minimum Color value. */
    public Color getMinColor() {
        return ColorSpaceConversionUtil.convertCIELabToJavaColor(m_stopColorsCIELab[0]);
    }

    /**
     * @return {@link #getMinColor()} has opaque 24bit hex string.
     * @since 5.1
     */
    public String getMinColorHex() {
        return ColorModel.colorToHexString(getMinColor());
    }

    /** @return maximum double value. */
    public double getMaxValue() {
        return m_stopValues[m_stopValues.length - 1];
    }

    /**
     * @return {@link #getMaxColor()} has opaque 24bit hex string.
     * @since 5.1
     */
    public String getMaxColorHex() {
        return ColorModel.colorToHexString(getMaxColor());
    }

    /** @return maximum Color value. */
    public Color getMaxColor() {
        return ColorSpaceConversionUtil.convertCIELabToJavaColor(m_stopColorsCIELab[m_stopColorsCIELab.length - 1]);
    }

    /**
     * @return whether the stop values are given as percentages (0-100). If true, the model needs to be applied to a
     *         domain by {@link #applyToDomain(double, double)} before use.
     * @since 5.10
     */
    public boolean isPercentageBased() {
        return m_stopValuesArePercentages;
    }

    private static final String CFG_LOWER_VALUE = "lower_value";

    private static final String CFG_UPPER_VALUE = "upper_value";

    private static final String CFG_LOWER_COLOR = "lower_color";

    private static final String CFG_UPPER_COLOR = "upper_color";

    private static final String CFG_STOP_VALUES_ARE_PERCENTAGES = "stopValuesArePercentages";

    private static final String CFG_INTERPOLATE_CIELAB = "interpolateCIELab";

    private static final String CFG_STOP_VALUES = "stopValues";

    private static final String CFG_STOP_COLORS_HEX = "stopColorsHex";

    private static final String CFG_GRADIENT = "gradient";

    /**
     * Save lower and upper, and min and max colors to the given Config.
     *
     * @param config to save settings to.
     * @see org.knime.core.data.property.ColorModel #save(ConfigWO)
     */
    @Override
    public void save(final ConfigWO config) {
        config.addBoolean(CFG_STOP_VALUES_ARE_PERCENTAGES, m_stopValuesArePercentages);
        config.addBoolean(CFG_INTERPOLATE_CIELAB, m_interpolateCIELab);
        if (m_gradient == ColorGradient.CUSTOM) {
            config.addDoubleArray(CFG_STOP_VALUES, m_stopValues);
            config.addStringArray(CFG_STOP_COLORS_HEX, Arrays.stream(m_stopColorsCIELab)
                .map(ColorSpaceConversionUtil::convertCIELabColorToHexString).toArray(String[]::new));
        } else if (!m_stopValuesArePercentages) {
            /**
             * we can recreate a non-custom scale from only the gradient name if the values are percentages as their
             * range is 0-100. For absolute values we need to store at least the min and max values to be able to
             * recreate the scale as the values in between are determined by the number of colors of the scale.
             */
            config.addDoubleArray(CFG_STOP_VALUES, m_stopValues[0], m_stopValues[m_stopValues.length - 1]);
        }
        config.addString(CFG_GRADIENT, m_gradient.name());
        m_specialColors.forEach((key, color) -> config.addString(key.name(), ColorModel.colorToHexString(color)));
    }

    /**
     * Load color settings from Config including lower and upper bound, and min and max colors.
     *
     * @param config Read settings from.
     * @return A new <code>ColorModelRange</code> object.
     * @throws InvalidSettingsException If the settings could not be read.
     */
    public static ColorModelRange load(final ConfigRO config) throws InvalidSettingsException {
        if (config.containsKey(CFG_LOWER_COLOR)) {
            final var lower = config.getDouble(CFG_LOWER_VALUE);
            final var upper = config.getDouble(CFG_UPPER_VALUE);
            final var minColor = loadColorBefore5Dot10(config, CFG_LOWER_COLOR);
            final var maxColor = loadColorBefore5Dot10(config, CFG_UPPER_COLOR);
            return new ColorModelRange(lower, minColor, upper, maxColor);
        }
        final var stopValuesArePercentages = config.getBoolean(CFG_STOP_VALUES_ARE_PERCENTAGES);
        final var gradient = ColorGradient.valueOf(config.getString(CFG_GRADIENT));
        Map<SpecialColorType, Color> specialColors = new EnumMap<>(SpecialColorType.class);
        for (final var type : SpecialColorType.values()) {
            final var typeName = type.name();
            if (config.containsKey(typeName)) {
                specialColors.put(type, Color.decode(config.getString(typeName)));
            }
        }
        if (gradient != ColorGradient.CUSTOM) {
            var colorModel = new ColorModelRange(specialColors, gradient);
            if (!stopValuesArePercentages) {
                final var stopValues = config.getDoubleArray(CFG_STOP_VALUES);
                colorModel = colorModel.applyToDomain(stopValues[0], stopValues[1]);
            }
            return colorModel;
        }

        final var stopValues = config.getDoubleArray(CFG_STOP_VALUES);
        final var stopColorsHex = config.getStringArray(CFG_STOP_COLORS_HEX);
        final var stopColors = Arrays.stream(stopColorsHex).map(ColorSpaceConversionUtil::convertHexColorToCIELab)
            .toArray(double[][]::new);
        final var interpolateLab = config.getBoolean(CFG_INTERPOLATE_CIELAB);
        return new ColorModelRange(specialColors, gradient, stopValues, stopColors, stopValuesArePercentages,
            interpolateLab);
    }

    private static Color loadColorBefore5Dot10(final ConfigRO config, final String configKey)
        throws InvalidSettingsException {
        try {
            // load color components before 5.10
            int[] colComponents = config.getIntArray(configKey);
            return new Color(colComponents[0], colComponents[1], colComponents[2], colComponents[3]);
        } catch (InvalidSettingsException ise) {
            // load color components before 2.0
            int color = config.getInt(configKey);
            return new Color(color, true);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this) //
            .append(CFG_GRADIENT, m_gradient) //
            .append(CFG_STOP_VALUES_ARE_PERCENTAGES, m_stopValuesArePercentages) //
            .append(CFG_INTERPOLATE_CIELAB, m_interpolateCIELab) //
            .append("specialColors", m_specialColors) //
            .append(CFG_STOP_VALUES, m_stopValues) //
            .append("stopColorsCIELab", m_stopColorsCIELab) //
            .build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof ColorModelRange)) {
            return false;
        }
        ColorModelRange cmodel = (ColorModelRange)obj;
        return new EqualsBuilder() //
            .append(m_specialColors, cmodel.m_specialColors) //
            .append(m_gradient, cmodel.m_gradient) //
            .append(m_stopValues, cmodel.m_stopValues) //
            .append(m_stopColorsCIELab, cmodel.m_stopColorsCIELab) //
            .append(m_stopValuesArePercentages, cmodel.m_stopValuesArePercentages) //
            .append(m_interpolateCIELab, cmodel.m_interpolateCIELab) //
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder() //
            .append(m_specialColors) //
            .append(m_gradient) //
            .append(m_stopValues) //
            .append(m_stopColorsCIELab) //
            .append(m_stopValuesArePercentages) //
            .append(m_interpolateCIELab) //
            .build();
    }

    /**
     * Applies the current color model to the given domain by distributing the stop colors evenly between min and max.
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return a new color model applied to the given domain
     *
     * @since 5.10
     */
    public ColorModelRange applyToDomain(final double min, final double max) {
        checkDomainIsFinite(min);
        checkDomainIsFinite(max);
        final var range = max - min;
        final double[] stopValues;
        if (m_stopValues.length == m_stopColorsCIELab.length) {
            stopValues =
                Arrays.stream(m_stopValues).map(percentageValue -> min + (percentageValue / 100.0) * range).toArray();
        } else {
            final var step = range / (m_stopColorsCIELab.length - 1);
            stopValues = IntStream.range(0, m_stopColorsCIELab.length).mapToDouble(i -> min + i * step).toArray();
        }
        return new ColorModelRange(m_specialColors, m_gradient, stopValues, m_stopColorsCIELab, false,
            m_interpolateCIELab);
    }

    private static void checkDomainIsFinite(final double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Domain values must be finite numbers.");
        }
    }
}
