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

import org.knime.base.node.viz.histogram.datamodel.BinDataModel;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the {@link IntervalBinCreator} implementation that created the
 * {@link BinDataModel}s for the fixed column histogram.
 * @author Tobias Koetter, University of Konstanz
 */
public final class FixedIntervalBinCreator
extends IntervalBinCreator<BinDataModel> {

    private List<BinDataModel> m_bins;

    /**Constructor for class FixedIntervalBinCreator.*/
    protected FixedIntervalBinCreator() {
        //avoid object creation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addBin(final String binCaption, final double lowerBound,
            final double upperBound) {
        getBins().add(new BinDataModel(binCaption, lowerBound,
                upperBound));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BinDataModel> getBins() {
        if (m_bins == null) {
            m_bins = new ArrayList<BinDataModel>();
        }
        return m_bins;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createList(final int noOfBins) {
        m_bins = new ArrayList<BinDataModel>(noOfBins);
    }
}
