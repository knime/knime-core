/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *    12.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

import java.awt.Color;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This abstract class holds the data of a particular aggregation value and its
 * {@link AggregationValSubModel}s.
 * @author Tobias Koetter, University of Konstanz
 * @param <T> the type of the concrete sub model implementation
 * @param <S> the basic shape
 * @param <H> the optional hilite shape
 */
public abstract class AggregationValModel
<T extends AggregationValSubModel<S, H>, S extends Shape, H extends Shape>
implements Serializable, AggregationModel<S, H> {

    private static final long serialVersionUID = -1350754955352409195L;

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(AggregationValModel.class);

    private static final String CFG_BAR_NAME = "name";
    private static final String CFG_COLOR_RGB = "color";
    private static final String CFG_ROW_COUNTER = "rowCount";
    private static final String CFG_VALUE_COUNTER = "valueCount";
    private static final String CFG_AGGR_SUM = "aggrSum";
    private static final String CFG_HILITING = "hiliting";
    private static final String CFG_SUBMODELS = "subElements";

    private final String m_name;

    private Color m_color;

    private final boolean m_supportHiliting;

    private final Map<Color, T> m_elements =
            new HashMap<Color, T>();

    /**The number of rows including empty value rows.*/
    private int m_rowCounter = 0;

    /**The number of values without missing values!*/
    private int m_valueCount = 0;

    private double m_aggrSum = 0;

    private boolean m_presentable = false;

    private boolean m_isSelected = false;

    private S m_shape;

    /**If the different elements of this bar can't be draw because the bar
     * is to small this rectangle is calculated to reflect the proportion
     * of hilited rows in this bar. */
    private H m_hiliteShape;

    /**Constructor for class AttributeValModel.
     * @param name the name of this element
     * @param color the color to use for this element
     * @param supportHiliting if hiliting should be supported
     */
    protected AggregationValModel(final String name, final Color color,
            final boolean supportHiliting) {
        m_name = name;
        m_color = color;
        m_supportHiliting = supportHiliting;
    }

    /**Constructor for class AttributeValModel.
     * @param name the name of this element
     * @param color the color of this element
     * @param elements the sub elements
     * @param rowCounter the number of rows including missing values
     * @param valueCounter the number of values exl. missing values
     * @param aggrSum the aggregation sum
     * @param supportHiliting if hiliting should be supported
     */
    protected AggregationValModel(final String name, final Color color,
            final Map<Color, T> elements,
            final int rowCounter, final int valueCounter,
            final double aggrSum, final boolean supportHiliting) {
        m_name = name;
        m_color = color;
        m_elements.putAll(elements);
        m_rowCounter = rowCounter;
        m_valueCount = valueCounter;
        m_aggrSum = aggrSum;
        m_supportHiliting = supportHiliting;
    }

    /**Constructor for class AggregationValModel.
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress information
     * @throws InvalidSettingsException if the config object is invalid
     * @throws CanceledExecutionException if the operation is canceled
     */
    protected AggregationValModel(final ConfigRO config,
            final ExecutionMonitor exec)
        throws InvalidSettingsException, CanceledExecutionException {
        m_name = config.getString(CFG_BAR_NAME);
        m_color = new Color(config.getInt(CFG_COLOR_RGB));
        m_rowCounter = config.getInt(CFG_ROW_COUNTER);
        m_valueCount = config.getInt(CFG_VALUE_COUNTER);
        m_aggrSum = config.getDouble(CFG_AGGR_SUM);
        m_supportHiliting = config.getBoolean(CFG_HILITING);
        final Config subConfig = config.getConfig(CFG_SUBMODELS);
        final Collection<T> elements = loadElements(subConfig, exec);
        for (final T element : elements) {
            m_elements.put(element.getColor(), element);
        }
    }

    /**
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress information
     * @return the elements
     * @throws CanceledExecutionException if the operation is canceled
     * @throws InvalidSettingsException if the config object is invalid
     */
    protected abstract Collection<T> loadElements(final ConfigRO config,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            InvalidSettingsException;

    /**
     * Adds a new row to this element.
     * @param color the color of the data row
     * @param rowKey the row key
     * @param cell the optional aggregation value cell
     */
    public void addDataRow(final Color color, final RowKey rowKey,
            final DataCell cell) {
        if (color == null) {
            throw new NullPointerException("color must not be null");
        }
        if (rowKey == null) {
            throw new NullPointerException("rowKey must not be null");
        }
        T element = getElement(color);
        if (element == null) {
            element = createElement(color);
            m_elements.put(color, element);
        }
        if (cell != null && !cell.isMissing()) {
            if (!cell.getType().isCompatible(DoubleValue.class)) {
                throw new IllegalArgumentException(
                        "DataCell should be numeric");
            }
            m_aggrSum += ((DoubleValue)cell).getDoubleValue();
            m_valueCount++;
        }
        element.addDataRow(rowKey, cell);
        m_rowCounter++;
    }

    /**
     * @param color the color of the new sub element
     * @return the new sub element with the given color
     */
    protected abstract T createElement(final Color color);

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return m_name;
    }

    /**
     * @param color the new color of this aggregation value model
     */
    public void setColor(final Color color) {
        if (color == null) {
            throw new NullPointerException("color must not be null");
        }
        m_color = color;
    }

    /**
     * {@inheritDoc}
     */
    public Color getColor() {
        return m_color;
    }

    /**
     * @param color the color of the sub element
     * @return the sub element with the given color or <code>null</code> if none
     * sub element with the given color exists
     */
    public T getElement(final Color color) {
        return m_elements.get(color);
    }

    /**
     * @return all sub elements of this element
     */
    public Collection<T> getElements() {
        return m_elements.values();
    }

    /**
     * @return all selected sub elements of this element
     */
    public List<T> getSelectedElements() {
        final List<T> result = new ArrayList<T>(m_elements.size());
        for (final T element : m_elements.values()) {
            if (element.isSelected()) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Returns the sub element of this element that contains the given point
     * or <code>null</code> if none contains the point.
     * @param p the point to select
     * @return the sub element that contains the point or
     * <code>null</code>
     */
    public T getSelectedSubElement(final Point p) {
        if (p == null) {
            return null;
        }
        final Collection<T> elements = getElements();
        for (final T element : elements) {
            final S shape = element.getShape();
            if (shape != null && shape.contains(p)) {
                return element;
            }
        }
        return null;
    }

    /**
     * @return the number of sub elements
     */
    public int getNoOfElements() {
        return m_elements.size();
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
    public double getAggregationSum() {
        return m_aggrSum;
    }

    /**
     * {@inheritDoc}
     */
    public int getValueCount() {
        return m_valueCount;
    }

    /**
     * {@inheritDoc}
     */
    public double getAggregationValue(final AggregationMethod method) {
        if (AggregationMethod.COUNT.equals(method)) {
            return m_rowCounter;
        } else if (AggregationMethod.VALUE_COUNT.equals(method)) {
            return m_valueCount;
        } else if (AggregationMethod.SUM.equals(method)) {
            return m_aggrSum;
        } else if (AggregationMethod.AVERAGE.equals(method)) {
            if (m_valueCount == 0) {
                //avoid division by 0
                return 0;
            }
            return m_aggrSum / m_valueCount;
        }
        throw new IllegalArgumentException("Aggregation method " + method
                + " not supported.");
    }

    /**
     * {@inheritDoc}
     */
    public S getShape() {
        return m_shape;
    }

    /**
     * @param shape the shape check for selection and drawing
     * @param calculator the hilite shape calculator
     */
    public void setShape(final S shape,
            final HiliteShapeCalculator<S, H> calculator) {
        if (shape == null) {
            m_presentable = false;
        } else {
            m_presentable = true;
        }
        m_shape = shape;
        calculateHiliteShape(calculator);
    }

    /**
     * {@inheritDoc}
     */
    public H getHiliteShape() {
        return m_hiliteShape;
    }

    /**
     * @param shape the hilite shape to draw
     */
    protected void setHiliteShape(final H shape) {
        m_hiliteShape = shape;
    }

    /**
     * @param presentable <code>true</code> if this element is presentable
     * @param calculator the hilite shape calculator
     */
    protected void setPresentable(final boolean presentable,
            final HiliteShapeCalculator<S, H> calculator) {
        if (m_presentable == presentable) {
            return;
        }
        m_presentable = presentable;
        //recalculate the hilite shape
        calculateHiliteShape(calculator);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPresentable() {
        return m_presentable;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSelected() {
        return m_isSelected;
    }

    /**
     * @param selected <code>true</code> if this element is selected
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setSelected(final boolean selected) {
        if (m_isSelected == selected) {
            return false;
        }
        m_isSelected = selected;
        for (final T element : getElements()) {
            element.setSelected(selected);
        }
        return true;
    }

    /**
     * @param point the {@link Point} to check
     * @param detailed if also the sub sections should be checked
     * @return <code>true</code> if at least one sub element of this element
     * contains the point
     */
    public boolean selectElement(final Point point, final boolean detailed) {
        if (m_shape != null && m_shape.contains(point)) {
            //if the element is to small to draw the different
            //elements we have to select all elements
            //of this element
            if (!detailed || !isPresentable()) {
                //select all sub element if the detail mode is of or
                //the element wasn't selected yet
                for (final T element : getElements()) {
                    element.setSelected(true);
                }
                m_isSelected = true;
            } else {
                for (final T element : getElements()) {
                    m_isSelected = element.selectElement(point) || m_isSelected;
                }
            }
        }
        return m_isSelected;
    }

    /**
     * Selects all sub element of this element which intersect the given
     * rectangle.
     * @param rect the {@link Rectangle2D} to check
     * @param detailed if also the sub sections should be checked
     * @return <code>true</code> if at least one sub element of this element
     * intersects the rectangle
     */
    public boolean selectElement(final Rectangle2D rect,
            final boolean detailed) {
        if (m_shape != null && m_shape.intersects(rect)) {
            //if the element is to small to draw the different
            //elements we have to select all elements
            //of this element
            if (!detailed || !isPresentable()) {
                //select all sub element if the detail mode is of or
                //the element wasn't selected yet
                for (final T element : getElements()) {
                    element.setSelected(true);
                }
                m_isSelected = true;
            } else {
                for (final T element : getElements()) {
                    m_isSelected = element.selectElement(rect) || m_isSelected;
                }
            }
        }
        return m_isSelected;
    }

    /**
     * @return <code>true</code> if hiliting is supported
     */
    public boolean supportsHiliting() {
        return m_supportHiliting;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return m_rowCounter < 1;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isHilited() {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        for (final T element : getElements()) {
            if ((element).isHilited()) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int getHiliteRowCount() {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        int noOfHilitedKeys = 0;
        for (final T element : getElements()) {
            noOfHilitedKeys +=
                (element).getHiliteRowCount();
        }
        return noOfHilitedKeys;
    }

    /**
     * @param hilited the row keys to unhilite
     * @param calculator the hilite shape calculator
     * @return if the hilite keys have changed
     */
    public boolean removeHilitedKeys(final Collection<RowKey> hilited,
            final HiliteShapeCalculator<S, H> calculator) {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        boolean changed = false;
        for (final T element : getElements()) {
            changed = element.removeHilitedKeys(hilited, calculator) || changed;
        }
        if (changed) {
            calculateHiliteShape(calculator);
        }
        return changed;
    }

    /**
     * @param hilited the row keys to hilite
     * @param calculator the hilite shape calculator
     * @return if the hilite keys have changed
     */
    public boolean setHilitedKeys(final Collection<RowKey> hilited,
            final HiliteShapeCalculator<S, H> calculator) {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        boolean changed = false;
        for (final T element : getElements()) {
            changed = element.setHilitedKeys(hilited, calculator) || changed;
        }
        if (changed) {
            calculateHiliteShape(calculator);
        }
        return changed;
    }

    /**
     * Clears all hilite information.
     */
    public void clearHilite() {
        if (!m_supportHiliting) {
            throw new UnsupportedOperationException(
                    "Hilitign is not supported");
        }
        for (final T element : getElements()) {
            element.clearHilite();
        }
        setHiliteShape(null);
    }

    /**
     * Overwrite this method to support hiliting.
     * @param calculator the optional hilite calculator
     */
    @SuppressWarnings("unchecked")
    protected void calculateHiliteShape(
            final HiliteShapeCalculator<S, H> calculator) {
        if (calculator == null) {
            return;
        }
        if (supportsHiliting()) {
            setHiliteShape(calculator.calculateHiliteShape(
                (AggregationValModel<AggregationValSubModel<S, H>, S, H>)this));
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected AggregationValModel <T, S , H> clone()
        throws CloneNotSupportedException {
        LOGGER.debug("Entering clone() of class AggregationValModel.");
        final long startTime = System.currentTimeMillis();
        AggregationValModel <T, S , H> clone =
            null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(this);
            final ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
            clone = (AggregationValModel <T, S , H>)
                new ObjectInputStream(bais).readObject();
        } catch (final Exception e) {
            final String msg =
                "Exception while cloning aggregation value model: "
                + e.getMessage();
              LOGGER.debug(msg);
              throw new CloneNotSupportedException(msg);
        }

        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for cloning. " + durationTime + " ms");
        LOGGER.debug("Exiting clone() of class AggregationValModel.");
        return clone;
    }

    /**
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @throws CanceledExecutionException if the operation is canceled
     */
    public void save2File(final ConfigWO config,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        config.addString(CFG_BAR_NAME, getName());
        config.addInt(CFG_COLOR_RGB, getColor().getRGB());
        config.addInt(CFG_ROW_COUNTER, getRowCount());
        config.addInt(CFG_VALUE_COUNTER, getValueCount());
        config.addDouble(CFG_AGGR_SUM, getAggregationSum());
        config.addBoolean(CFG_HILITING, supportsHiliting());
        final Config subConfig = config.addConfig(CFG_SUBMODELS);
        saveElements(getElements(), subConfig, exec);
    }

    /**
     * @param elements the elements to save
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress information
     * @throws CanceledExecutionException if the operation is canceled
     */
    protected abstract void saveElements(final Collection<T> elements,
            final ConfigWO config, final ExecutionMonitor exec)
    throws CanceledExecutionException;
}
