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
 *    12.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;

/**
 *
 * @author Tobias Koetter, University of Konstanz
 * @param <S> the shape of this sub model
 * @param <H> the optional hilite shape
 */
public abstract class AggregationValSubModel <S extends Shape, H extends Shape>
implements Serializable, AggregationModel<S, H> {

    private final Color m_color;
    private final boolean m_supportHiliting;
    private double m_aggrSum = 0;
    /**The number of values without missing values!*/
    private int m_valueCounter = 0;
    /**The number of rows including empty value rows.*/
    private int m_rowCounter = 0;
    private boolean m_isSelected = false;
    private S m_shape;
    private final Set<DataCell> m_rowKeys;
    private final Set<DataCell> m_hilitedRowKeys;
    private H m_hilitedShape;

    /**Constructor for class AttributeValColorModel.
     * @param color the color to use for this sub element
     */
    protected AggregationValSubModel(final Color color) {
        this(color, false);
    }

    /**Constructor for class AttributeValColorModel.
     * @param color the color to use for this sub element
     * @param supportHiliting if hiliting support should be enabled
     */
    protected AggregationValSubModel(final Color color,
            final boolean supportHiliting) {
        m_color = color;
        m_supportHiliting = supportHiliting;
        if (m_supportHiliting) {
            m_rowKeys  = new HashSet<DataCell>();
            m_hilitedRowKeys = new HashSet<DataCell>();
        } else {
            m_rowKeys = null;
            m_hilitedRowKeys = null;
        }
    }

    /**Constructor for class AttributeValColorModel (used for cloning).
     * @param color the color to use for this sub element
     * @param supportHiliting if hiliting should be supported
     * @param aggrSum the sum of the aggregation values
     * @param valueCounter the number of aggregation values
     * @param rowCounter the number of rows incl. missing values
     * @param selected <code>true</code> if this element is selected
     */
    protected AggregationValSubModel(final Color color,
            final boolean supportHiliting, final double aggrSum,
            final int valueCounter, final int rowCounter,
            final boolean selected) {
        m_color = color;
        m_aggrSum = aggrSum;
        m_valueCounter = valueCounter;
        m_rowCounter = rowCounter;
        m_isSelected = selected;
        m_supportHiliting = supportHiliting;
        if (m_supportHiliting) {
            m_rowKeys  = new HashSet<DataCell>();
            m_hilitedRowKeys = new HashSet<DataCell>();
        } else {
            m_rowKeys = null;
            m_hilitedRowKeys = null;
        }
    }

    /**
     * Adds the given values to the sub element.
     * @param rowKey the rowkey of the row to add
     * @param aggrValCell the value cell of the aggregation column of this sub
     * element
     */
    protected void addDataRow(final DataCell rowKey,
            final DataCell aggrValCell) {
        if (!aggrValCell.isMissing()) {
            m_aggrSum += ((DoubleValue)aggrValCell).getDoubleValue();
            m_valueCounter++;
        }
        m_rowCounter++;
        if (m_supportHiliting) {
            m_rowKeys.add(rowKey);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getRowCount() {
        return m_rowCounter;
    }

    /**
     * {@inheritDoc}
     */
    public int getValueCount() {
        return m_valueCounter;
    }

    /**
     * {@inheritDoc}
     */
    public double getAggregationSum() {
        return m_aggrSum;
    }

    /**
     * @param method the {@link AggregationMethod} to use
     * @return the aggregation value of this sub element
     */
    public double getAggregationValue(final AggregationMethod method) {
        if (AggregationMethod.COUNT.equals(method)) {
            return m_rowCounter;
        } else if (AggregationMethod.SUM.equals(method)) {
            return m_aggrSum;
        } else if (AggregationMethod.AVERAGE.equals(method)) {
            if (m_valueCounter == 0) {
                //avoid division by 0
                return 0;
            }
            return m_aggrSum / m_valueCounter;
        }
       throw new IllegalArgumentException("Aggregation method "
               + method + " not supported.");
    }

    /**
     * @return the color to use for this sub element
     */
    public Color getColor() {
        return m_color;
    }

    /**
     * @return the shape
     */
    public S getShape() {
        return m_shape;
    }

    /**
     * @param shape the new shape of this sub element
     * @param calculator the hilite shape calculator
     */
    protected void setShape(final S shape,
            final HiliteShapeCalculator<S, H> calculator) {
        m_shape = shape;
        calculateHilitedRectangle(calculator);
    }

    /**
     * {@inheritDoc}
     */
    public H getHiliteShape() {
        return m_hilitedShape;
    }

    /**
     * @param shape the new hilite shape
     */
    protected void setHiliteShape(final H shape) {
        m_hilitedShape = shape;
    }

    /**
     * @return <code>true</code> if the element is selected
     */
    public boolean isSelected() {
        return m_isSelected;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPresentable() {
        //these elements are always presentable
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        //this element has no name
        return null;
    }

    /**
     * @return the enableHiliting variable
     */
    protected boolean supportsHiliting() {
        return m_supportHiliting;
    }

    /**
     * @param isSelected set to <code>true</code> if the element is selected
     */
    protected void setSelected(final boolean isSelected) {
        this.m_isSelected = isSelected;
    }

    /**
     * Selects this element if the element rectangle contains the given
     * point.
     * @param point the {@link Point} to check
     * @return <code>true</code> if the element contains the point
     */
    public boolean selectElement(final Point point) {
        if (m_shape != null
                    && m_shape.contains(point)) {
            setSelected(true);
            return true;
        }
        return false;
    }

    /**
     * Selects this element if the element rectangle intersect the given
     * rectangle.
     * @param rect the {@link Rectangle} to check
     * @return <code>true</code> if the element intersects the rectangle
     */
    public boolean selectElement(final Rectangle rect) {
        if (m_shape != null
                && m_shape.intersects(rect)) {
        setSelected(true);
        return true;
        }
        return false;
    }

    /**
     * @return the keys of the rows in this element.
     */
    public Set<DataCell> getKeys() {
        return m_rowKeys;
    }

    /**
     * @return <code>true</code> if at least one row of this element is hilited
     */
    public boolean isHilited() {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        return m_hilitedRowKeys.size() > 0;
    }

    /**
     * @return the keys of the hilited rows in this element
     */
    public Set<DataCell> getHilitedKeys() {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        return m_hilitedRowKeys;
    }

    /**
     * Clears the hilite counter.
     */
    protected void clearHilite() {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        m_hilitedRowKeys.clear();
        setHiliteShape(null);
    }

    /**
     * @param hilitedKeys the hilited keys
     * @param calculator the hilite shape calculator
     * @return <code>true</code> if at least one key has been added
     */
    protected boolean setHilitedKeys(final Collection<DataCell> hilitedKeys,
            final HiliteShapeCalculator<S, H> calculator) {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        boolean changed = false;
        for (final DataCell key : hilitedKeys) {
            if (m_rowKeys.contains(key)) {
                if (m_hilitedRowKeys.add(key)) {
                    changed = true;
                }
            }
        }
        if (changed) {
            calculateHilitedRectangle(calculator);
        }
        return changed;
    }

    /**
     * @param unhilitedKeys the keys which should be unhilited
     * @param calculator the hilite shape calculator
     * @return <code>true</code> if at least one key has been removed
     */
    protected boolean removeHilitedKeys(
            final Collection<DataCell> unhilitedKeys,
            final HiliteShapeCalculator<S, H> calculator) {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        final boolean changed = m_hilitedRowKeys.removeAll(unhilitedKeys);
        if (changed) {
            calculateHilitedRectangle(calculator);
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    public int getHiliteRowCount() {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        return m_hilitedRowKeys.size();
    }

    /**
     * Calculates the hilite rectangle or resets the hilite rectangle to
     * <code>null</code> if no rows are hilited.
     * @param calculator the hilite shape calculator
     */
    protected void calculateHilitedRectangle(
            final HiliteShapeCalculator<S, H> calculator) {
        if (calculator == null) {
            return;
        }
        setHiliteShape(calculator.calculateHiliteShape(this));
    }
}
