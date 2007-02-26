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
 *    12.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;


/**
 * This class holds all visualization data of a histogram. 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedHistogramVizModel extends HistogramVizModel {
    private final List<FixedHistogramDataRow> m_dataRows;

    private final Collection<ColorColumn> m_aggrColumns;
    
    private final DataColumnSpec m_xColSpec;
    
    /**
     * Constructor for class HistogramVizModel.
     * @param rowColors all possible colors the user has defined for a row
     * @param noOfBins the number of bins to create
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param layout {@link HistogramLayout} to use
     * @param rows the {@link FixedHistogramDataRow}
     * @param xColSpec the {@link DataColumnSpec} of the x column
     * @param aggrColumns the selected aggregation columns
     */
    public FixedHistogramVizModel(final SortedSet<Color> rowColors,
            final int noOfBins, final AggregationMethod aggrMethod,
            final HistogramLayout layout, 
            final List<FixedHistogramDataRow> rows,
            final DataColumnSpec xColSpec, 
            final List<ColorColumn> aggrColumns) {
        super(rowColors, aggrMethod, layout, noOfBins);
        if (aggrMethod == null) {
            throw new IllegalArgumentException("No aggregation method defined");
        }
        if (layout == null) {
            throw new IllegalArgumentException("No layout defined");
        }
        m_aggrColumns = aggrColumns;
        m_dataRows = rows;
        m_xColSpec = xColSpec;
        if (m_xColSpec.getType().isCompatible(
                DoubleValue.class)) {
            setBinNominal(false);
        } else {
            setBinNominal(true);
        }
        createBins();
    }


    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * HistogramVizModel#getXColumnName()
     */
    @Override
    public String getXColumnName() {
        return m_xColSpec.getName();
    }

    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * HistogramVizModel#getXColumnSpec()
     */
    @Override
    public DataColumnSpec getXColumnSpec() {
        return m_xColSpec;
    }

    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * HistogramVizModel#getAggrColumns()
     */
    @Override
    public Collection<ColorColumn> getAggrColumns() {
        return m_aggrColumns;
    }
    
    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * HistogramVizModel#addRows2Bins()
     */
    @Override
    protected void addRows2Bins() {
        //add the data rows to the new bins
        int startBin = 0;
        for (FixedHistogramDataRow row : m_dataRows) {
            startBin = addDataRow2Bin(startBin, row.getXVal(), row.getColor(),
                    row.getRowKey().getId(), m_aggrColumns, row.getAggrVals());
        }
    }
 
}
