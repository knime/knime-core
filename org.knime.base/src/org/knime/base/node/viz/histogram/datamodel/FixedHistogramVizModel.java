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
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;


/**
 * This class holds all visualization data of a histogram. 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedHistogramVizModel extends AbstractHistogramVizModel {
    private final Collection<FixedHistogramDataRow> m_sortedDataRows;

    private final Collection<ColorColumn> m_aggrColumns;
    
    private final DataColumnSpec m_xColSpec;
    
    /**
     * Constructor for class HistogramVizModel.
     * @param rowColors all possible colors the user has defined for a row
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param layout {@link HistogramLayout} to use
     * @param sortedRows the {@link FixedHistogramDataRow} sorted in ascending
     * order
     * @param xColSpec the {@link DataColumnSpec} of the x column
     * @param aggrColumns the selected aggregation columns
     * @param noOfBins the number of bins to create
     */
    public FixedHistogramVizModel(final SortedSet<Color> rowColors,
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final Collection<FixedHistogramDataRow> sortedRows, 
            final DataColumnSpec xColSpec, 
            final Collection<ColorColumn> aggrColumns, final int noOfBins) {
        super(rowColors, aggrMethod, layout, noOfBins);
        if (aggrMethod == null) {
            throw new NullPointerException(
                    "Aggregation method must not be null");
        }
        if (layout == null) {
            throw new NullPointerException("Layout must not be null");
        }
        m_aggrColumns = aggrColumns;
        m_sortedDataRows = sortedRows;
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
     * AbstractHistogramVizModel#getXColumnName()
     */
    @Override
    public String getXColumnName() {
        return m_xColSpec.getName();
    }

    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#getXColumnSpec()
     */
    @Override
    public DataColumnSpec getXColumnSpec() {
        return m_xColSpec;
    }

    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#getAggrColumns()
     */
    @Override
    public Collection<ColorColumn> getAggrColumns() {
        return m_aggrColumns;
    }
    
    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#addRows2Bins()
     */
    @Override
    protected void addRows2Bins() {
        //add the data rows to the new bins
        int startBin = 0;
        for (FixedHistogramDataRow row : m_sortedDataRows) {
            startBin = addDataRow2Bin(startBin, row.getXVal(), row.getColor(),
                    row.getRowKey().getId(), m_aggrColumns, row.getAggrVals());
        }
    }

// hiliting and selection stuff

    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#clearSelection()
     */
    @Override
    public void clearSelection() {
        // not supported in this implementation   
    }


    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#getHilitedKeys()
     */
    @Override
    public Set<DataCell> getHilitedKeys() {
        return null;
    }


    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#getSelectedKeys()
     */
    @Override
    public Set<DataCell> getSelectedKeys() {
        return null;
    }


    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#selectElement(java.awt.Point)
     */
    @Override
    public void selectElement(final Point point) {
        //not supported in this implementation
    }


    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#selectElement(java.awt.Rectangle)
     */
    @Override
    public void selectElement(final Rectangle rect) {
        //not supported in this implementation
    }
 
}
