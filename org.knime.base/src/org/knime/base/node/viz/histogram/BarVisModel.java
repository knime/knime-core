/*
 * ------------------------------------------------------------------
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
 */
package org.knime.base.node.viz.histogram;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;


/**
 * Class which represents a Histogram bar for drawing.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class BarVisModel {
    /** The <code>AbstractBarDataModel</code> which holds the basic 
     * information. */
    private AbstractBarDataModel m_bar;

    /** The drawing space on the screen. */
    private Rectangle m_rect;

    /**
     * The <code>DataTableSpec</code> which holds the color information for
     * all rows in the <code>AbstractBarDataModel</code>.
     */
    private final DataTableSpec m_tableSpec;

    /**
     * <code>Hashtable</code> with all <code>ColorAttr</code> objects which
     * are represented in this bar as key and the associated
     * <code>DataRow</code> objects as <code>Collection</code> as values.
     */
    private final Hashtable<ColorAttr, Collection<RowKey>> m_rowsByColor;

    /**
     * All <code>ColorAttr</code> objects which are represented in this bar in
     * a defined order.
     */
    private final Collection<ColorAttr> m_sortedColors;

    /**
     * Indicates if this bar is selected(<code>true</code>) by the user or
     * not (<code>false</code>).
     */
    private boolean m_isSelected = false;

    /**
     * Constructor for class BarVisModel.
     * 
     * @param bar the <code>AbstractBarDataModel</code> which contains the data
     * @param rect the <code>Rectangle</code> which defines the drawing space
     * @param tableSpec the <code>DataTableSpec</code> which contains the
     *            color and size information
     */
    protected BarVisModel(final AbstractBarDataModel bar, final Rectangle rect,
            final DataTableSpec tableSpec) {
        if (bar == null || rect == null || tableSpec == null) {
            throw new IllegalArgumentException("Bar, rectangle and tableSpec "
                    + "shouldn't be null.");
        }
        m_bar = bar;
        m_rect = rect;
        m_tableSpec = tableSpec;
        m_rowsByColor = m_bar.createColorInformation(m_tableSpec);
        m_sortedColors = sortColorInformation(m_rowsByColor.keySet());
    }

    /**
     * @return the rectangle on the screen where this bar gets painted
     */
    protected Rectangle getRectangle() {
        return m_rect;
    }

    /**
     * Checks if the rectangle is overlapping the screen rectangle of the bar.
     * 
     * @param r the rectangle to check
     * @return true if the screen rectangle of the bar and the specified
     *         rectangle are overlapping
     */
    protected boolean screenRectOverlapping(final Rectangle r) {
        if ((r == null) || (m_rect == null)) {
            return false;
        }
        if ((r.x <= m_rect.x + m_rect.width - 1)
                && (m_rect.x <= r.x + r.width - 1)
                && (r.y <= m_rect.y + m_rect.height - 1)
                && (m_rect.y <= r.y + r.height - 1)) {
            return true;
        }
        return false;
    }

    /**
     * The number of rows in this bar.
     * 
     * @return number of rows in this bar
     */
    public int getNumberOfRows() {
        return m_bar.getNumberOfRows();
    }

    /**
     * @return the row key of all rows which belong to this bar
     */
    protected Collection<? extends DataCell> getRowKeys() {
        return m_bar.getRowKeys();
    }
    
    /**
     * @return <code>true</code> if this bar is user selected
     */
    protected boolean isSelected() {
        return m_isSelected;
    }

    /**
     * @param b <code>true</code> if this bar should be selected
     */
    protected void setSelected(final boolean b) {
        m_isSelected = b;
    }

    /**
     * Returns all rows which are decorated with the given
     * <code>ColorAttr</code>.
     * 
     * @param colorAttr the <code>ColorAttr</code> we want the rows for
     * @return the rows with the given <code>ColorAttr</code>
     */
    protected Collection<RowKey> getRowsByColorAttr(final ColorAttr colorAttr) {
        return m_rowsByColor.get(colorAttr);
    }

    /**
     * @return gets all colors which are represented in this bar in a defined
     *         order.
     */
    protected Collection<ColorAttr> getSortedColors() {
        return m_sortedColors;
    }

    /**
     * @return the value for the current aggregation method
     */
    protected double getAggregationValue() {
        return m_bar.getAggregationValue();
    }

    /**
     * @return the caption of the bar
     */
    protected String getCaption() {
        return m_bar.getCaption();
    }

    /**
     * @return the aggregation value label of this bar
     */
    protected String getLabel() {
        return m_bar.getLabel();
    }

    /**
     * Sorts the given <code>Collection</code> with <code>ColorAttr</code>
     * objects by using the <code>ColorAttrComparator</code>.
     * 
     * @param colors the <code>Collection</code> to sort
     * @return the sorted <code>Collection</code>
     */
    private static Collection<ColorAttr> sortColorInformation(
            final Collection<ColorAttr> colors) {
        final List<ColorAttr> sortedColors = new ArrayList<ColorAttr>(colors);
        final Comparator<ColorAttr> comp = new HSBColorAttrComparator(false);
        Collections.sort(sortedColors, comp);
        return sortedColors;
    }

    /**
     * Updates this <code>BarVisModel</code> with the new data.
     * 
     * @param bar the new <code>AbstractBarDataModel</code>
     * @param rect the new<code>Rectangle</code>
     */
    public void updateBarData(final AbstractBarDataModel bar, 
            final Rectangle rect) {
        m_bar = bar;
        m_rect = rect;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(m_bar.toString());
        buf.append("\n");
        buf.append("Is selected: ");
        buf.append(m_isSelected ? "true" : "false");
        buf.append("\n");
        buf.append("Drawing rectangle: ");
        buf.append(m_rect.toString());
        buf.append("Number of rows by color: ");
        for (ColorAttr colorAttr : m_sortedColors) {
            buf.append("Color attribute: ");
            buf.append(colorAttr.toString());
            buf.append("\t");
            buf.append(m_rowsByColor.get(colorAttr).size());
        }
        return buf.toString();
    }
}
