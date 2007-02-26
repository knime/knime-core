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
 *    26.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;


/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramVizModel extends HistogramVizModel {

    private final DataTableSpec m_tableSpec;
    
    private int m_xColIdx;
    
    private DataColumnSpec m_xColSpec;
    
    private Collection<ColorColumn> m_aggrColumns;
    
    private final List<DataRow> m_dataRows;
    
    /**Constructor for class InteractiveHistogramVizModel.
     * @param rowColors all possible colors the user has defined for a row
     * @param noOfBins the number of bins to create
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param layout {@link HistogramLayout} to use
     * @param spec the {@link DataTableSpec}
     * @param rows the {@link FixedHistogramDataRow}
     * @param xColIdx the index of the selected x column
     * @param aggrColumns the selected aggregation columns
     */
    public InteractiveHistogramVizModel(final SortedSet<Color> rowColors,
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final DataTableSpec spec,  final List<DataRow> rows,
            final int xColIdx, final Collection<ColorColumn> aggrColumns, 
            final int noOfBins) {
        super(rowColors, aggrMethod, layout, noOfBins);
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Table specification shouldn't be null");
        }
        if (rows == null) {
            throw new IllegalArgumentException("Rows shouldn't be null");
        }
        if (xColIdx < 0 || xColIdx >= spec.getNumColumns()) {
            throw new IllegalArgumentException(
                    "Selected x column index out of range");
        }
        if (aggrColumns == null || aggrColumns.size() < 1) {
            throw new IllegalArgumentException("At least one aggregation "
                    + "column should be selected");
                    
        }
        if (noOfBins < 1) {
            throw new IllegalArgumentException("Number of bins should be > 0");
        }
        m_tableSpec = spec;
        m_dataRows = rows;
        m_aggrColumns = aggrColumns;
        final DataColumnSpec columnSpec = 
            m_tableSpec.getColumnSpec(xColIdx);
        if (columnSpec == null) {
            throw new IllegalArgumentException(
            "No column specification found for selected x column");
        }
        m_xColSpec = columnSpec;
        m_xColIdx = xColIdx;
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
     * @see org.knime.base.node.viz.histogram.datamodel.HistogramVizModel#addRows2Bins()
     */
    @Override
    protected void addRows2Bins() {
//      add the data rows to the new bins
        int startBin = 0;
        final Collection<ColorColumn> aggrColumns = getAggrColumns();
        for (DataRow row : m_dataRows) {
            final DataCell xVal = row.getCell(m_xColIdx);
            final Color color = 
                m_tableSpec.getRowColor(row).getColor(false, false);
            final DataCell id = row.getKey().getId();
            DataCell[] aggrVals = new DataCell[0];
            startBin = addDataRow2Bin(startBin, xVal, color, id, 
                    aggrColumns, aggrVals);
        }
    }

}
