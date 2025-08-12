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
 * ------------------------------------------------------------------------
 *
 * History
 *   09.07.2010 (hofer): created
 */
package org.knime.core.util.binning;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.OptionalDouble;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.util.binning.BinningSettings.BinBoundary.BinBoundaryExactMatchBehaviour;

/**
 * This class hold the settings required to use {@link BinningUtil}.
 *
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @since 5.8
 */
public final class BinningSettings {

    private BinningSettings() {
        // Only static members. This is a collection of settings.
    }

    /**
     * This interface defines the settings for binning. It allows for different types of binning - see the subclasses
     * for more detail.
     */
    public sealed interface BinningMethod {

        /**
         * Create bins with fixed width, i.e. each bin has the same width.
         *
         * @param numBins the number of bins to create.
         * @param integerBounds if true, the lower and upper bounds of the bins will be rounded to integers. This can
         *            affect the number of bins created.
         */
        public static record EqualWidth(int numBins, boolean integerBounds) implements BinningMethod {

            public static final String NAME_EQUAL_WIDTH = "Equal width";

            public static final String DESC_EQUAL_WIDTH = """
                    Creates bins of equal size across the value range. \
                    Requires a specified number of bins.
                    """;
        }

        /**
         * Create bins with fixed frequency, i.e. each bin contains the same number of values (or as close as possible).
         *
         * @param numBins the number of bins to create.
         * @param integerBounds if true, the lower and upper bounds of the bins will be rounded to integers. This can
         *            affect the number of bins created.
         */
        public static record EqualCount(int numBins, boolean integerBounds) implements BinningMethod {

            public static final String NAME_EQUAL_FREQUENCY = "Equal frequency";

            public static final String DESC_EQUAL_FREQUENCY = """
                    Creates bins with approximately the same number of values. \
                    Requires a specified number of bins.
                    """;
        }

        /**
         * Create bins based on a sample of quantiles. The quantiles are specified as boundaries, so at least two
         * quantiles must be provided.
         *
         * @param quantiles the quantiles to use for binning. These are the boundaries of the bins as quantiles.
         * @param integerBounds if true, the lower and upper bounds of the bins will be rounded to integers (after
         *            determining those with non-rounded values). This can affect the number of bins created.
         */
        public static record FixedQuantiles(BinBoundary[] quantiles, boolean integerBounds) implements BinningMethod {

            public static final String NAME_CUSTOM_QUANTILES = "Custom quantiles";

            private static final String QUANTILE_URL = "https://en.wikipedia.org/wiki/Quantile";

            public static final String DESC_CUSTOM_QUANTILES = """
                    Define bin edges based on quantile values. \
                    At least two quantiles are required. \
                    Quantiles are converted to bin edges \
                    using the R-7 algorithm, see <a href="
                    """ + QUANTILE_URL + """
                    ">WP:Quantile</a> for more details. \
                    Note that when setting upper or lower \
                    bounds quantiles are calculated only on \
                    values within those bounds.
                    """;

            @Override
            public boolean equals(final Object other) {
                return this == other || (other instanceof FixedQuantiles that && new EqualsBuilder() //
                    .append(this.quantiles, that.quantiles) //
                    .append(this.integerBounds, that.integerBounds) //
                    .isEquals());
            }

            @Override
            public int hashCode() {
                return new HashCodeBuilder() //
                    .append(quantiles) //
                    .append(integerBounds) //
                    .toHashCode();
            }

            @Override
            public String toString() {
                return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE) //
                    .append("quantiles", quantiles) //
                    .append("integerBounds", integerBounds) //
                    .toString();
            }
        }

        /**
         * Create bins based on fixed boundaries. The boundaries are specified as cutoffs, so at least two cutoffs must
         * be provided.
         *
         * @param boundaries the boundaries to use for binning. These are the cutoffs of the bins.
         */
        public static record FixedBoundaries(BinBoundary[] boundaries) implements BinningMethod {

            public static final String NAME_CUSTOM_CUTOFFS = "Custom cutoffs";

            public static final String DESC_CUSTOM_CUTOFFS = """
                    Manually define bin edges using a list of cutoff values. \
                    At least two cutoffs are required.
                    """;

            @Override
            public boolean equals(final Object other) {
                return this == other || (other instanceof FixedBoundaries that && new EqualsBuilder() //
                    .append(this.boundaries, that.boundaries) //
                    .isEquals());
            }

            @Override
            public int hashCode() {
                return new HashCodeBuilder() //
                    .append(boundaries) //
                    .toHashCode();
            }

            @Override
            public String toString() {
                return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE) //
                    .append("boundaries", boundaries) //
                    .toString();
            }
        }
    }

    /**
     * Everything that is needed to name bins in the binning process.
     *
     * @param binNaming the naming scheme for the bins, e.g. numbered, by borders or midpoints.
     * @param binNameForLowerOutliers the name of the bin that will contain values outside the lower bound * defined by
     *            this setting.
     * @param binNameForUpperOutliers the name of the bin that will contain values outside the upper bound * defined by
     *            this setting.
     */
    public record BinNamingScheme(//
        BinNaming binNaming, //
        String binNameForLowerOutliers, //
        String binNameForUpperOutliers //
    ) {
    }

    /**
     * Settings for the bounds of the data used for binning. This allows for setting fixed bounds, or no bounds at all.
     * In the former case, the bin that these outside values fall into will also be included in the setting.
     *
     * @param optLowerBound the lower bound
     * @param optUpperBound the upper bound
     */
    public record DataBounds(OptionalDouble optLowerBound, OptionalDouble optUpperBound) {
    }

    /**
     * Used to name bins pursuing a certain naming strategy.
     */
    @FunctionalInterface
    public interface BinNaming {

        /**
         * Compute the name of a bin based on its index and the lower and upper bounds;
         *
         * @param index the index of the bin, starting from 0.
         * @param lower the lower bound of the bin.
         * @param upper the upper bound of the bin.
         * @return the name of the bin, formatted according to the settings.
         */
        String computedName(final int index, final BinBoundary lower, final BinBoundary upper);

    }

    /**
     * Utilities for creating and documenting a {@link BinNaming}.
     */
    @SuppressWarnings("javadoc")
    public static final class BinNamingUtils {

        private BinNamingUtils() {
            // Utility class, no instances allowed
        }

        public static final String DESC_NUMBERED = "Bins are labeled by index.";

        public static final String DESC_BORDERS = "Bins are labeled using interval borders.";

        public static final String DESC_MIDPOINTS = "Bins are labeled using the midpoint of each interval.";

        public static final String NAME_NUMBERED = "Numbered (e.g., Bin 1, Bin 2)";

        public static final String NAME_BORDERS = "Borders (e.g., [0.0, 1.0))";

        public static final String NAME_MIDPOINTS = "Midpoints";

        public static final BinNaming numberedBinNaming = (index, lower, upper) -> "Bin " + (index + 1);

        public static BinNaming getBordersBinNaming(final BinNamingNumberFormatter numberFormatter) {
            return (index, lower, upper) -> {
                String openChar =
                    lower.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_LOWER_BIN ? "(" : "[";
                String closeChar =
                    upper.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_UPPER_BIN ? ")" : "]";
                return openChar //
                    + numberFormatter.format(lower.value())//
                    + ", " //
                    + numberFormatter.format(upper.value())//
                    + closeChar;
            };
        }

        public static BinNaming getMidpointsBinNaming(final BinNamingNumberFormatter numberFormatter) {
            return (index, lower, upper) -> {
                double midpoint = (lower.value() + upper.value()) / 2;
                return numberFormatter.format(midpoint);
            };
        }

        /**
         * This functional interface defines a formatter used within bin naming strategies that include numbers.
         */
        @FunctionalInterface
        public interface BinNamingNumberFormatter {

            /**
             * Format the given double value according to the settings defined in this formatter.
             *
             * @param value the double value to format
             * @return the formatted string representation of the value
             */
            String format(final double value);
        }

        /**
         * Utilities for creating and documenting a {@link BinNamingNumberFormatter}.
         */
        public static final class BinNamingNumberFormatterUtils {

            private BinNamingNumberFormatterUtils() {
                // Utility class, no instances allowed
            }

            /**
             * Creates a default number formatter that can be used in bin naming strategies.
             *
             * @param colSpec the column specification for which the formatter should be created
             * @return a default number formatter that formats numbers according to the column's value format handler,
             *         or a default formatter if no value format handler is defined.
             */
            public static BinNamingNumberFormatter getDefaultNumberFormatterForColumn(final DataColumnSpec colSpec) {
                final var valueFormatHandler = colSpec.getValueFormatHandler();
                if (valueFormatHandler != null) {
                    return value -> valueFormatHandler.getPlaintext(new DoubleCell(value));
                }
                return defaultFormatHandler;
            }

            /** for numbers less than 0.0001. */
            private static DecimalFormat smallFormat = new DecimalFormat("0.00E0", new DecimalFormatSymbols(Locale.US));

            /** in all other cases, use the default Java formatter. */
            private static NumberFormat defaultFormat = NumberFormat.getNumberInstance(Locale.US);

            /** the threshold where we switch between {@link #smallFormat} and {@link #defaultFormat} */
            private static double smallNumberThreshold = 0.0001;

            private static final BinNamingNumberFormatter defaultFormatHandler = value -> {

                if (value == 0.0) { // NOSONAR we actually want exact equality here
                    return "0";
                } else if (Double.isInfinite(value) || Double.isNaN(value)) {
                    return Double.toString(value);
                }
                double abs = Math.abs(value);
                var format = abs < smallNumberThreshold ? smallFormat : defaultFormat;
                synchronized (format) {
                    return format.format(value);
                }
            };

            /**
             * Creates a custom number formatter that can be used in bin naming strategies.
             *
             * @param numberFormat the format to use for numbers in the bins, e.g. standard, plain or engineering.
             * @param precision the precision to use for rounding numbers in the bins, e.g. number of decimal places or
             *            significant figures.
             * @param precisionMode the mode of precision to use for rounding numbers in the bins, e.g. decimal places
             *            or significant figures.
             * @param roundingMode the rounding direction to use for rounding numbers in the bins, e.g. up, down,
             *            ceiling, floor, half up, half down or half even.
             * @return a custom number formatter that formats numbers according to the specified settings.
             */
            public static BinNamingNumberFormatter createCustomNumberFormatter( //
                final CustomNumberFormat numberFormat, //
                final int precision, //
                final PrecisionMode precisionMode, //
                final RoundingMode roundingMode //
            ) {
                return value -> {
                    if (Double.isInfinite(value)) {
                        return value > 0 ? "Infinity" : "-Infinity";
                    } else if (Double.isNaN(value)) {
                        return "NaN";
                    }

                    var bd = BigDecimal.valueOf(value);

                    bd = switch (precisionMode) {
                        case DECIMAL_PLACES -> bd.setScale(precision, roundingMode);
                        case SIGNIFICANT_FIGURES -> bd.round(new MathContext(precision, roundingMode));
                    };

                    return switch (numberFormat) {
                        case STANDARD_STRING -> bd.toString();
                        case PLAIN_STRING -> bd.toPlainString();
                        case ENGINEERING_STRING -> bd.toEngineeringString();
                    };
                };
            }

            /**
             * The formatting of numbers in the bins.
             */
            public enum CustomNumberFormat {

                    STANDARD_STRING, //
                    PLAIN_STRING, //
                    ENGINEERING_STRING;

                public static final String NAME_STANDARD_STRING = "Standard";

                public static final String DESC_STANDARD_STRING = "Will use an exponent only if needed";

                public static final String NAME_PLAIN_STRING = "Plain";

                public static final String DESC_PLAIN_STRING = "Will not use an exponent, e.g. 1.234";

                public static final String NAME_ENGINEERING_STRING = "Engineering";

                public static final String DESC_ENGINEERING_STRING = """
                        Will use an exponent only if needed. Exponents are always a \
                        multiple of 3, e.g. 12.3E3
                        """;
            }

            /**
             * This enum defines the precision modes for number formatting.
             */
            public enum PrecisionMode {

                    DECIMAL_PLACES, //
                    SIGNIFICANT_FIGURES;

                public static final String NAME_DECIMAL_PLACES = "Decimal places";

                public static final String DESC_DECIMAL_PLACES = """
                        Will round to the given number of decimal \
                        places, e.g. 12.34567 will become \
                        12.346 if the precision is set to 3.
                        """;

                public static final String NAME_SIGNIFICANT_FIGURES = "Significant figures";

                public static final String DESC_SIGNIFICANT_FIGURES = """
                        Will round to the given number of significant \
                        figures, e.g. 12.34567 will become \
                        12.3 if the precision is set to 3.
                        """;
            }

            /**
             * If numbers are to be rounded, a {@link RoundingMode} is used. This class defines the available rounding
             * directions.
             */
            public static final class RoundingModeUtils {

                private RoundingModeUtils() {
                    // Utility class, no instances allowed
                }

                public static final String NAME_UP = "Up";

                public static final String DESC_UP = """
                        Will round away from zero, e.g. 1.2 will become \
                        2 and -1.2 will become -2.
                        """;

                public static final String NAME_DOWN = "Down";

                public static final String DESC_DOWN = """
                        Will round towards zero, e.g. 1.2 will become \
                        1 and -1.2 will become -1.
                        """;

                public static final String NAME_CEILING = "Ceiling";

                public static final String DESC_CEILING = """
                        Will round towards positive infinity, e.g. 1.2 will become \
                        2 and -1.2 will become -1.
                        """;

                public static final String NAME_FLOOR = "Floor";

                public static final String DESC_FLOOR = """
                        Will round towards negative infinity, e.g. 1.2 will become \
                        1 and -1.2 will become -2.
                        """;

                public static final String NAME_HALF_UP = "Half up";

                public static final String DESC_HALF_UP = """
                        Will round towards the nearest neighbor. When the number is exactly \
                        halfway between two neighbors, it will round away from zero, e.g. \
                        1.5 will become 2 and -1.5 will become -2.
                        """;

                public static final String NAME_HALF_DOWN = "Half down";

                public static final String DESC_HALF_DOWN = """
                        Will round towards the nearest neighbor. When the number is exactly \
                        halfway between two neighbors, it will round towards zero, e.g. \
                        1.5 will become 1 and -1.5 will become -1.
                        """;

                public static final String NAME_HALF_EVEN = "Half even";

                public static final String DESC_HALF_EVEN = """
                        Will round towards the nearest neighbor. When the number is exactly \
                        halfway between two neighbors, it will round towards the nearest even \
                        neighbor, e.g. 1.5 will become 2 and 2.5 will become 2.
                        """;

            }
        }
    }

    /**
     * This record represents a bin boundary, which is a value that defines the edge of a bin.
     *
     * @param value the value of the boundary
     * @param exactMatchBehaviour the behaviour when a value falls exactly on this boundary, e.g. whether it should be
     *            assigned to the lower or upper bin.
     */
    public record BinBoundary( //
        double value, //
        BinBoundaryExactMatchBehaviour exactMatchBehaviour //
    ) {

        /**
         * This enum defines the behaviour of the binning when a value falls exactly on the boundary between two bins.
         */
        @SuppressWarnings("javadoc")
        public enum BinBoundaryExactMatchBehaviour {
                TO_LOWER_BIN, //
                TO_UPPER_BIN;

            public static final String NAME_TO_LOWER_BIN = "To lower bin";

            public static final String DESC_TO_LOWER_BIN = "Assign to the bin below the cutoff.";

            public static final String NAME_TO_UPPER_BIN = "To upper bin";

            public static final String DESC_TO_UPPER_BIN = "Assign to the bin above the cutoff.";
        }
    }
}
