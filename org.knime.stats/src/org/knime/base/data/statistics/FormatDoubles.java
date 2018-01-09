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
 * Created on 2014.03.05. by Gabor Bakos
 */
package org.knime.base.data.statistics;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

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
        nf.setMinimumIntegerDigits(1);
        return nf;
    }

    /** for numbers less than 0.0001. */
    public static final DecimalFormat SMALL_FORMAT = new DecimalFormat("0.000E0",
        DecimalFormatSymbols.getInstance(Locale.US));

    /** for numbers in (0.0001, 10]. */
    public static final DecimalFormat NORMAL_FORMAT = new DecimalFormat("##0.####",
        DecimalFormatSymbols.getInstance(Locale.US));

    /** for numbers in (10, 10'000'000). */
    public static final DecimalFormat LARGE_FORMAT = new DecimalFormat("###,###,##0.###",
        DecimalFormatSymbols.getInstance(Locale.US));

    /** for numbers larger than 10'000'000. */
    public static final DecimalFormat VERY_LARGE_FORMAT = new DecimalFormat("0.000E0",
        DecimalFormatSymbols.getInstance(Locale.US));

    /**
     * Selects the {@link NumberFormatter} appropriate for the number.
     *
     * @param d The number.
     * @return The {@link NumberFormat} appropriate to format it.
     */
    public NumberFormat formatterForNumber(final double d) {
        double abs = Math.abs(d);
        if (abs < 1e-3) {
            return SMALL_FORMAT;
        }
        if (abs < 10) {
            return NORMAL_FORMAT;
        }
        if (abs < 1e7) {
            return LARGE_FORMAT;
        }
        return VERY_LARGE_FORMAT;
    }

    /**
     * @param in Different double numbers, but none of them {@link Double#isInfinite() infinite} or {@link Double#NaN}.
     * @return A {@link NumberFormat} for the numbers which usually makes them distinct.
     */
    public final NumberFormat formatterWithSamePrecisio(final double... in) {
        if (in.length == 1) {
            return formatterForNumber(in[0]);
        }
        if (in.length == 0) {
            return NORMAL_FORMAT;
        }
        double maxAbs = 0;
        for (double d : in) {
            maxAbs = Math.max(d, maxAbs);
        }
        if (maxAbs > 1E8) {
            Set<String> unique = new LinkedHashSet<String>();
            DecimalFormat ret = new DecimalFormat();
            for (int i = 0; i < 10 && unique.size() != in.length; i++) {
                unique.clear();
                StringBuilder formatBuilder = new StringBuilder("0");
                for (int p = 0; p < i; p++) {
                    formatBuilder.append(p == 0 ? "." : "");
                    formatBuilder.append("0");
                }
                ret = new DecimalFormat(formatBuilder.toString() + "E0");
                for (double d : in) {
                    unique.add(ret.format(d));
                }
            }
            //We can add additional 0 if preferred, but probably not necessary.
            //Not null
            ret.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
            return ret;
        }
        double minDiff = Double.MAX_VALUE;
        for (int i = in.length - 1; i-- > 0;) {
            for (int j = in.length; j-- > i + 1;) {
                minDiff = Math.min(minDiff, Math.abs(in[i] - in[j]));
            }
        }
        if (minDiff <= Double.MIN_VALUE) {
            //TODO There are identical numbers, probably throw exception?
            return NORMAL_FORMAT;
        }
        //A less than 1, but close to it number.
        double current = .875;
        StringBuilder formatBuilder = new StringBuilder("#,##0");
        if (minDiff < current) {
            formatBuilder.append('.');
        }
        for (int i = 0; i < 10 && minDiff < current; i++) {
            formatBuilder.append('#');
            current /= 10d;
        }
        if (current > minDiff) {
            formatBuilder.append('#');
        }
        final DecimalFormat best = new DecimalFormat(formatBuilder.toString());
        best.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
        if (current > minDiff) {
            //This case can produce identical numbers
            return SMALL_FORMAT;
        }
        return best;
    }

    private static final String[] formatWithSamePrecision(final double[] in) {
        Set<String> unique = new LinkedHashSet<String>();
        // over the number of decimal digits, start with none, then add more until string values are unique.
        for (int i = 0; i < 10 && unique.size() != in.length; i++) {
            unique.clear();
            StringBuilder formatBuilder = new StringBuilder("#,##0");
            for (int p = 0; p < i; p++) {
                formatBuilder.append(p == 0 ? "." : "");
                formatBuilder.append("0");
            }
            DecimalFormat ret = new DecimalFormat(formatBuilder.toString());
            for (double d : in) {
                unique.add(ret.format(d));
            }
        }
        return unique.toArray(new String[unique.size()]);
    }

    /**
     * Selects the {@link NumberFormatter} appropriate for all the numbers with minimal number of decimal digits. (Like
     * having the same decimal digits to use.)
     *
     * @param ds The numbers.
     * @return The {@link NumberFormat} appropriate to format them.
     */
    public NumberFormat minimalFormatterForNumbers(final double... ds) {
        NumberFormat[] options = new NumberFormat[]{LARGE_FORMAT, SMALL_FORMAT};
        int[][] lengths = new int[options.length][ds.length];
        for (int i = 0; i < ds.length; i++) {
            for (int j = 0; j < options.length; j++) {
                lengths[j][i] = options[j].format(ds[i]).length();
            }
        }
        int[] sums = new int[options.length];
        for (int i = 0; i < sums.length; ++i) {
            for (int j = 0; j < ds.length; ++j) {
                sums[i] += lengths[i][j];
            }
        }
        NumberFormat min = options[0];
        int minSum = sums[0];
        for (int i = 1; i < options.length; ++i) {
            if (sums[i] < minSum) {
                minSum = sums[i];
                min = options[i];
            }
        }
        return min;
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
                decimal[i] = 10;
                for (int t = POWERS_OF_TEN.length; t-- > 0;) {
                    double mult = d * POWERS_OF_TEN[t];
                    if (Math.abs(Math.round(mult) - mult) < 1E-9) {
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

    private static void debug(final double... in) {
        System.out.println(String.format("In: %s, Out: %s Alt: %s", Arrays.toString(in),
            Arrays.toString(formatWithSamePrecision(in)),
            Arrays.toString(apply(new FormatDoubles().formatterWithSamePrecisio(in), in))));
    }

    private static String[] apply(final NumberFormat format, final double... in) {
        String[] ret = new String[in.length];
        for (int i = in.length; i-- > 0;) {
            ret[i] = format.format(in[i]);
        }
        return ret;
    }

    /**
     * Test program to check {@link #formatterWithSamePrecisio(double...)}
     *
     * @param args Not used.
     */
    public static void main(final String[] args) {
        double[] in = new double[]{0.000343, 0.999134};
        debug(in);
        in = new double[]{1.000343, 1.999134};
        debug(in);
        in = new double[]{0.9563, 0.999134};
        debug(in);
        in = new double[]{0.9443, 0.999134};
        debug(in);
        in = new double[]{193.3, 393};
        debug(in);
        in = new double[]{10103193.3, 100000003};
        debug(in);
        in = new double[]{.3, .7};
        debug(in);
        in = new double[]{2.3E-24, 7.3E-23};
        debug(in);
        in = new double[]{2.33233332E23, 2.332233232E23};
        debug(in);
        in = new double[]{2E23, 3E23};
        debug(in);
        in = new double[]{3, 3};
        debug(in);
    }
}