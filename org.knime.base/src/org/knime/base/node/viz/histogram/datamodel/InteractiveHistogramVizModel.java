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
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;


/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramVizModel extends AbstractHistogramVizModel {

    /**
     * Compares the value on the given column index with the given
     * {@link DataValueComparator} of to rows.
     * @author Tobias Koetter, University of Konstanz
     */
    private class RowComparator implements Comparator<DataRow> {

        private DataValueComparator m_colComparator;
        
        private int m_colIdx;
        
        /**Constructor for class InteractiveHistogramVizModel.RowComparator.
         * @param comparator the {@link DataValueComparator} to use
         * @param colIdx the column index to compare
         * 
         */
        public RowComparator(final DataValueComparator comparator, 
                final int colIdx) {
            if (comparator == null) {
                throw new IllegalArgumentException(
                        "Column comparator shouldn't be null");
            }
            m_colComparator = comparator;
            m_colIdx = colIdx;
        }
        
        /**
         * @param comparator the new {@link DataValueComparator} to use
         * @param colIdx the new column index to compare
         */
        public void update(final DataValueComparator comparator, 
                final int colIdx) {
            m_colIdx = colIdx;
            m_colComparator = comparator;
        }
        
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(final DataRow o1, final DataRow o2) {
            return m_colComparator.compare(o1.getCell(m_colIdx),
                    o2.getCell(m_colIdx));
        }
        
    }
    
    private final DataTableSpec m_tableSpec;
    
    private int m_xColIdx = -1;
    
    private DataColumnSpec m_xColSpec;
    
    private RowComparator m_rowComparator;
    
    private Collection<ColorColumn> m_aggrColumns;
    
    private final List<DataRow> m_dataRows;
    
    private boolean m_isSorted = false;
    
    /**Constructor for class InteractiveHistogramVizModel.
     * @param rowColors all possible colors the user has defined for a row
     * @param noOfBins the number of bins to create
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param layout {@link HistogramLayout} to use
     * @param spec the {@link DataTableSpec}
     * @param rows the {@link FixedHistogramDataRow}
     * @param xColSpec the {@link DataColumnSpec} of the selected x column
     * @param aggrColumns the selected aggregation columns
     */
    public InteractiveHistogramVizModel(final SortedSet<Color> rowColors,
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final DataTableSpec spec,  final List<DataRow> rows,
            final DataColumnSpec xColSpec, 
            final Collection<ColorColumn> aggrColumns, 
            final int noOfBins) {
        super(rowColors, aggrMethod, layout, noOfBins);
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Table specification shouldn't be null");
        }
        if (xColSpec == null) {
            throw new IllegalArgumentException(
            "No column specification found for selected x column");
        }
        if (rows == null) {
            throw new IllegalArgumentException("Rows shouldn't be null");
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
        setXColumn(xColSpec);
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
     * @param xColSpec the new x column specification
     * @return <code>true</code> if the variable has changed
     */
    public boolean setXColumn(final DataColumnSpec xColSpec) {
        if (xColSpec == null) {
            throw new IllegalArgumentException(
                    "X column specification shouldn't be null");
        }
        final int xColIdx = m_tableSpec.findColumnIndex(xColSpec.getName());
        if (xColIdx < 0) {
            throw new IllegalArgumentException("X column not found");
        }
        if (xColIdx == m_xColIdx) {
            return false;
        }
        m_xColSpec = xColSpec;
        m_xColIdx = xColIdx;
        m_isSorted = false;
        final DataType xColType = m_xColSpec.getType();
        if (m_rowComparator == null) {
            m_rowComparator = 
                new RowComparator(xColType.getComparator(), m_xColIdx);
        } else {
            m_rowComparator.update(xColType.getComparator(), m_xColIdx);
        }
        if (xColType.isCompatible(
                DoubleValue.class)) {
            //if we have binned nominal reset the number of bins to default
            setBinNominal(false);
            if (isBinNominal()) {
                updateNoOfBins(DEFAULT_NO_OF_BINS);
            }
        } else {
            setBinNominal(true);
        }
        createBins();
        return true;
    }

    /**
     * @param aggrCols the new aggregation columns
     * @return <code>true</code> if the variable has changed
     */
    public boolean setAggregationColumns(
            final ArrayList<ColorColumn> aggrCols) {
        if (aggrCols == null || aggrCols.size() < 1) {
            throw new IllegalArgumentException(
                    "Aggregation column shouldn't be null");
        }
        if (m_aggrColumns.containsAll(aggrCols)) {
            return false;
        }
        m_aggrColumns = aggrCols;
        createBins();
        return true;
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
     * @return the data rows in ascending order
     */
    private List<DataRow> getSortedRows() {
        if (!m_isSorted) {
            Collections.sort(m_dataRows, m_rowComparator);
        }
        return m_dataRows;
    }

    /**
     * @see org.knime.base.node.viz.histogram.datamodel.
     * AbstractHistogramVizModel#addRows2Bins()
     */
    @Override
    protected void addRows2Bins() {
//      add the data rows to the new bins
        int startBin = 0;
        final Collection<ColorColumn> aggrColumns = getAggrColumns();
        final int aggrSize = aggrColumns.size();
        final int[] aggrIdx = new int[aggrSize];
        int i = 0;
        for (ColorColumn aggrColumn : aggrColumns) {
            aggrIdx[i++] = aggrColumn.getColumnIndex();
        }
        for (DataRow row : getSortedRows()) {
            final DataCell xVal = row.getCell(m_xColIdx);
            final Color color = 
                m_tableSpec.getRowColor(row).getColor(false, false);
            final DataCell id = row.getKey().getId();
            DataCell[] aggrVals = new DataCell[aggrSize];
            for (int j = 0, length = aggrIdx.length; j < length; j++) {
                aggrVals[j] = row.getCell(aggrIdx[j]);
            }
            startBin = addDataRow2Bin(startBin, xVal, color, id, 
                    aggrColumns, aggrVals);
        }
    }
//Hiliting selection stuff
    /**
     * @return all keys of hilited rows
     */
    public Set<DataCell> getHilitedKeys() {
        final Set<DataCell> keys = new HashSet<DataCell>();
        for (final InteractiveBinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                if (bar.isSelected()) {
                    final Collection<InteractiveBarElementDataModel> elements = bar
                            .getElements();
                    for (final InteractiveBarElementDataModel element : elements) {
                        keys.addAll(element.getHilitedKeys());
                    }
                }
            }
        }
        return keys;
    }

    /**
     * @return all keys of the selected elements
     */
    public Set<DataCell> getSelectedKeys() {
        final Set<DataCell> keys = new HashSet<DataCell>();
        for (final InteractiveBinDataModel bin : getBins()) {
            if (bin.isSelected()) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (final BarDataModel bar : bars) {
                    if (bar.isSelected()) {
                        final Collection<InteractiveBarElementDataModel> elements = bar
                                .getElements();
                        for (final InteractiveBarElementDataModel element : elements) {
                            if (element.isSelected()) {
                                keys.addAll(element.getKeys());
                            }
                        }
                    }
                }
            }
        }
        return keys;
    }

    /**
     * Selects the element which contains the given point.
     * @param point the point on the screen to select
     */
    public void selectElement(final Point point) {
        for (final InteractiveBinDataModel bin : getBins()) {
            bin.selectElement(point);
        }
        return;
    }

    /**
     * Selects all elements which are touched by the given rectangle.
     * @param rect the rectangle on the screen select
     */
    public void selectElement(final Rectangle rect) {
        for (final InteractiveBinDataModel bin : getBins()) {
            bin.selectElement(rect);
        }
        return;
    }

    /**
     * Clears all selections.
     */
    public void clearSelection() {
        for (final InteractiveBinDataModel bin : getBins()) {
            bin.setSelected(false);
        }
    }

    /**
     * This method un/hilites all rows with the given key.
     * @param hilited the rowKeys of the rows to un/hilite
     * @param hilite if the given keys should be hilited <code>true</code> 
     * or unhilited <code>false</code>
     */
    public void updateHiliteInfo(final Set<DataCell> hilited,
            final boolean hilite) {
        if (hilited == null || hilited.size() < 1) {
            return;
        }
        final AggregationMethod aggrMethod = getAggregationMethod();
        final HistogramLayout layout = getHistogramLayout();
        for (final InteractiveBinDataModel bin : getBins()) {
            if (hilite) {
                bin.setHilitedKeys(hilited, aggrMethod, layout);
            } else {
                bin.removeHilitedKeys(hilited, aggrMethod, layout);
            }
        }
    }

    /**
     * Unhilites all rows.
     */
    public void unHiliteAll() {
        for (final InteractiveBinDataModel bin : getBins()) {
            bin.clearHilite();
        }
    }
}
