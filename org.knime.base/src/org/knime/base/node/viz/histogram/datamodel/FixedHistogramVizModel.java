/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
