/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 */
package org.knime.base.node.viz.histogram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;


/**
 * Class which holds all information of a bar in a histogram view and provides
 * methods to retrieve information needed to present this bar like number of
 * members in total or the value of the aggregation.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
final class FixedColumnBarDataModel extends AbstractBarDataModel {

    private double m_aggrSum = 0;
    
    /**
     * <code>Hashtable</code> with the <code>RowKey</code> as
     * <code>DataCell</code> as key and the <code>DataRow</code> itself as
     * value.
     */
    private Hashtable<DataCell, HistogramDataRow> m_rows = 
        new Hashtable<DataCell, HistogramDataRow>();
    
    /**
     * Constructor for class HistogramBar.
     * 
     * @param caption the caption of this bar
     * @param xCoordColIDx the index of the x column
     * @param aggrColIDx the index of the aggregation column
     * @param aggrMethod the aggregation method
     */
    protected FixedColumnBarDataModel(final String caption, 
            final int xCoordColIDx,
            final int aggrColIDx, final AggregationMethod aggrMethod) {
        super(caption, xCoordColIDx, aggrColIDx, aggrMethod);
    }

    /**
     * Adds a new row to this bar.
     * 
     * @param row the <code>DataRow</code> itself
     */
    protected void addRow(final HistogramDataRow row) {
        m_rows.put(row.getRowKey().getId(), row);
        DataCell cell = row.getAggrVal();
        if (!cell.isMissing()) {
            m_aggrSum += ((DoubleValue)cell).getDoubleValue();
        }
    }
    
    /**
     * @see org.knime.dev.node.view.histogram.AbstractBarDataModel#
     * getNumberOfRows()
     */
    @Override
    public int getNumberOfRows() {
        return m_rows.size();
    }

    /**
     * @see org.knime.base.node.viz.histogram.AbstractBarDataModel#
     * setAggregationColumn(int)
     */
    @Override
    protected void setAggregationColumn(final int aggrColIdx) {
        if (aggrColIdx != getAggregationColIdx()) {
            throw new IllegalArgumentException(getClass().getName()
                    + " doesn't support aggregation column changing.");
        }
    }
    
    /**
     * @see org.knime.base.node.viz.histogram.AbstractBarDataModel#
     * getAggregationValue()
     */
    @Override
    public double getAggregationValue() {
        final AggregationMethod method = getAggregationMethod();
        if (method.equals(AggregationMethod.COUNT)) {
            return getNumberOfRows();
        } else if (method.equals(AggregationMethod.SUM)) {
            return m_aggrSum;
        } else if (method.equals(AggregationMethod.AVERAGE)) {
            int noOfRows = getNumberOfRows();
            if (noOfRows == 0) {
                return 0;
            }
            return m_aggrSum / noOfRows;
        } else {
            throw new IllegalArgumentException("Internal exception: "
                    + "Aggregation method not implemented.");
        }
    }

    /**
     * @see org.knime.dev.node.view.histogram.AbstractBarDataModel#getRowKeys()
     */
    @Override
    public Set<DataCell> getRowKeys() {
        return m_rows.keySet();
    }
    
    /**
     * @see org.knime.dev.node.view.histogram.AbstractBarDataModel#
     * createColorInformation(org.knime.core.data.DataTableSpec)
     */
    @Override
    public Hashtable<ColorAttr, Collection<RowKey>> 
        createColorInformation(final DataTableSpec tableSpec) {
        Hashtable<ColorAttr, Collection<RowKey>> rowsByColor = 
            new Hashtable<ColorAttr, Collection<RowKey>>();
        for (HistogramDataRow row : m_rows.values()) {
            ColorAttr colAtr = row.getColor();
            Collection<RowKey> colRows = rowsByColor.get(colAtr);
            if (colRows == null) {
                colRows = new ArrayList<RowKey>();
                rowsByColor.put(colAtr, colRows);
            }
            colRows.add(row.getRowKey());
        }
        return rowsByColor;
    }
}
