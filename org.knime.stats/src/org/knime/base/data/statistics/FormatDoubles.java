/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 * ---------------------------------------------------------------------
 *
 * Created on 2014.03.05. by Gabor Bakos
 */
package org.knime.base.data.statistics;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.text.NumberFormatter;

/**
 * This is a helper class to find a consistent (and culture independent) {@link NumberFormat} for some numbers.
 *
 * @author Gabor Bakos
 * @since 2.10
 */
public class FormatDoubles {
    /**
     * Selects the {@link NumberFormatter} appropriate for all the numbers. (Like having the same decimal digits to
     * use.)
     *
     * @param ds The numbers.
     * @return The {@link NumberFormat} appropriate to format them.
     */
    public NumberFormat formatterForNumbers(final double... ds) {
        int[] highestDigits = highestDigits(ds);
        int[] decimalDigits = decimalDigits(ds);
        int highestDigit = max(highestDigits);
        int decimalDigit = max(decimalDigits);
        if (highestDigit >= 9) {
            DecimalFormat ret = new DecimalFormat("0.000E0");
            ret.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
            return ret;
        }
        NumberFormat nf = (NumberFormat)NumberFormat.getNumberInstance(Locale.ROOT).clone();
        nf.setMinimumFractionDigits(decimalDigit);
        nf.setMaximumFractionDigits(decimalDigit);
        nf.setMaximumIntegerDigits(highestDigit);
        return nf;
    }

    private static double[] POWERS_OF_TEN = new double[]{10d, 100d, 1000d, 1E4, 1E5, 1E6, 1E7, 1E8, 1E9};

    /**
     * @param ds The numbers to test.
     * @return The significant decimal digits after the decimal separator (at most 10, can be {@link Integer#MIN_VALUE}
     *         when there are inifinite or NaN values).
     */
    private int[] decimalDigits(final double[] ds) {
        final int[] decimal = new int[ds.length];
        for (int i = ds.length; i-- > 0;) {
            final double d = ds[i];
            if (Double.isInfinite(d) || Double.isNaN(d)) {
                decimal[i] = Integer.MIN_VALUE;
                continue;
            }
            if (Math.abs(Math.round(d) - d) < 1E-10) {
                decimal[i] = 0;
            } else {
                for (int t = POWERS_OF_TEN.length; t-- > 0;) {
                    double mult = d * POWERS_OF_TEN[t];
                    if (Math.abs(Math.round(mult) - mult) < 1E-10) {
                        decimal[i] = t + 1;
                    }
                }
            }
        }
        return decimal;
    }

    /**
     * @param ds The numbers to test.
     * @return The position of highest decimal digits after the decimal separator (can be {@link Integer#MIN_VALUE} when
     *         there are inifinite or NaN values).
     */
    private int[] highestDigits(final double... ds) {
        final int[] highest = new int[ds.length];
        for (int i = ds.length; i-- > 0;) {
            final double d = ds[i];
            if (Double.isInfinite(d) || Double.isNaN(d)) {
                highest[i] = Integer.MIN_VALUE;
                continue;
            }
            highest[i] = 1 + (int)Math.floor(Math.log10(Math.abs(d)));
        }
        return highest;
    }

    /**
     * Finds the maximal value of {@code value}.
     *
     * @param values Some numbers.
     * @return The maximum, or {@value Integer#MIN_VALUE} if no values were passed.
     */
    private static int max(final int... values) {
        int max = Integer.MIN_VALUE;
        for (int v : values) {
            max = Math.max(max, v);
        }
        return max;
    }
}
