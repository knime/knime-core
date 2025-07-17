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
package org.knime.core.util.binning.auto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.OptionalDouble;

import org.knime.core.util.binning.auto.AutoBinningSettings.BinningSettings.EqualCount;
import org.knime.core.util.binning.auto.AutoBinningSettings.BinningSettings.EqualWidth;
import org.knime.core.util.binning.auto.AutoBinningSettings.BinningSettings.FixedBoundaries;
import org.knime.core.util.binning.auto.AutoBinningSettings.BinningSettings.FixedQuantiles;
import org.knime.core.util.binning.auto.AutoBinningSettings.DataBoundsSettings.BoundSetting.FixedBound;
import org.knime.core.util.binning.auto.AutoBinningSettings.DataBoundsSettings.BoundSetting.NoBound;
import org.knime.core.util.binning.auto.AutoBinningSettings.NumberFormattingSettings.ColumnFormat;
import org.knime.core.util.binning.auto.AutoBinningSettings.NumberFormattingSettings.CustomFormat;

/**
 * This class hold the settings required to use {@link AutoBinningPMMLCreator}.
 *
 * @param binning the binning settings, which define how the bins should be created, e.g. equal width or a custom list
 *            of quantiles (amongst others).
 * @param integerBounds if true, the lower and upper bounds of the bins will be rounded to integers. This can affect the
 *            number of bins created.
 * @param binNaming the naming scheme for the bins, e.g. numbered, by borders or midpoints, along with how any doubles
 *            in the bin names should be formatted.
 * @param boundsSettings the settings for the data bounds, e.g. if there should be some fixed bounds beyond which
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @since 5.6
 */
public record AutoBinningSettings( //
    BinningSettings binning, //
    boolean integerBounds, //
    BinNamingSettings binNaming, //
    DataBoundsSettings boundsSettings //
) {

    /**
     * This interface defines the settings for binning. It allows for different types of binning - see the subclasses
     * for more detail.
     */
    public sealed interface BinningSettings permits EqualWidth, EqualCount, FixedQuantiles, FixedBoundaries {

        /**
         * Get the {@link BinningType} associated with this binning settings.
         *
         * @return the type of binning
         */
        BinningType binningType();

        /**
         * Create bins with fixed width, i.e. each bin has the same width.
         *
         * @param numBins the number of bins to create.
         */
        public static record EqualWidth(int numBins) implements BinningSettings {
            @Override
            public BinningType binningType() {
                return BinningType.EQUAL_WIDTH;
            }
        }

        /**
         * Create bins with fixed frequency, i.e. each bin contains the same number of values (or as close as possible).
         *
         * @param numBins the number of bins to create.
         */
        public static record EqualCount(int numBins) implements BinningSettings {
            @Override
            public BinningType binningType() {
                return BinningType.EQUAL_FREQUENCY;
            }
        }

        /**
         * Create bins based on a sample of quantiles. The quantiles are specified as boundaries, so at least two
         * quantiles must be provided.
         *
         * @param quantiles the quantiles to use for binning. These are the boundaries of the bins as quantiles.
         */
        public static record FixedQuantiles(BinBoundary[] quantiles) implements BinningSettings { // NOSONAR equals not important for this record
            @Override
            public BinningType binningType() {
                return BinningType.CUSTOM_QUANTILES;
            }
        }

        /**
         * Create bins based on fixed boundaries. The boundaries are specified as cutoffs, so at least two cutoffs must
         * be provided.
         *
         * @param boundaries the boundaries to use for binning. These are the cutoffs of the bins.
         */
        public static record FixedBoundaries(BinBoundary[] boundaries) implements BinningSettings { // NOSONAR doesn't need .equals
            @Override
            public BinningType binningType() {
                return BinningType.CUSTOM_CUTOFFS;
            }
        }
    }

    /**
     * This class holds the settings for naming bins.
     *
     * @param binNaming the naming scheme for the bins, e.g. numbered, by borders or midpoints.
     * @param numberFormattingSettings the formatting of any doubles in the bin names.
     */
    public static record BinNamingSettings( //
        BinNaming binNaming, //
        NumberFormattingSettings numberFormattingSettings //
    ) {

        /**
         * Compute the name of a bin based on its index and the lower and upper bounds, subject to the settings defined
         * here.
         *
         * @param index the index of the bin, starting from 0.
         * @param lower the lower bound of the bin.
         * @param upper the upper bound of the bin.
         * @return the name of the bin, formatted according to the settings.
         */
        public String computedName(final int index, final BinBoundary lower, final BinBoundary upper) {
            return switch (binNaming) {
                case NUMBERED -> "Bin " + (index + 1);
                case BORDERS -> openChar(lower.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_LOWER_BIN) //
                    + numberFormattingSettings.getFormattedValue(lower.value()) //
                    + ", " //
                    + numberFormattingSettings.getFormattedValue(upper.value()) //
                    + closeChar(upper.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_UPPER_BIN);
                case MIDPOINTS -> numberFormattingSettings.getFormattedValue((lower.value() + upper.value()) / 2);
            };
        }

        private static String openChar(final boolean open) {
            return open ? "(" : "[";
        }

        private static String closeChar(final boolean open) {
            return open ? ")" : "]";
        }
    }

    /**
     * Settings for the bounds of the data used for binning. This allows for setting fixed bounds, or no bounds at all.
     * In the former case, the bin that these outside values fall into will also be included in the setting.
     *
     * @param lowerBound the lower bound setting
     * @param upperBound the upper bound setting
     */
    public record DataBoundsSettings(BoundSetting lowerBound, BoundSetting upperBound) {

        /**
         * Get the upper bound of the data used for binning as an optional. If there is a fixed bound, the value will be
         * present, otherwise it will be empty.
         *
         * @return the upper bound
         */
        public OptionalDouble optUpperBound() {
            return upperBound instanceof BoundSetting.FixedBound fixed //
                ? OptionalDouble.of(fixed.value()) //
                : OptionalDouble.empty();
        }

        /**
         * Get the lower bound of the data used for binning as an optional. If there is a fixed bound, the value will be
         * present, otherwise it will be empty.
         *
         * @return the lower bound
         */
        public OptionalDouble optLowerBound() {
            return lowerBound instanceof BoundSetting.FixedBound fixed //
                ? OptionalDouble.of(fixed.value()) //
                : OptionalDouble.empty();
        }

        /**
         * Get the name of the bin that will contain values outside the upper bound defined by this setting.
         *
         * @return the bin name
         */
        public String binNameForValuesOutsideUpperBound() {
            return upperBound.binNameForValuesOutsideBound();
        }

        /**
         * Get the name of the bin that will contain values outside the lower bound defined by this setting.
         *
         * @return the bin name
         */
        public String binNameForValuesOutsideLowerBound() {
            return lowerBound.binNameForValuesOutsideBound();
        }

        /**
         * The settings for a single boundary.
         */
        public sealed interface BoundSetting permits FixedBound, NoBound {

            /**
             * Get the name of the bin that will contain values outside the bounds defined by this setting.
             *
             * @return the bin name
             */
            String binNameForValuesOutsideBound();

            /**
             * This record represents a bound setting that imposes fixed bounds on the data. If the data falls outside
             * the bounds, it will be assigned to a bin with the provided bin name.
             *
             * @param value the fixed value of the bound
             * @param binNameForValuesOutsideBound the name of the bin that will contain values outside this bound
             */
            public static record FixedBound(double value, String binNameForValuesOutsideBound) implements BoundSetting {
            }

            /**
             * This record represents a bound setting that does not impose any fixed bounds on the data.
             *
             * However, it still requires a bin name for values that fall outside the bounds, because every extant use
             * case for this setting can still result in some values falling outside the bounds.
             *
             * @param binNameForValuesOutsideBound the name of the bin that will contain values outside this bound
             */
            public static record NoBound(String binNameForValuesOutsideBound) implements BoundSetting {
                // no additional fields
            }
        }
    }

    /**
     * This interface defines how numbers should be formatted in the output bins. It allows for different formatting
     * strategies, such as using a custom format or the default Java formatter.
     */
    public sealed interface NumberFormattingSettings permits ColumnFormat, CustomFormat {

        /** for numbers less than 0.0001. */
        DecimalFormat SMALL_FORMAT = new DecimalFormat("0.00E0", new DecimalFormatSymbols(Locale.US));

        /** in all other cases, use the default Java formatter. */
        NumberFormat DEFAULT_FORMAT = NumberFormat.getNumberInstance(Locale.US);

        /** the threshold where we switch between {@link #SMALL_FORMAT} and {@link #DEFAULT_FORMAT} */
        double SMALL_NUMBER_THRESHOLD = 0.0001;

        /**
         * Apply the formatting settings to the given double value and return the formatted string.
         *
         * @param value the double value to format
         * @return the formatted string representation of the value
         */
        String getFormattedValue(final double value);

        /**
         * The default formatting for numbers in the bins.
         */
        public static record ColumnFormat() implements NumberFormattingSettings {
            // no additional fields

            @Override
            public String getFormattedValue(final double value) {
                if (value == 0.0) { // NOSONAR we actually want exact equality here
                    return "0";
                } else if (Double.isInfinite(value) || Double.isNaN(value)) {
                    return Double.toString(value);
                }

                double abs = Math.abs(value);
                var format = abs < SMALL_NUMBER_THRESHOLD ? SMALL_FORMAT : DEFAULT_FORMAT;
                synchronized (format) {
                    return format.format(value);
                }
            }
        }

        /**
         * This class allows for custom formatting of numbers in the bins, depending on the provided settings.
         *
         * @param settings the settings to use for formatting, such as precision, rounding mode, and number format.
         */
        public static record CustomFormat(NumberFormatSettingsGroup settings) implements NumberFormattingSettings {

            @Override
            public String getFormattedValue(final double value) {
                if (Double.isInfinite(value)) {
                    return value > 0 ? "Infinity" : "-Infinity";
                } else if (Double.isNaN(value)) {
                    return "NaN";
                }

                var bd = BigDecimal.valueOf(value);

                var roundingMode = settings.roundingMode.toJavaRoundingMode();

                bd = switch (settings.precisionMode) {
                    case DECIMAL_PLACES -> bd.setScale(settings.precision, roundingMode);
                    case SIGNIFICANT_FIGURES -> bd.round(new MathContext(settings.precision, roundingMode));
                };

                return switch (settings.numberFormat) {
                    case STANDARD_STRING -> bd.toString();
                    case PLAIN_STRING -> bd.toPlainString();
                    case ENGINEERING_STRING -> bd.toEngineeringString();
                };
            }
        }
    }

    /**
     * This enum defines the behaviour of the binning when a value falls exactly on the boundary between two bins.
     */
    @SuppressWarnings("javadoc")
    public enum BinBoundaryExactMatchBehaviour {
            TO_LOWER_BIN, //
            TO_UPPER_BIN;

        public static final String NAME_TO_LOWER_BIN = "To lower bin";

        public static final String DESC_TO_LOWER_BIN = """
                Values that fall on the bin border will be assigned \
                to the lower bin.
                """;

        public static final String NAME_TO_UPPER_BIN = "To upper bin";

        public static final String DESC_TO_UPPER_BIN = """
                Values that fall on the bin border will be assigned \
                to the upper bin.
                """;
    }

    /**
     * This enum defines the different ways to name bins.
     */
    @SuppressWarnings("javadoc")
    public enum BinNaming {
            NUMBERED, //
            BORDERS, //
            MIDPOINTS;

        public static final String DESC_NUMBERED = "Bins will be named by their number, e.g. Bin 1";

        public static final String DESC_BORDERS = "Bins will be named by their borders, e.g. [0.0, 1.0)";

        public static final String DESC_MIDPOINTS = "Bins will be named by their midpoints, e.g. 0.5";

        public static final String NAME_NUMBERED = "Numbered";

        public static final String NAME_BORDERS = "Borders";

        public static final String NAME_MIDPOINTS = "Midpoints";
    }

    /**
     * This enum defines the different ways to bin data.
     */
    @SuppressWarnings("javadoc")
    public enum BinningType {

            EQUAL_WIDTH, //
            EQUAL_FREQUENCY, //
            CUSTOM_CUTOFFS, //
            CUSTOM_QUANTILES;

        public static final String NAME_EQUAL_WIDTH = "Equal width";

        public static final String DESC_EQUAL_WIDTH = """
                Each bin will have the same width. The number \
                of bins must be specified.
                """;

        public static final String NAME_EQUAL_FREQUENCY = "Equal frequency";

        public static final String DESC_EQUAL_FREQUENCY = """
                Each bin will contain the same number of values \
                (or as close as possible). The number of bins \
                must be specified.
                """;

        public static final String NAME_CUSTOM_CUTOFFS = "Custom cutoffs";

        public static final String DESC_CUSTOM_CUTOFFS = """
                Bins will be created based on a fixed list of \
                cutoffs. The cutoffs are specified as \
                boundaries, i.e. the values that define \
                the bin edges. At least two cutoffs must \
                be provided.
                """;

        public static final String NAME_CUSTOM_QUANTILES = "Custom quantiles";

        public static final String DESC_CUSTOM_QUANTILES = """
                Bins will be created based on a fixed list of \
                quantiles. The quantiles are specified as \
                boundaries, i.e. the values that define \
                the bin edges. At least two quantiles must \
                be provided.

                The quantiles will be converted to bin edges \
                using the R-7 algorithm, see <a href="
                """ + QUANTILE_URL + """
                ">WP:Quantile</a> for more details.
                """;

    }

    private static final String QUANTILE_URL = "https://en.wikipedia.org/wiki/Quantile";

    /**
     * This class holds the settings for number formatting in the bins. This class implements
     * {@link DefaultNodeSettings} which means it can be used as a widget group in a node dialog.
     *
     * @param numberFormat the format to use for numbers in the bins, e.g. standard, plain or engineering.
     * @param precision the precision to use for rounding numbers in the bins, e.g. number of decimal places or
     *            significant figures.
     * @param precisionMode the mode of precision to use for rounding numbers in the bins, e.g. decimal places or
     *            significant figures.
     * @param roundingMode the rounding direction to use for rounding numbers in the bins, e.g. up, down, ceiling,
     *            floor, half up, half down or half even.
     */
    @SuppressWarnings("javadoc")
    public static record NumberFormatSettingsGroup( //
        NumberFormat numberFormat, //
        int precision, //
        PrecisionMode precisionMode, //
        RoundingDirection roundingMode //
    ) {

        /**
         * The formatting of numbers in the bins.
         */
        public enum NumberFormat {

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
         * If numbers are to be rounded, this enum defines the direction of rounding.
         */
        public enum RoundingDirection {

                UP, //
                DOWN, //
                CEILING, //
                FLOOR, //
                HALF_UP, //
                HALF_DOWN, //
                HALF_EVEN;

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

            final RoundingMode m_roundingMode;

            RoundingDirection() {
                m_roundingMode = RoundingMode.valueOf(name());
            }

            /**
             * Convert the rounding direction to a Java {@link RoundingMode}, which can be used directly by e.g.
             * {@link BigDecimal}.
             *
             * @return the corresponding Java RoundingMode
             */
            public RoundingMode toJavaRoundingMode() {
                return m_roundingMode;
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
    }
}
