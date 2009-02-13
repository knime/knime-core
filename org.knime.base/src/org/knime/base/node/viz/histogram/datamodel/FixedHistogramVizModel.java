/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *    12.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.RowKey;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.util.ColorColumn;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * This class holds all visualization data of a histogram.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedHistogramVizModel extends AbstractHistogramVizModel {

    private final Collection<ColorColumn> m_aggrColumns;

    private final DataColumnSpec m_xColSpec;

    /**
     * Constructor for class HistogramVizModel.
     * @param rowColors the different row colors
     * @param bins the bins
     * @param missingValueBin the bin with the rows with missing x values
     * @param xColSpec the column specification of the selected x column
     * @param aggrColumns the selected aggregation columns. Could be
     * <code>null</code>
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param layout {@link HistogramLayout} to use
     */
    public FixedHistogramVizModel(final List<Color> rowColors,
            final List<BinDataModel> bins, final BinDataModel missingValueBin,
            final DataColumnSpec xColSpec,
            final Collection<ColorColumn> aggrColumns,
            final AggregationMethod aggrMethod,
            final HistogramLayout layout) {
        super(rowColors, aggrMethod, layout, bins.size());
        if (aggrMethod == null) {
            throw new NullPointerException(
                    "Aggregation method must not be null");
        }
        if (layout == null) {
            throw new NullPointerException("Layout must not be null");
        }
        m_aggrColumns = aggrColumns;
        if (aggrColumns != null && aggrColumns.size() > 1) {
            setShowBarOutline(true);
            setShowBinOutline(true);
        } else {
            setShowBarOutline(false);
            setShowBinOutline(false);
        }
        m_xColSpec = xColSpec;
        setBins(bins, missingValueBin);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getXColumnName() {
        return m_xColSpec.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec getXColumnSpec() {
        return m_xColSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ColorColumn> getAggrColumns() {
        return m_aggrColumns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsHiliting() {
        return false;
    }

    // hiliting stuff

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RowKey> getHilitedKeys() {
        throw new UnsupportedOperationException("Hiliting not supported");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Set<RowKey> getSelectedKeys() {
        throw new UnsupportedOperationException("Hiliting not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiliteAll() {
        throw new UnsupportedOperationException("Hiliting not supported");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateHiliteInfo(final Set<RowKey> hilited,
            final boolean hilite) {
        throw new UnsupportedOperationException("Hiliting not supported");
    }
}
