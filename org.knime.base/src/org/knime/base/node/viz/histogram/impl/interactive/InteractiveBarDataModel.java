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
package org.knime.base.node.viz.histogram.impl.interactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;

import org.knime.base.node.viz.histogram.AbstractBarDataModel;
import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
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
final class InteractiveBarDataModel extends AbstractBarDataModel {

    private double m_aggrValue = Double.NaN;

    /**
     * <code>Hashtable</code> with the <code>RowKey</code> as
     * <code>DataCell</code> as key and the <code>DataRow</code> itself as
     * value.
     */
    private Hashtable<DataCell, DataRow> m_rows =
            new Hashtable<DataCell, DataRow>();

    /**
     * Constructor for class HistogramBar.
     * 
     * @param caption the caption of this bar
     * @param aggrColIDx the index of the aggregation column
     * @param aggrMethod the aggregation method
     */
    protected InteractiveBarDataModel(final String caption,
            final int aggrColIDx, final AggregationMethod aggrMethod) {
        super(caption, aggrColIDx, aggrColIDx, aggrMethod);
    }

    /**
     * Adds a new row to this bar.
     * 
     * @param row the <code>DataRow</code> itself
     */
    protected void addRow(final DataRow row) {
        m_rows.put(row.getKey().getId(), row);
    }

    /**
     * @see AbstractBarDataModel#getNumberOfRows()
     */
    @Override
    public int getNumberOfRows() {
        return m_rows.size();
    }

    /**
     * @see org.knime.base.node.viz.histogram.AbstractBarDataModel
     *      #setAggregationMethod(
     *      org.knime.base.node.viz.histogram.AggregationMethod)
     */
    @Override
    public void setAggregationMethod(final AggregationMethod aggrMethod) {
        super.setAggregationMethod(aggrMethod);
        m_aggrValue = Double.NaN;
    }

    /**
     * Sets the index of the new aggregation column.
     * 
     * @param aggrColIdx sets the possibly new aggregation column
     */
    @Override
    protected void setAggregationColumn(final int aggrColIdx) {
        if (getAggregationColIdx() == aggrColIdx || aggrColIdx < 0) {
            return;
        } else {
            super.setAggregationColumn(aggrColIdx);
            // check if the column type and aggregation method are compatible
            if (!getAggregationMethod().equals(AggregationMethod.COUNT)) {
                Collection<DataRow> rows = m_rows.values();
                if (rows != null && rows.size() > 0 && aggrColIdx >= 0
                        && aggrColIdx < rows.size()) {
                    DataRow row = rows.iterator().next();
                    DataCell cell = row.getCell(aggrColIdx);
                    if (!cell.getType().isCompatible(DoubleValue.class)) {
                        throw new IllegalArgumentException("Selected"
                                + " aggregation column and method aren't"
                                + " compatible.");
                    }
                }
            }
            // This forces the recalculation of the aggregation value in the
            // getAggregationValue method.
            m_aggrValue = Double.NaN;
            return;
        }
    }

    /**
     * @see AbstractBarDataModel#getAggregationValue()
     */
    @Override
    public double getAggregationValue() {
        if (Double.isNaN(m_aggrValue)) {
            calculateAggregationValue();
        }
        return m_aggrValue;
    }

    /**
     * @see AbstractBarDataModel#getRowKeys()
     */
    @Override
    public Set<DataCell> getRowKeys() {
        return m_rows.keySet();
    }

    /**
     * @see AbstractBarDataModel#createColorInformation(DataTableSpec)
     */
    @Override
    public Hashtable<ColorAttr, Collection<RowKey>> createColorInformation(
            final DataTableSpec tableSpec) {
        Hashtable<ColorAttr, Collection<RowKey>> rowsByColor =
                new Hashtable<ColorAttr, Collection<RowKey>>();
        for (DataRow row : m_rows.values()) {
            ColorAttr colAtr = tableSpec.getRowColor(row);
            Collection<RowKey> colRows = rowsByColor.get(colAtr);
            if (colRows == null) {
                colRows = new ArrayList<RowKey>();
                rowsByColor.put(colAtr, colRows);
            }
            colRows.add(row.getKey());
        }
        return rowsByColor;
    }

    /**
     * Calculates the aggregation value depending on the defined
     * <code>AggregationMethod</code>.
     */
    private void calculateAggregationValue() {
        final AggregationMethod method = getAggregationMethod();
        if (method.equals(AggregationMethod.COUNT)) {
            m_aggrValue = getNumberOfRows();
        } else {
            // calculate the sum of all cells of the aggregation column first
            // because it is needed for both methods!
            double aggrSum = 0.0;
            final int aggrColIdx = getAggregationColIdx();
            if (aggrColIdx < 0) {
                throw new IllegalStateException(
                        "Wrong aggregation column index.");
            }
            for (DataRow row : m_rows.values()) {
                final DataCell cell = row.getCell(aggrColIdx);
                if (!cell.isMissing()) {
                    aggrSum += ((DoubleValue)cell).getDoubleValue();
                }
            }
            if (method.equals(AggregationMethod.SUM)) {
                m_aggrValue = aggrSum;
            } else if (method.equals(AggregationMethod.AVERAGE)) {
                if (getNumberOfRows() == 0) {
                    m_aggrValue = 0;
                } else {
                    m_aggrValue = aggrSum / getNumberOfRows();
                }
            } else {
                // this should never happen because we check the aggregation
                // method in the set method!
                throw new IllegalArgumentException(
                        "No valid aggregation method");
            }
        }
    }

    /**
     * @see org.knime.base.node.viz.histogram.AbstractBarDataModel#clone()
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        final InteractiveBarDataModel copy = new InteractiveBarDataModel(
                getCaption(), getAggregationColIdx(), getAggregationMethod());
        copy.m_aggrValue = m_aggrValue;
        copy.m_rows = (Hashtable<DataCell, DataRow>)m_rows.clone();
        return copy;
    }
}
