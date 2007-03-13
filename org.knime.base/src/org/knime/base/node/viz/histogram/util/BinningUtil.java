/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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

import org.knime.base.node.viz.histogram.AbstractHistogramPlotter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;

/**
 * This class provides methods to create the bins of numerical bars.
 * @author Tobias Koetter, University of Konstanz
 */
public final class BinningUtil {

    private BinningUtil() {
        //prevent object creation for utility class
    }
   
    /**
     * @param maxVal the maximum possible value
     * @param minVal the minimum possible value
     * @param noOfBars the number of bars
     * @param isInteger <code>true</code> if the value should be an integer
     * @return the interval for the given min, max value and no of bars
     */
    public static double createBinInterval(final double maxVal,
            final double minVal, final int noOfBars,
            final boolean isInteger) {
        if (maxVal < minVal) {
            throw new IllegalArgumentException(
                    "Max value should be at least as big as min value.");
        }
        if (noOfBars == 0) {
            return maxVal - minVal;
        }
        double interval = (maxVal - minVal) / noOfBars;
        if (interval > 10) {
                // find the next higher number divided by ten.
                interval = bigValueRounder(interval, true);
        } else {
            interval = smallValueRounder(interval, 
                    AbstractHistogramPlotter.INTERVAL_DIGITS, isInteger, true);
        }
        return interval;
    }

    /**
     * Called to calculate the left start value of the binning.
     * If the minVal is is greater then 0 it returns 0 if
     * minVal - interval <= 0 otherwise it rounds the given minVal.
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
        } else {
            result = bigValueRounder(minVal, false);
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
     * @param firstBar indicates if this is the first bar
     * @param leftBoundary the left boundary of the bin
     * @param rightBoundary the right boundary of the bin
     * @param isInteger indicates that the x column is of type integer so we
     * display simply the upper value if its in a range of one
     * @return the bin name
     */
    public static String createBarName(final boolean firstBar,
            final double leftBoundary, final double rightBoundary, 
            final boolean isInteger) {
        StringBuffer buf = new StringBuffer();
        //treat integer intervals special if they are only the size of one
        if (!firstBar && isInteger && rightBoundary - leftBoundary <= 1) {
            return Integer.toString((int)rightBoundary);
        }
        //append the interval border indicator
        if (firstBar) {
            //closed interval -> border is included
            buf.append("[");
        } else {
            //open interval -> border is excluded
            buf.append("(");
        }
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
        //the interval is always closed -> border is included
        buf.append("]");
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
        long factor = (long)Math.pow(10, digitCounter);

        // Shift the decimal the correct number of places
        // to the right.
        final double val = doubleVal * factor;
        double tmp;
        if (doubleVal < 0) {
        // Round to the nearest integer.
            tmp = Math.floor(val);
        } else {
            tmp = Math.ceil(val);
        }

        // Shift the decimal the correct number of places
        // back to the left.
        return tmp / factor;
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
    public static double smallValueRounder(final double doubleVal,
            final int noOfDigits, final boolean isInteger, 
            final boolean roundUp) {
        // if the value is >= 1 or an integer return an interval without decimal
        // places
        final double absVal = Math.abs(doubleVal);
        if (absVal >= 1) {
            return bigValueRounder(doubleVal, roundUp);
        }
        //it's an integer and less then one
        if (isInteger) {
            return 1;
        }
    // the given doubleVal is less then one and no integer
        char[] interval = Double.toString(doubleVal).toCharArray();
        boolean digitFound = false;
        int digitCounter = 0;
        int positionCounter = 0;
        for (int length = interval.length; positionCounter < length
                && digitCounter <= noOfDigits; positionCounter++) {
            char c = interval[positionCounter];
            if (c == '-' || c == '.') {
                //ignore negative sign
                continue;
            } else if (c != '0' || digitFound) {
                    digitFound = true;
                    digitCounter++;
            }
        }
        long factor = (long)Math.pow(10, digitCounter);

        // Shift the decimal the correct number of places
        // to the right.
        final double val = doubleVal * factor;
        double tmp;
        if (roundUp) {
            tmp = Math.ceil(val);
        } else {
            tmp = Math.floor(val);
        }

        // Shift the decimal the correct number of places
        // back to the left.
        return tmp / factor;
    }

    /**
     * Rounds the given number in steps if it is bigger than 20. For values
     * less than 1 it uses the {@link smallValueRounder} method.
     * 
     * @param value the value to round
     * @return the rounded value which is >= the given value and looks nicer :-)
     */
    private static double bigValueRounder(final double value, 
            final boolean roundUp) {
        double divider = 1;
        double addition = 1;
        final double absVal = Math.abs(value);
        if (absVal < 1) {
            return smallValueRounder(value, 
                    AbstractHistogramPlotter.INTERVAL_DIGITS, false, roundUp);
        } else if (absVal > 20 && absVal <= 50) {
          divider = 20;
          addition = 2;
        } else if (absVal > 50 && absVal <= 100) {
            divider = 50;
            addition = 5;
        } else if (absVal > 100 && absVal <= 500) {
            divider = 100;
            addition = 10;
        } else if (absVal > 500 && absVal <= 1000) {
            divider = 500;
            addition = 50;
        } else if (absVal > 1000 && absVal <= 100000) {
            divider = 1000;
            addition = 100;
        } else if (absVal > 100000) {
            divider = 100000;
            addition = 1000;
            while ((absVal / 10) > divider) {
                divider *= 10;
                addition *= 10;
            }
        }
        while (absVal / divider > 1) {
            divider += addition;
        }
        if (!roundUp && value - divider != 0 && value > 0) {
            //if we shouldn't not round up but round down subtract
            //the last addition to get the next smaller value
            divider -= addition;
        }
        if (value < 0) {
            return divider * -1;
        }
        return divider;
    }
}
