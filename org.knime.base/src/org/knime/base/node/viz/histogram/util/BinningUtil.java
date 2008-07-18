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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.BinDataModel;
import org.knime.base.node.viz.histogram.datamodel.BinDataModelComparator;
import org.knime.base.node.viz.histogram.datamodel.InteractiveBinDataModel;
import org.knime.base.node.viz.histogram.impl.AbstractHistogramPlotter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.IntCell;

/**
 * This class provides methods to create the bins of numerical bars.
 * @author Tobias Koetter, University of Konstanz
 */
public final class BinningUtil {

    /**Compare the caption of bins.*/
    private static final BinDataModelComparator BIN_CAPTION_COMPARATOR =
        new BinDataModelComparator(BinDataModelComparator.COMPARE_CAPTION);

    private BinningUtil() {
        //prevent object creation for utility class
    }

    /**
     * @param maxVal the maximum possible value
     * @param minVal the minimum possible value
     * @param noOfBins the number of bars
     * @param isInteger <code>true</code> if the value should be an integer
     * @return the interval for the given min, max value and no of bars
     */
    public static double createBinInterval(final double maxVal,
            final double minVal, final int noOfBins,
            final boolean isInteger) {
        if (maxVal < minVal) {
            throw new IllegalArgumentException(
                    "Max value should be at least as big as min value.");
        }
        if (noOfBins == 0) {
            return maxVal - minVal;
        }
        double interval = (maxVal - minVal) / noOfBins;
        if (interval > 10) {
                // find the next higher number divided by ten.
                interval = bigValueRounder(interval, true);
        } else {
            interval = smallValueRounder(interval,
                    AbstractHistogramPlotter.INTERVAL_DIGITS, isInteger, true);
        }
        if (isInteger && interval == 1 && (maxVal - minVal) >= noOfBins) {
            interval = 2;
        }
        return interval;
    }

    /**
     * Called to calculate the left start value of the binning.
     * If the minVal is greater then 0 it returns 0 if the x column is
     * an integer and minVal - interval <= 0 otherwise it rounds the
     * given minVal.
     * @param minVal the minimum value to round
     * @param binInterval the bin interval to check if we can start with 0
     * @param isInteger <code>true</code> if the x column is an integer
     * @return the left start value of the binning interval
     */
    public static double createBinStart(final double minVal,
            final double binInterval, final boolean isInteger) {
        double result = minVal;
        if ((!isInteger || binInterval > 1) && minVal >= 0
                && minVal - binInterval <= 0) {
            // try to start with 0 as left border to have nicer intervals
            //but only if the x column is not an integer,
            //the minimum value is bigger then 0 and 0 is in the interval
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
            throw new IllegalArgumentException("Cell must not be null.");
        }
        if (!(cell.getType().isCompatible(DoubleValue.class))) {
            throw new IllegalArgumentException("Cell type not "
                    + "compatible with numeric data type.");
        }
        final double val = ((DoubleValue)cell).getDoubleValue();
        return val;
    }

    /**
     * Creates the name of the bin depending on the given boundaries.
     *
     * @param firstBar indicates if this is the first bar
     * @param leftBoundary the left boundary of the bin
     * @param rightBoundary the right boundary of the bin
     * @return the bin name
     */
    public static String createBarName(final boolean firstBar,
            final double leftBoundary, final double rightBoundary) {
        final StringBuffer buf = new StringBuffer();
        //treat integer intervals special if they are only the size of one
        if (leftBoundary == rightBoundary) {
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
        if (increment <= 0) {
            throw new IllegalArgumentException("Increment should be positive");
        }
//        boolean dotFound = false;
        int firstDigitCounter = 0;
//        int positionCounter = 0;
        int currentVal = 0;
        while (currentVal == 0) {
            firstDigitCounter++;
            currentVal = (int)(increment * Math.pow(10, firstDigitCounter));
        }
//        for (int length = incrementString.length; positionCounter < length;
//            positionCounter++) {
//            char c = incrementString[positionCounter];
//            if (c == '.') {
//                dotFound = true;
//            } else if (dotFound) {
//                if (c != '0') {
//                    firstDigitCounter++;
//                    break;
//                }
//                firstDigitCounter++;
//            }
//        }
        //add one more digit since we call round up the last number
        final int digits = Math.max(noOfDigits, firstDigitCounter) + 1;
        final long factor = (long)Math.pow(10, digits);

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
     * Returns the rounded value. If the value is bigger or equal 10 it returns
     * the result of the <code>bigValueRounder(double, boolean)</code>
     * method otherwise it returns the rounded value which contains the
     * given number of decimal places after the last 0.
     *
     * @param doubleVal the value to round
     * @param noOfDigits the number of
     * decimal places we want for less then 1 values
     * @param isInteger <code>true</code> if the given number is an integer
     * @param roundUp if the value should be rounded up
     * @return the rounded value of the given value
     */
    public static double smallValueRounder(final double doubleVal,
            final int noOfDigits, final boolean isInteger,
            final boolean roundUp) {
        // if the value is >= 1 or an integer return an interval without decimal
        // places
        final double absVal = Math.abs(doubleVal);
        if (absVal >= 10) {
            return bigValueRounder(doubleVal, roundUp);
        }
        //it's an integer and less then one
        if (isInteger) {
            return Math.ceil(absVal);
        }
    // the given doubleVal is less then one and no integer
        final char[] interval = Double.toString(doubleVal).toCharArray();
        boolean digitFound = false;
        int digitCounter = 0;
        int positionCounter = 0;
        for (final int length = interval.length; positionCounter < length
                && digitCounter <= noOfDigits; positionCounter++) {
            final char c = interval[positionCounter];
            if (c == '-' || c == '.') {
                //ignore negative sign
                continue;
            } else if (c != '0' || digitFound) {
                    digitFound = true;
                    digitCounter++;
            }
        }
        final long factor = (long)Math.pow(10, digitCounter);

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

    /**
     * Creates interactive nominal bins for the given column specification.
     * @param colSpec the column specification
     * @return the interactive bins
     */
    public static List<InteractiveBinDataModel> createInteractiveNominalBins(
                final DataColumnSpec colSpec) {
        final Set<DataCell> values = getBinDataCells(colSpec);
        final List<InteractiveBinDataModel> bins =
            new ArrayList<InteractiveBinDataModel>(values.size());
        for (final DataCell value : values) {
            bins.add(new InteractiveBinDataModel(value.toString(), 0, 0));
        }
        //sort the bins by their caption
        Collections.sort(bins, BinningUtil.BIN_CAPTION_COMPARATOR);
        return bins;
    }

    /**
     * Creates interactive nominal bins for the given column specification.
     * @param colSpec the column specification
     * @return the interactive bins
     */
    public static List<BinDataModel> createNominalBins(
                final DataColumnSpec colSpec) {
        final Set<DataCell> values = getBinDataCells(colSpec);
        final List<BinDataModel> bins =
            new ArrayList<BinDataModel>(values.size());
        for (final DataCell value : values) {
            bins.add(new BinDataModel(value.toString(), 0, 0));
        }
        //sort the bins by their caption
        Collections.sort(bins, BinningUtil.BIN_CAPTION_COMPARATOR);
        return bins;
    }

    private static Set<DataCell> getBinDataCells(final DataColumnSpec colSpec) {
        final DataColumnDomain domain = colSpec.getDomain();
        final Set<DataCell> values;
        //check if we have the values
        if (domain.getValues() == null) {
            //check if it's an integer
//            if (!colSpec.getType().isCompatible(IntValue.class)) {
                throw new IllegalArgumentException(
                    "No domain values defined for nominal binning column. "
                    + "Please use DomainCalculator or ColumnFilter node "
                    + "to set the domain values.");
//            }
//            final int lowerBound =
//                ((IntValue)domain.getLowerBound()).getIntValue();
//            final int upperBound =
//                ((IntValue)domain.getUpperBound()).getIntValue();
//            values =
//                new LinkedHashSet<DataCell>(upperBound - lowerBound + 1);
//            for (int i = lowerBound; i <= upperBound; i++) {
//                values.add(new IntCell(i));
//            }
        }
//        else {
            values = colSpec.getDomain().getValues();
//        }
        return values;
    }

    /**
     * Creates the given number of interval bins for the given
     * column specification.
     * @param colSpec the column specification
     * @param numberOfBins the number of bins to create
     * @return the created bins
     */
    public static List<InteractiveBinDataModel> createInteractiveIntervalBins(
                final DataColumnSpec colSpec, final int numberOfBins) {
            //set the bounds for binning
            final DataColumnDomain domain = colSpec.getDomain();
            final DataCell lowerBoundCell = domain.getLowerBound();
            if (lowerBoundCell == null || lowerBoundCell.isMissing()
                    || !lowerBoundCell.getType().isCompatible(
                            DoubleValue.class)) {
                throw new IllegalArgumentException(
                        "The lower bound of the binning column domain "
                        + "should be defined");
            }
            final double lowerBound =
                ((DoubleValue)lowerBoundCell).getDoubleValue();
            final DataCell upperBoundCell = domain.getUpperBound();
            if (upperBoundCell == null || upperBoundCell.isMissing()
                    || !upperBoundCell.getType().isCompatible(
                            DoubleValue.class)) {
                throw new IllegalArgumentException(
                        "The upper bound of the binning column domain "
                        + "should be defined");
            }
            final double upperBound =
                ((DoubleValue)upperBoundCell).getDoubleValue();
            int noOfBins = numberOfBins;
            //start the binning
            if (noOfBins < 1) {
                noOfBins = AbstractHistogramVizModel.DEFAULT_NO_OF_BINS;
            }
            if ((lowerBound - upperBound) == 0) {
                noOfBins = 1;
            }
            final boolean isInteger =
                colSpec.getType().isCompatible(IntValue.class);
            double binInterval = createBinInterval(upperBound,
                    lowerBound, noOfBins, isInteger);
            final double calculatedLowerBound =
                createBinStart(lowerBound, binInterval, isInteger);
            if (calculatedLowerBound != lowerBound) {
                binInterval = createBinInterval(upperBound,
                        calculatedLowerBound, noOfBins, isInteger);
            }
            // increase the number of bars to include the max value
            while (calculatedLowerBound + (binInterval * noOfBins)
                    < upperBound) {
                noOfBins++;
            }
            double leftBoundary = myRoundedBorders(calculatedLowerBound,
                    binInterval, AbstractHistogramVizModel.INTERVAL_DIGITS);
            boolean firstBar = true;
            final List<InteractiveBinDataModel> bins =
                new ArrayList<InteractiveBinDataModel>(noOfBins);
            for (int i = 0; i < noOfBins; i++) {
        // I have to use this rounding method to avoid problems with very
        // small intervals. If the interval is very small it could happen
        // that we get the same boundaries for several bars by rounding the
        // borders
                final double rightBoundary;
                if (isInteger && binInterval == 1) {
                    rightBoundary = leftBoundary;
                } else {
                    rightBoundary = myRoundedBorders(
                            leftBoundary + binInterval, binInterval,
                        AbstractHistogramVizModel.INTERVAL_DIGITS);

                }
                final String binCaption = createBarName(
                        firstBar, leftBoundary, rightBoundary);
                firstBar = false;
                bins.add(new InteractiveBinDataModel(
                        binCaption, leftBoundary, rightBoundary));
                // set the left boundary of the next bar to the current right
                // boundary
                if (isInteger && binInterval == 1) {
                    leftBoundary = rightBoundary + binInterval;
                } else {
                    leftBoundary = rightBoundary;
                }
            }
            return bins;
        }
    /**
     * Creates the given number of interval bins for the given
     * column specification.
     * @param colSpec the column specification
     * @param numberOfBins the number of bins to create
     * @return the created bins
     */
    public static List<BinDataModel> createIntervalBins(
                final DataColumnSpec colSpec, final int numberOfBins) {
            //set the bounds for binning
            final DataColumnDomain domain = colSpec.getDomain();
            final DataCell lowerBoundCell = domain.getLowerBound();
            if (lowerBoundCell == null || lowerBoundCell.isMissing()
                    || !lowerBoundCell.getType().isCompatible(
                            DoubleValue.class)) {
                throw new IllegalArgumentException(
                        "The lower bound of the binning column domain "
                        + "should be defined");
            }
            final double lowerBound =
                ((DoubleValue)lowerBoundCell).getDoubleValue();
            final DataCell upperBoundCell = domain.getUpperBound();
            if (upperBoundCell == null || upperBoundCell.isMissing()
                    || !upperBoundCell.getType().isCompatible(
                            DoubleValue.class)) {
                throw new IllegalArgumentException(
                        "The upper bound of the binning column domain "
                        + "should be defined");
            }
            final double upperBound =
                ((DoubleValue)upperBoundCell).getDoubleValue();
            int noOfBins = numberOfBins;
            //start the binning
            if (noOfBins < 1) {
                noOfBins = AbstractHistogramVizModel.DEFAULT_NO_OF_BINS;
            }
            if ((lowerBound - upperBound) == 0) {
                noOfBins = 1;
            }
            final boolean isInteger =
                colSpec.getType().isCompatible(IntValue.class);
            double binInterval = createBinInterval(upperBound,
                    lowerBound, noOfBins, isInteger);
            final double calculatedLowerBound = createBinStart(lowerBound,
                    binInterval, isInteger);
            if (calculatedLowerBound != lowerBound) {
                binInterval = createBinInterval(upperBound,
                        calculatedLowerBound, noOfBins, isInteger);
            }
            // increase the number of bars to include the max value
            while (calculatedLowerBound + (binInterval * noOfBins)
                    < upperBound) {
                noOfBins++;
            }
            double leftBoundary = myRoundedBorders(calculatedLowerBound,
                    binInterval, AbstractHistogramVizModel.INTERVAL_DIGITS);
            boolean firstBar = true;
            final List<BinDataModel> bins =
                new ArrayList<BinDataModel>(noOfBins);
            for (int i = 0; i < noOfBins; i++) {
        // I have to use this rounding method to avoid problems with very
        // small intervals. If the interval is very small it could happen
        // that we get the same boundaries for several bars by rounding the
        // borders
                double rightBoundary;
                if (isInteger && binInterval == 1) {
                    rightBoundary = leftBoundary;
                } else {
                    rightBoundary = myRoundedBorders(
                        leftBoundary + binInterval, binInterval,
                        AbstractHistogramVizModel.INTERVAL_DIGITS);
                }
                final String binCaption = createBarName(
                        firstBar, leftBoundary, rightBoundary);
                firstBar = false;
                bins.add(new BinDataModel(
                        binCaption, leftBoundary, rightBoundary));
                // set the left boundary of the next bar to the current right
                // boundary
                if (isInteger && binInterval == 1) {
                    leftBoundary = rightBoundary + binInterval;
                } else {
                    leftBoundary = rightBoundary;
                }
            }
            return bins;
        }

    /**
     * Adds the given row either to the missing value bin if the x value is
     * missing or to the corresponding bin.
     * @param binNominal if <code>true</code> the bins should be nominal
     * bins where the x value has to match exactly. If <code>false</code>
     * the bins should be interval bins and the x value has fit into the lower
     * and upper bound of the bin.
     *
     * @param bins the {@link BinDataModel} list
     * @param missingValueBin the bin for the missing x value rows
     * @param startBin the index of the bin to start with to speed up the
     * process if the data rows are sorted by the x value
     * @param xCell the x cell
     * @param rowColor the color of the row
     * @param id the row id
     * @param aggrColumns the aggregation columns as {@link ColorColumn}
     * objects in the same order like the aggregation cells
     * @param aggrCells the aggregation {@link DataCell} objects which
     * contain the value
     * @return the index of the bin where the row was added
     * @throws IllegalArgumentException if the given row doesn't fit in any bin
     */
    public static int addDataRow2Bin(final boolean binNominal,
            final List<? extends BinDataModel> bins,
            final BinDataModel missingValueBin, final int startBin,
            final DataCell xCell, final Color rowColor, final DataCell id,
            final Collection<ColorColumn> aggrColumns,
            final DataCell... aggrCells) throws IllegalArgumentException {
        if (bins == null) {
            throw new NullPointerException("Bins must not be null");
        }
        if (missingValueBin == null) {
            throw new NullPointerException(
                    "Missing value bin must not be null");
        }
        if (xCell == null) {
            throw new NullPointerException("X cell must not be null");
        }
        if (rowColor == null) {
            throw new NullPointerException("Row color must not be null");
        }
        if (id == null) {
            throw new NullPointerException("Row id must not be null");
        }
        if (xCell.isMissing()) {
            missingValueBin.addDataRow(id, rowColor, aggrColumns,
                    aggrCells);
            return startBin;
        } else if (binNominal) {
            return BinningUtil.addDataRow2NominalBin(bins, startBin, xCell,
                    rowColor, id, aggrColumns, aggrCells);
        } else {
            if (!xCell.getType().isCompatible(DoubleValue.class)) {
                throw new IllegalStateException(
                        "X value is not a valid number");
            }
            return BinningUtil.addDataRow2IntervalBin(bins, startBin,
                    (DoubleValue) xCell, rowColor, id, aggrColumns,
                    aggrCells);
        }
    }

    /**
     * Adds the given row to a nominal bin by checking if the given x
     * value.toString is equal to the current bin caption.
     * @param bins
     * @param startBin the index of the bin to start with
     * @param xVal the x value of this row
     * @param color the color of this row
     * @param id the id of this row
     * @param aggrColumns the selected aggregation columns
     * @param aggrVals the aggregation values
     * @return the index of the bin this row was added
     */
    private static int addDataRow2NominalBin(
            final List<? extends BinDataModel> bins,
            final int startBin, final DataCell xVal,
            final Color color, final DataCell id,
            final Collection<ColorColumn> aggrColumns,
            final DataCell... aggrVals) {
        final String xValString = xVal.toString();
         for (int binIdx = startBin, length = bins.size(); binIdx < length;
             binIdx++) {
            final BinDataModel bin = bins.get(binIdx);
            if (bin.getXAxisCaption().equals(xValString)) {
                bin.addDataRow(id, color, aggrColumns, aggrVals);
                return startBin;
            }
         }
        throw new IllegalArgumentException("No bin found for x value:"
                + xValString);
    }

    /**
     * Adds the given row to a none nominal bin by checking if the given
     * x value is in the range of the lower and upper bound of a bin.
     * @param bins
     * @param startBin the index of the bin to start with
     * @param xVal the x value of this row
     * @param color the color of this row
     * @param id the id of this row
     * @param aggrColumns the selected aggregation columns
     * @param aggrVals the aggregation values
     * @return the index of the bin this row was added
     */
    private static int addDataRow2IntervalBin(
            final List<? extends BinDataModel> bins, final int startBin,
            final DoubleValue xVal, final Color color, final DataCell id,
            final Collection<ColorColumn> aggrColumns,
            final DataCell... aggrVals) {
        final double value = xVal.getDoubleValue();
         for (int binIdx = startBin, length = bins.size(); binIdx < length;
             binIdx++) {
            final BinDataModel bin = bins.get(binIdx);
            final Double lowerBoundObj = bin.getLowerBound();
            final Double upperBoundObj = bin.getUpperBound();
            if (lowerBoundObj == null || upperBoundObj == null) {
                continue;
            }
            final double lowerBound = lowerBoundObj.doubleValue();
            final double upperBound = upperBoundObj.doubleValue();
            boolean add2Bin = false;
            if (binIdx == 0) {
                add2Bin = (value >= lowerBound && value <= upperBound);
            } else if (lowerBound == upperBound && upperBound == value) {
                add2Bin = true;
            } else {
                add2Bin = (value > lowerBound && value <= upperBound);
            }
            if (add2Bin) {
                bin.addDataRow(id, color, aggrColumns, aggrVals);
                return binIdx;
            }
         }
        throw new IllegalArgumentException("No bin found for x value:"
                + xVal.toString());
    }

    /**
     * @param colSpec the {@link DataColumnSpec} of the column to bin
     * @param noOfBins the number of bins
     * @return <code>true</code> if the bins should be nominal
     */
    public static boolean binNominal(final DataColumnSpec colSpec,
            final int noOfBins) {
        final DataType dataType = colSpec.getType();
        if (!dataType.isCompatible(DoubleValue.class)) {
            //it's not numerical
            return true;
        }
        if (dataType.isCompatible(IntValue.class)) {
            //it's an integer...
            final DataColumnDomain domain = colSpec.getDomain();
            final int lowerBound =
                ((IntValue)domain.getLowerBound()).getIntValue();
            final int upperBound =
                ((IntValue)domain.getUpperBound()).getIntValue();
            if (upperBound - lowerBound <= noOfBins) {
                //... and should be binned nominal to have for each value
                //an own bin
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given cell is in the domain range of the given
     * {@link DataColumnSpec}. If the cell is missing the method returns
     * <code>true</code>.
     * @param cell the cell to check
     * @param spec the {@link DataColumnSpec} with the domain
     * @return <code>true</code> if the cell is missing or the value is between
     * the upper and lower bound specified by the domain of the given column
     * specification
     */
    public static boolean checkDomainRange(final DataCell cell,
            final DataColumnSpec spec) {
        if (!cell.isMissing()) {
            if (!cell.getType().isCompatible(DoubleValue.class)) {
                throw new IllegalStateException(
                        "X value is not a valid number");
            }
            final DataColumnDomain domain = spec.getDomain();
            final DataCell lowerBoundCell = domain.getLowerBound();
            if (lowerBoundCell == null || lowerBoundCell.isMissing()
                    || !lowerBoundCell.getType().isCompatible(
                            DoubleValue.class)) {
                throw new IllegalArgumentException(
                        "The lower bound of the binning column domain "
                        + "should be defined");
            }
            final double lowerBound =
                ((DoubleValue)lowerBoundCell).getDoubleValue();
            final DataCell upperBoundCell = domain.getUpperBound();
            if (upperBoundCell == null || upperBoundCell.isMissing()
                    || !upperBoundCell.getType().isCompatible(
                            DoubleValue.class)) {
                throw new IllegalArgumentException(
                    "The upper bound of the binning column domain "
                        + "should be defined");
            }
            final double upperBound =
                ((DoubleValue)upperBoundCell).getDoubleValue();
            if (((DoubleValue)cell).getDoubleValue() < lowerBound
                    || ((DoubleValue)cell).getDoubleValue() > upperBound) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the maximum number of bins for the given column spec if it
     * is an integer column or returns the given number of bins.
     * @param noOfBins the current number of bins
     * @param xColSpec to calculate the range for
     * @return the maximum number of bins
     */
    public static int calculateIntegerMaxNoOfBins(final int noOfBins,
            final DataColumnSpec xColSpec) {
        int result = noOfBins;
        if (xColSpec != null
                && xColSpec.getType().isCompatible(IntValue.class)) {
            final DataColumnDomain domain = xColSpec.getDomain();
            if (domain != null) {
                final IntCell lowerBound =
                    (IntCell)domain.getLowerBound();
                final IntCell upperBound =
                    (IntCell)domain.getUpperBound();
                final int range =
                    upperBound.getIntValue() - lowerBound.getIntValue()
                    + 1;
                if (result > range) {
                    result = range;
                }
            }
        }
        return result;
    }
}
