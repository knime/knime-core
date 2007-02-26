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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.StringCell;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class HistogramVizModel {

    /** The caption of the bar which holds all missing values. */
    public static final String MISSING_VAL_BAR_CAPTION = "Missing_values";

    /**
     * The default number of bars which get created if the createBinnedBars
     * method is called with a number smaller then 1.
     */
    public static final int DEFAULT_NO_OF_BINS = 10;

    /**
     * Defines the maximum number of decimal places which are used in the
     * binning method.
     */
    public static final int INTERVAL_DIGITS = 2;

    /**
     * The space between to bins in pixels.
     */
    public static final int SPACE_BETWEEN_BINS = 2;

    /**
     * The width of the hilite rectangle in percent of the surrounding
     * rectangle. Should be greater 0 and less than 1. 0.8 = 80%
     */
    public static final double HILITE_RECTANGLE_WIDTH_FACTOR = 0.5;

    /**Compare the caption of bins.*/
    protected static final BinDataModelComparator BIN_CAPTION_COMPARATOR = 
        new BinDataModelComparator(BinDataModelComparator.COMPARE_CAPTION);

    private final SortedSet<Color> m_rowColors;

    private boolean m_binNominal = false;

    private AggregationMethod m_aggrMethod;

    private HistogramLayout m_layout;

    private boolean m_showMissingValBin = true;

    /**
     *The plotter will show all bars empty or not when set to <code>true</code>.
     */
    private boolean m_showEmptyBins = false;

    private final BinDataModel m_missingValueBin = new BinDataModel(
            HistogramVizModel.MISSING_VAL_BAR_CAPTION, 0, 0);

    /**Constructor for class HistogramVizModel.
     * @param rowColors all possible colors the user has defined for a row
     * @param layout the {@link HistogramLayout} to use
     * @param aggrMethod the {@link AggregationMethod} to use
     */
    public HistogramVizModel(final SortedSet<Color> rowColors,
            final AggregationMethod aggrMethod, final HistogramLayout layout) {
        if (rowColors == null) {
            throw new IllegalArgumentException(
                    "Bar elements shouldn't be null");
        }
        m_rowColors = rowColors;
        m_aggrMethod = aggrMethod;
        m_layout = layout;
    }

    /**
     * @return all {@link BinDataModel} objects of this histogram including
     * the missing value bin if the showMissingValue bin variable is set to
     * <code>true</code>
     */
    public abstract Collection<BinDataModel> getBins();

    /**
     * @return the aggregation columns
     */
    public abstract Collection<ColorColumn> getAggrColumns();

    /**
     * @return the x column specification
     */
    public abstract DataColumnSpec getXColumnSpec();

    /**
     * @return the x column name
     */
    public abstract String getXColumnName();

    /**
     * @return all available element colors. This is the color the user has
     * set for one attribute in the ColorManager node.
     */
    public SortedSet<Color> getRowColors() {
        return m_rowColors;
    }

    /**
     * @return the noOfBins without the missing value bin
     */
    public abstract int getNoOfBins();

    /**
     * @param noOfBins the new number of bins to create
     * @return <code>true</code> if the number of bins has changed
     */
    public abstract boolean setNoOfBins(final int noOfBins);

    /**
     * @param caption the caption of the bin of interest
     * @return the bin with the given caption or <code>null</code> if no bin
     * with the given caption exists
     */
    public BinDataModel getBin(final String caption) {
        for (final BinDataModel bin : getBins()) {
            if (bin.getXAxisCaption().equals(caption)) {
                return bin;
            }
        }
        return null;
    }

    /**
     * @return all bin captions in the order they should be displayed
     */
    public Set<DataCell> getBinCaptions() {
        final Collection<BinDataModel> bins = getBins();
        final LinkedHashSet<DataCell> captions = 
            new LinkedHashSet<DataCell>(bins.size());
        for (final BinDataModel bin : bins) {
            if (m_showEmptyBins || bin.getMaxBarRowCount() > 0) {
                captions.add(new StringCell(bin.getXAxisCaption()));
            }
        }
        if (m_showMissingValBin && m_missingValueBin.getMaxBarRowCount() > 0) {
            captions.add(new StringCell(m_missingValueBin.getXAxisCaption()));
        }
        return captions;
    }

    /**
     * @return <code>true</code> if the bins are nominal or
     * <code>false</code> if the bins are intervals
     */
    public boolean isBinNominal() {
        return m_binNominal;
    }

    /**
     * @return the maximum aggregation value
     */
    public double getMaxAggregationValue() {
        double maxAggrValue = Double.MIN_VALUE;
        for (final BinDataModel bin : getBins()) {
            final double value = bin.getMaxAggregationValue(m_aggrMethod,
                    m_layout);
            if (value > maxAggrValue) {
                maxAggrValue = value;
            }
        }
        return maxAggrValue;
    }

    /**
     * @return the minimum aggregation value
     */
    public double getMinAggregationValue() {
        double minAggrValue = Double.MAX_VALUE;
        for (final BinDataModel bin : getBins()) {
            final double value = bin.getMinAggregationValue(m_aggrMethod,
                    m_layout);
            if (value < minAggrValue) {
                minAggrValue = value;
            }
        }
        return minAggrValue;
    }

    /**
     * @return the missingValueBin or <code>null</code> if the selected
     * x column contains no missing values
     */
    public BinDataModel getMissingValueBin() {
        if (m_missingValueBin.getMaxBarRowCount() == 0) {
            return null;
        }
        return m_missingValueBin;
    }

    /**
     * @return <code>true</code> if this model contains a missing value bin
     */
    public boolean containsMissingValueBin() {
        return (getMissingValueBin() != null);
    }

    /**
     * @return <code>true</code> if the histogram contains at least one
     * bin with no rows in it.
     */
    public boolean containsEmptyBins() {
        for (final BinDataModel bin : getBins()) {
            if (bin.getBinRowCount() < 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return <code>true</code> if the empty bins should be displayed
     */
    public boolean isShowEmptyBins() {
        return m_showEmptyBins;
    }

    /**
     * @param showEmptyBins set to <code>true</code> if also the empty bins 
     * should be shown
     * @return <code>true</code> if the variable has changed
     */
    public boolean setShowEmptyBins(final boolean showEmptyBins) {
        if (showEmptyBins != m_showEmptyBins) {
            m_showEmptyBins = showEmptyBins;
            return true;
        }
        return false;
    }

    /**
     * @return the aggregation method which is used to calculate the 
     * aggregation value
     */
    public AggregationMethod getAggregationMethod() {
        return m_aggrMethod;
    }

    /**
     * @param aggrMethod the aggregation method to use to calculate
     * the aggregation value
     * @return <code>true</code> if the aggregation method has changed
     */
    public boolean setAggregationMethod(final AggregationMethod aggrMethod) {
        if (m_aggrMethod.equals(aggrMethod)) {
            return false;
        }
        m_aggrMethod = aggrMethod;
        return true;
    }

    /**
     * @return the layout
     */
    public HistogramLayout getHistogramLayout() {
        return m_layout;
    }

    /**
     * @param layout the layout to set
     * @return <code>true</code> if the layout has changed
     */
    public boolean setHistogramLayout(final HistogramLayout layout) {
        if (layout != null && !m_layout.equals(layout)) {
            m_layout = layout;
            return true;
        }
        return false;
    }

    /**
     * @return the inclMissingValBin
     */
    public boolean isShowMissingValBin() {
        return m_showMissingValBin;
    }

    /**
     * @param inclMissingValBin the inclMissingValBin to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setShowMissingValBin(final boolean inclMissingValBin) {
        if (m_showMissingValBin == inclMissingValBin) {
            return false;
        }
        m_showMissingValBin = inclMissingValBin;
        return true;
    }

    /**
     * @return all keys of hilited rows
     */
    public Set<DataCell> getHilitedKeys() {
        final Set<DataCell> keys = new HashSet<DataCell>();
        for (final BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                if (bar.isSelected()) {
                    final Collection<BarElementDataModel> elements = bar
                            .getElements();
                    for (final BarElementDataModel element : elements) {
                        keys.addAll(element.getHilitedKeys());
                    }
                }
            }
        }
        return keys;
    }

    /**
     * @param hilite <code>true</code> if the selected elements should be 
     * hilited or <code>false</code> if they should be unhilited
     * @return all keys of the selected elements
     */
    public Set<DataCell> getSelectedKeys(final boolean hilite) {
        final Set<DataCell> keys = new HashSet<DataCell>();
        for (final BinDataModel bin : getBins()) {
            if (bin.isSelected()) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (final BarDataModel bar : bars) {
                    if (bar.isSelected()) {
                        final Collection<BarElementDataModel> elements = bar
                                .getElements();
                        for (final BarElementDataModel element : elements) {
                            if (element.isSelected()) {
                                keys.addAll(element.getKeys());
                                if (hilite) {
                                    element.setHilitedKeys(keys, m_aggrMethod);
                                } else {
                                    element.clearHilite();
                                }
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
        for (final BinDataModel bin : getBins()) {
            final Rectangle binRectangle = bin.getBinRectangle();
            if (binRectangle != null && binRectangle.contains(point)) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (final BarDataModel bar : bars) {
                    final Rectangle barRectangle = bar.getBarRectangle();
                    if (barRectangle != null && barRectangle.contains(point)) {
                        bar.setSelected(true);
                        final Collection<BarElementDataModel> elements = bar
                                .getElements();
                        for (final BarElementDataModel element : elements) {
                            final Rectangle elementRectangle = element
                                    .getElementRectangle();
                            //if the bar is to small to draw the different
                            //elements we have to select all elements 
                            //of this bar
                            if (!bar.isDrawElements()
                                    || (elementRectangle != null 
                                        && elementRectangle.contains(point))) {
                                element.setSelected(true);
                                return;
                            }
                        }
                    }
                }
            }
        }
        return;
    }

    /**
     * Selects all elements which are touched by the given rectangle.
     * @param rect the rectangle on the screen select
     */
    public void selectElement(final Rectangle rect) {
        for (final BinDataModel bin : getBins()) {
            final Rectangle binRectangle = bin.getBinRectangle();
            if (binRectangle != null && binRectangle.intersects(rect)) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (final BarDataModel bar : bars) {
                    final Rectangle barRectangle = bar.getBarRectangle();
                    if (barRectangle != null && barRectangle.intersects(rect)) {
                        bar.setSelected(true);
                        final Collection<BarElementDataModel> elements = bar
                                .getElements();
                        for (final BarElementDataModel element : elements) {
                            final Rectangle elementRectangle = element
                                    .getElementRectangle();
                            //if the bar is to small to draw the different
                            //elements we have to select all elements 
                            //of this bar
                            if (!bar.isDrawElements()
                                    || (elementRectangle != null 
                                        && elementRectangle.intersects(rect))) {
                                element.setSelected(true);
                            }
                        }
                    }
                }
            }
        }
        return;
    }

    /**
     * Clears all selections.
     */
    public void clearSelection() {
        for (final BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                bar.setSelected(false);
                final Collection<BarElementDataModel> elements = bar
                        .getElements();
                for (final BarElementDataModel element : elements) {
                    element.setSelected(false);
                }
            }
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
        for (final BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                final Collection<BarElementDataModel> elements = bar
                        .getElements();
                for (final BarElementDataModel element : elements) {
                    if (hilite) {
                        element.setHilitedKeys(hilited, m_aggrMethod);
                    } else {
                        element.removeHilitedKeys(hilited, m_aggrMethod);
                    }
                }
            }
        }
    }

    /**
     * Unhilites all rows.
     */
    public void unHiliteAll() {
        for (final BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                final Collection<BarElementDataModel> elements = bar
                        .getElements();
                for (final BarElementDataModel element : elements) {
                    element.clearHilite();
                }
            }
        }
    }

    /**
     * @return a HTML <code>String</code> which contains details information
     * about the current selected elements
     */
    public String getHTMLDetailData() {
        final StringBuilder buf = new StringBuilder();
        buf.append("<h2>Details data</h2>");
        buf.append("Nothing selected");
        return buf.toString();
    }

    /**
     * @param nominal set to <code>true</code> if the nominal binning method
     * should be used.
     */
    protected void setBinNominal(final boolean nominal) {
        m_binNominal = nominal;
    }
}
