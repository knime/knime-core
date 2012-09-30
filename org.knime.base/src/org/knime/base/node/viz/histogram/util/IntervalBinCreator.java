/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * -------------------------------------------------------------------
 *
 * History
 *    11.07.2008 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.util;

import java.util.List;

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.BinDataModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;

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
        double leftBoundary = calculatedLowerBound;
        final double lastBoundary = BinningUtil.myRoundedBorders(
                upperBound, binInterval,
                AbstractHistogramVizModel.INTERVAL_DIGITS, isInteger);
        // increase bin interval if we have rounding problems
        while (leftBoundary + (binInterval * noOfBins) < lastBoundary) {
            binInterval = binInterval + binInterval * 0.001;
        }
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
                    AbstractHistogramVizModel.INTERVAL_DIGITS, isInteger);
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
