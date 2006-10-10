/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *    10.10.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.util;

import java.text.DecimalFormat;

import org.knime.base.node.viz.histogram.AbstractHistogramDataModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public final class BinningUtil {

    private BinningUtil() {
        //prevent object creation for util class
    }
   
    /**
     * @param maxVal the maximum possible value
     * @param minVal the minimum possible value
     * @param noOfBars the number of bars
     * @param colSpec the specification of the column for whom we want the
     *            interval
     * @return the interval for the given min, max value and no of bars
     */
    public static double createBinInterval(final double maxVal,
            final double minVal, final int noOfBars,
            final DataColumnSpec colSpec) {
        double interval = (maxVal - minVal) / noOfBars;
        // if the column is of type integer we don't need to have an interval
        // with decimal places
        if (interval > 10) {
                // find the next higher number divided by ten.
                interval = bigValueRounder(interval);
        } else {
            interval = smallValueRounder(interval, 
                    AbstractHistogramDataModel.INTERVAL_DIGITS, 
                    colSpec.getType().isCompatible(IntValue.class));
        }
        return interval;
    }

    /**
     * Called to calculate the left start value of the binning.
     * @param minVal the minimum value to round
     * @param binInterval the bin interval to check if we can start with 0
     * @return the left start value of the binning interval
     */
    public static double createBinStart(final double minVal, 
            final double binInterval) {
        double result = minVal;
        if (minVal >= 0 && minVal - binInterval <= 0) {
            // try to start with 0 as left border to have nicer intervals
            //but only if the minimum value is bigger then 0
            result = 0;
        } else if (minVal < 0) {
            result = bigValueRounder(Math.abs(minVal));
            result *= -1;
        } else if (minVal > 0) {
            result = bigValueRounder(minVal);
        }
        return result;
    }

    /**
     * Checks if the given <code>DataCell</code> is a numeric cell and returns
     * the numeric value. If it's not a numeric cell it throws an
     * <code>IllegalArgumentException</code>.
     * 
     * @param cell the cell to convert
     * @return the numeric value of the given cell
     */
    public static double getNumericValue(final DataCell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("Cell shouldn't be null.");
        }
        if (!(cell.getType().isCompatible(DoubleValue.class))) {
            throw new IllegalArgumentException("Cell type not "
                    + "compatible with numeric data type.");
        }
        double val = ((DoubleValue)cell).getDoubleValue();
        return val;
    }

    /**
     * Creates the name of the bin depending on the given boundaries.
     * 
     * @param leftBoundary the left boundary of the bin
     * @param rightBoundary the right boundary of the bin
     * @return the bin name
     */
    public static String createBarName(final double leftBoundary,
            final double rightBoundary) {
        StringBuffer buf = new StringBuffer();
        if ((int)leftBoundary == leftBoundary) {
            buf.append((int)leftBoundary);
        } else {
            buf.append(Double.toString(leftBoundary));
        }
        buf.append(" - ");
        if ((int)rightBoundary == rightBoundary) {
            buf.append((int)rightBoundary);
        } else {
            buf.append(Double.toString(rightBoundary));
        }
        return buf.toString();
    }
    /**
     * Returns the rounded value which contains the given number of decimal
     * places after the last 0 in the given increment.
     * 
     * @param doubleVal the value to round
     * @param increment the increment which defines the start index of the digit
     *            counter
     * @param noOfDigits the number of decimal places to display
     * @return the rounded value
     */
    public static double myRoundedBorders(final double doubleVal,
            final double increment, final int noOfDigits) {
        char[] incrementString = Double.toString(increment).toCharArray();
        StringBuffer decimalFormatBuf = new StringBuffer();
        boolean digitFound = false;
        int digitCounter = 0;
        int positionCounter = 0;
        for (int length = incrementString.length; positionCounter < length
                && digitCounter <= noOfDigits; positionCounter++) {
            char c = incrementString[positionCounter];
            if (c == '.') {
                decimalFormatBuf.append(".");
            } else {
                if (c != '0' || digitFound) {
                    digitFound = true;
                    digitCounter++;
                }
                if (digitCounter <= noOfDigits) {
                    decimalFormatBuf.append("#");
                }
            }
        }
        DecimalFormat df = new DecimalFormat(decimalFormatBuf.toString());
        String resultString = df.format(doubleVal);
        double result = Double.parseDouble(resultString);
        return result;
    }

    /**
     * Returns the rounded value. If the value is bigger or equal 1 it returns
     * the result of the {@link bigValueRounder} method otherwise it returns 
     * the rounded value which contains the given number of decimal places 
     * after the last 0.
     * 
     * @param doubleVal the value to round 
     * @param noOfDigits the number of
     * decimal places we want for less then 1 values 
     * @param isInteger <code>true</code> if the given number is an integer
     * @return the rounded value of the given value
     */
    private static double smallValueRounder(final double doubleVal,
            final int noOfDigits, final boolean isInteger) {
        // if the value is >= 1 or an integer return an interval without decimal
        // places
        if (doubleVal >= 1 || isInteger) {
            return bigValueRounder(doubleVal);
        } else {
            // the given doubleVal is less then zero
            char[] interval = Double.toString(doubleVal).toCharArray();
            StringBuffer decimalFormatBuf = new StringBuffer();
            boolean digitFound = false;
            int digitCounter = 0;
            int positionCounter = 0;
            for (int length = interval.length; positionCounter < length
                    && digitCounter <= noOfDigits; positionCounter++) {
                char c = interval[positionCounter];
                if (c == '.') {
                    decimalFormatBuf.append(".");
                } else {
                    if (c != '0' || digitFound) {
                        digitFound = true;
                        digitCounter++;
                    }
                    if (digitCounter <= noOfDigits) {
                        decimalFormatBuf.append("#");
                    }
                }
            }
            double result = Double.parseDouble(new String(interval, 0,
                    positionCounter));
            DecimalFormat df = new DecimalFormat(decimalFormatBuf.toString());
            String resultString = df.format(result);
            result = Double.parseDouble(resultString);
            return result;
        }
    }

    /**
     * Rounds the given number in steps if it is bigger than 20. For values
     * less than 1 it uses the {@link smallValueRounder} method.
     * 
     * @param value the value to round
     * @return the rounded value which is >= the given value and looks nicer :-)
     */
    private static double bigValueRounder(final double value) {
        double divider = 1;
        double addition = 1;
        if (value < 1) {
            return smallValueRounder(value, 
                    AbstractHistogramDataModel.INTERVAL_DIGITS, false);
        } else if (value > 20 && value <= 50) {
          divider = 20;
          addition = 2;
        } else if (value > 50 && value <= 100) {
            divider = 50;
            addition = 5;
        } else if (value > 100 && value <= 500) {
            divider = 100;
            addition = 10;
        } else if (value > 500 && value <= 1000) {
            divider = 500;
            addition = 50;
        } else if (value > 1000 && value <= 100000) {
            divider = 1000;
            addition = 100;
        } else if (value > 100000) {
            divider = 100000;
            addition = 1000;
            while ((value / 10) > divider) {
                divider *= 10;
                addition *= 10;
            }
        }
        while (value / divider > 1) {
            divider += addition;
        }
        return divider;
    }
}
