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
 *    11.07.2008 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.util;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.BinDataModel;

import java.util.List;

/**
 * This class is a helper class of the {@link BinningUtil} class to create the
 * different {@link BinDataModel}s for fixed and interactive histogram.
 * @author Tobias Koetter, University of Konstanz
 * @param <E> the {@link BinDataModel} implementation
 */
public abstract class IntervalBinCreator <E extends BinDataModel> {

    /**
     * @param noOfBins the number of bins that will be added
     */
    public abstract void createList(int noOfBins);

    /**
     * @param binCaption the caption of the new bin
     * @param lowerBound the left boundary of the bin
     * @param upperBound the right boundary of the bin
     */
    public abstract void addBin(final String binCaption,
            double lowerBound, double upperBound);

    /**
     * @return the bins that where created
     */
    public abstract List<E> getBins();


    /**
     * @param colSpec the column specification
     * @param numberOfBins the number of bins to create
     * @return the created bins
     */
    public List<E> createBins(final DataColumnSpec colSpec,
            final int numberOfBins) {
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
        double binInterval = BinningUtil.createBinInterval(upperBound,
                lowerBound, noOfBins, isInteger);
        final double calculatedLowerBound =
            BinningUtil.createBinStart(lowerBound, binInterval, isInteger);
        if (calculatedLowerBound != lowerBound) {
            binInterval = BinningUtil.createBinInterval(upperBound,
                    calculatedLowerBound, noOfBins, isInteger);
        }
        // increase the number of bars to include the max value
        while (calculatedLowerBound + (binInterval * noOfBins)
                < upperBound) {
            noOfBins++;
        }
        double leftBoundary = BinningUtil.myRoundedBorders(calculatedLowerBound,
                binInterval, AbstractHistogramVizModel.INTERVAL_DIGITS);
        boolean firstBar = true;
        createList(noOfBins);
        for (int i = 0; i < noOfBins; i++) {
      // I have to use this rounding method to avoid problems with very
      // small intervals. If the interval is very small it could happen
      // that we get the same boundaries for several bars by rounding the
      // borders
            double rightBoundary;
            if (isInteger && binInterval == 1) {
                rightBoundary = leftBoundary;
            } else {
                rightBoundary = BinningUtil.myRoundedBorders(
                    leftBoundary + binInterval, binInterval,
                    AbstractHistogramVizModel.INTERVAL_DIGITS);
            }
            final String binCaption = BinningUtil.createBarName(
                    firstBar, leftBoundary, rightBoundary);
            firstBar = false;
            addBin(binCaption, leftBoundary, rightBoundary);
            // set the left boundary of the next bar to the current right
            // boundary
            if (isInteger && binInterval == 1) {
                leftBoundary = rightBoundary + binInterval;
            } else {
                leftBoundary = rightBoundary;
            }
        }
        return getBins();
    }
}