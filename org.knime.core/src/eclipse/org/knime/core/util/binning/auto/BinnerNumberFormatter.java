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
 *   Jun 01, 2018 (Mor Kalla): created
 */
package org.knime.core.util.binning.auto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Binner formatter for database auto binner node.
 *
 * @author Mor Kalla
 * @since 3.6
 */
final class BinnerNumberFormatter {

    private static final double THRESHOLD_SMALL_FORMAT = 0.0001;

    private static final DecimalFormat SMALL_FORMAT = new DecimalFormat("0.00E0", new DecimalFormatSymbols(Locale.US));

    private static final NumberFormat DEFAULT_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    /**
     * Formats the double to a string. It will use the following either the format {@code 0.00E0} for numbers less than
     * predefined value or the default NumberFormat.
     *
     * @param value the double to format
     * @param settings the {@link AutoBinnerLearnSettings} object
     * @return the string representation of the double variable
     */
    public static String format(final double value, final AutoBinnerLearnSettings settings) {
        if (settings.getAdvancedFormatting()) {
            return advancedFormat(value, settings);
        } else {
            if (value == 0.0) {
                return "0";
            }
            if (Double.isInfinite(value) || Double.isNaN(value)) {
                return Double.toString(value);
            }
            final NumberFormat format = isSmallFormat(Math.abs(value)) ? SMALL_FORMAT : DEFAULT_FORMAT;
            synchronized (format) {
                return format.format(value);
            }
        }
    }

    private static boolean isSmallFormat(final double abs) {
        return abs < THRESHOLD_SMALL_FORMAT;
    }

    private static String advancedFormat(final double value, final AutoBinnerLearnSettings settings) {
        final BigDecimal decimalValue = getBigDecimalValue(value, settings);

        switch (settings.getOutputFormat()) {
            case STANDARD:
                return decimalValue.toString();
            case PLAIN:
                return decimalValue.toPlainString();
            case ENGINEERING:
                return decimalValue.toEngineeringString();
            default:
                return Double.toString(decimalValue.doubleValue());
        }
    }

    private static BigDecimal getBigDecimalValue(final double value, final AutoBinnerLearnSettings settings) {
        BigDecimal decimalValue = new BigDecimal(value);
        final PrecisionMode precisionMode = settings.getPrecisionMode();

        if (precisionMode == PrecisionMode.DECIMAL) {
            decimalValue = decimalValue.setScale(settings.getPrecision(), settings.getRoundingMode());
        } else if (precisionMode == PrecisionMode.SIGNIFICANT) {
            decimalValue = decimalValue.round(new MathContext(settings.getPrecision(), settings.getRoundingMode()));
        }
        return decimalValue;
    }

    private BinnerNumberFormatter() {

    }

}
