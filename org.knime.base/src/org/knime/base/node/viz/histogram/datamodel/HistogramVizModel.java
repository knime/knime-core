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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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

    protected abstract void createBins();

    public abstract Collection<BinDataModel> getBins();

    public abstract List<ColorColumn> getAggrColumns();

    public abstract DataColumnSpec getXColumnSpec();

    public abstract String getXColumnName();

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
    protected static final BinDataModelComparator BIN_CAPTION_COMPARATOR = 
        new BinDataModelComparator(BinDataModelComparator.COMPARE_CAPTION);
    protected final SortedSet<Color> m_barElementColors;
    protected final List<BinDataModel> m_bins = new ArrayList<BinDataModel>(50);
    protected int m_noOfBins;
    protected boolean m_binNominal = false;
    protected AggregationMethod m_aggrMethod;
    protected HistogramLayout m_layout;
    protected boolean m_showMissingValBin = true;
    /**
     *The plotter will show all bars empty or not when set to <code>true</code>.
     */
    private boolean m_showEmptyBins = false;
    protected BinDataModel m_missingValueBin = new BinDataModel(HistogramVizModel.MISSING_VAL_BAR_CAPTION, 0, 0);
    /**
     * The width of the hilite rectangle in percent of the surrounding
     * rectangle. Should be greater 0 and less than 1. 0.8 = 80%
     */
    public static final double HILITE_RECTANGLE_WIDTH_FACTOR = 0.5;

    /**Constructor for class HistogramVizModel.
     * @param barElements 
     * 
     */
    public HistogramVizModel(final SortedSet<Color> barElements) {
        if (barElements == null) {
            throw new IllegalArgumentException(
                    "Bar elements shouldn't be null");
        }
        m_barElementColors = barElements;
    }

    /**
     * @return all available element colors. This is the color the user has
     * set for one attribute in the ColorManager node.
     */
    public SortedSet<Color> getBarElementColors() {
        return m_barElementColors;
    }

    /**
     * @return the noOfBins
     */
    public int getNoOfBins() {
        return m_noOfBins;
    }

    /**
     * @param noOfBins the new number of bins to create
     * @return <code>true</code> if the number of bins has changed
     */
    public boolean setNoOfBins(final int noOfBins) {
        if (m_binNominal) {
            throw new IllegalArgumentException(
                    "Not possible for nominal binning");
        }
        if (m_noOfBins == noOfBins) {
            return false;
        }
        m_noOfBins = noOfBins;
        createBins();
        return true;
    }

    /**
     * @param caption the caption of the bin of interest
     * @return the bin with the given caption or <code>null</code> if no bin
     * with the given caption exists
     */
    public BinDataModel getBin(final String caption) {
        for (BinDataModel bin : getBins()) {
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
        LinkedHashSet<DataCell> captions = 
            new LinkedHashSet<DataCell>(bins.size());
        for (BinDataModel bin : bins) {
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
        for (BinDataModel bin : getBins()) {
            final double value = 
                bin.getMaxAggregationValue(m_aggrMethod, m_layout);
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
        for (BinDataModel bin : getBins()) {
            final double value = 
                bin.getMinAggregationValue(m_aggrMethod, m_layout);
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
        for (BinDataModel bin : getBins()) {
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
        Set<DataCell> keys = new HashSet<DataCell>();
        for (BinDataModel bin : getBins()) {            
            final Collection<BarDataModel> bars = bin.getBars();
            for (BarDataModel bar : bars) {
                if (bar.isSelected()) {
                    final Collection<BarElementDataModel> elements = 
                        bar.getElements();
                    for (BarElementDataModel element : elements) {
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
        Set<DataCell> keys = new HashSet<DataCell>();
        for (BinDataModel bin : getBins()) {
            if (bin.isSelected()) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (BarDataModel bar : bars) {
                    if (bar.isSelected()) {
                        final Collection<BarElementDataModel> elements = 
                            bar.getElements();
                        for (BarElementDataModel element : elements) {
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
        for (BinDataModel bin : getBins()) {
            final Rectangle binRectangle = bin.getBinRectangle();
            if (binRectangle != null && binRectangle.contains(point)) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (BarDataModel bar : bars) {
                    final Rectangle barRectangle = bar.getBarRectangle();
                    if (barRectangle != null && barRectangle.contains(point)) {
                        bar.setSelected(true);
                        final Collection<BarElementDataModel> elements = 
                            bar.getElements();
                        for (BarElementDataModel element : elements) {
                            final Rectangle elementRectangle = 
                                element.getElementRectangle();
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
        for (BinDataModel bin : getBins()) {
            final Rectangle binRectangle = bin.getBinRectangle();
            if (binRectangle != null && binRectangle.intersects(rect)) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (BarDataModel bar : bars) {
                    final Rectangle barRectangle = bar.getBarRectangle();
                    if (barRectangle != null && barRectangle.intersects(rect)) {
                        bar.setSelected(true);
                        final Collection<BarElementDataModel> elements = 
                            bar.getElements();
                        for (BarElementDataModel element : elements) {
                            final Rectangle elementRectangle = 
                                element.getElementRectangle();
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
        for (BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (BarDataModel bar : bars) {
                bar.setSelected(false);
                final Collection<BarElementDataModel> elements = bar
                        .getElements();
                for (BarElementDataModel element : elements) {
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
    public void updateHiliteInfo(final Set<DataCell> hilited, final boolean hilite) {
        if (hilited == null || hilited.size() < 1) {
            return;
        }
        for (BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (BarDataModel bar : bars) {
                final Collection<BarElementDataModel> elements = 
                    bar.getElements();
                for (BarElementDataModel element : elements) {
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
            for (BinDataModel bin : getBins()) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (BarDataModel bar : bars) {
                    final Collection<BarElementDataModel> elements = bar
                            .getElements();
                    for (BarElementDataModel element : elements) {
                        element.clearHilite();
                    }
                }
            }        
        }
    //*******************Binning functions**********************************

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

}
