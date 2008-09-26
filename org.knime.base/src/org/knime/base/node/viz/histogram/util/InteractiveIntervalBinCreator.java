/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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

import org.knime.base.node.viz.histogram.datamodel.InteractiveBinDataModel;

import java.util.ArrayList;
import java.util.List;


/**
 *This is the {@link IntervalBinCreator} implementation that created the
 * {@link InteractiveBinDataModel}s for the interactive histogram.
 * @author Tobias Koetter, University of Konstanz
 */
public final class InteractiveIntervalBinCreator extends
IntervalBinCreator<InteractiveBinDataModel> {

    private List<InteractiveBinDataModel> m_bins;

    /**Constructor for class InteractiveIntervalBinCreator.*/
    protected InteractiveIntervalBinCreator() {
        //avoid object creation
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void addBin(final String binCaption, final double lowerBound,
            final double upperBound) {
        getBins().add(new InteractiveBinDataModel(binCaption, lowerBound,
                upperBound));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createList(final int noOfBins) {
        m_bins = new ArrayList<InteractiveBinDataModel>(noOfBins);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InteractiveBinDataModel> getBins() {
        return m_bins;
    }

}
