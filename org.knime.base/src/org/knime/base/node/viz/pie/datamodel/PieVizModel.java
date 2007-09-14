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

package org.knime.base.node.viz.pie.datamodel;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.AggregationModel;
import org.knime.base.node.viz.aggregation.AggregationValModel;
import org.knime.base.node.viz.aggregation.AggregationValSubModel;
import org.knime.base.node.viz.aggregation.HiliteShapeCalculator;
import org.knime.base.node.viz.pie.util.GeometryUtil;
import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class PieVizModel extends PieDataModel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PieVizModel.class);

    /**The size of the root pie in percentage of the available space.*/
    public static final double PIE_SIZE_FACTOR = 0.6;

    /**The size of the exploded pie in percentage of the available space.*/
    public static final double EXPLODE_SIZE_FACTOR = 0.3;


    /**
     * The hilite calculator for the pie chart.
     * @author Tobias Koetter, University of Konstanz
     */
    public class PieHiliteCalculator
    implements HiliteShapeCalculator<Arc2D, Arc2D> {

        /**Constructor for class PieHiliteCalculator.*/
        protected PieHiliteCalculator() {
            //avoid object creation
        }

        /**
         * @return the current aggregation method
         */
        public AggregationMethod getAggrMethod() {
            return getAggregationMethod();
        }

        /**
         * {@inheritDoc}
         */
        public Arc2D calculateHiliteShape(
                final AggregationValModel<AggregationValSubModel<Arc2D, Arc2D>,
                Arc2D, Arc2D> model) {
            final double fraction;
            if (model.getRowCount() == 0) {
                fraction = 0;
            } else {
                fraction = model.getHiliteRowCount()
                / (double)model.getRowCount();
            }
            return GeometryUtil.calculateSubArc(model.getShape(),
                    fraction);
        }

        /**
         * {@inheritDoc}
         */
        public Arc2D calculateHiliteShape(
                final AggregationValSubModel<Arc2D, Arc2D> model) {
            final double fraction;
            if (model.getRowCount() == 0) {
                fraction = 0;
            } else {
                fraction = model.getHiliteRowCount()
                / (double)model.getRowCount();
            }
            return GeometryUtil.calculateSubArc(model.getShape(),
                    fraction);
        }
    }

    private boolean m_showMissingValSection = true;

    private Dimension m_drawingSpace;

    private AggregationMethod m_aggrMethod;

    private boolean m_showDetails = false;

    private boolean m_drawSectionOutline = true;

    private boolean m_drawAntialias = true;

    private final PieHiliteCalculator m_calculator = new PieHiliteCalculator();

    /**Constructor for class PieVizModel.
     * @param model the data model
     */
    public PieVizModel(final PieDataModel model) {
        super(model.getSections(), model.getMissingSection(),
                model.supportsHiliting());
    }


    /**
     * @return the calculator
     */
    public PieHiliteCalculator getCalculator() {
        return m_calculator;
    }

    /**
     * @return <code>true</code> if a section with the missing values should
     * be displayed
     */
    public boolean showMissingValSection() {
        return m_showMissingValSection;
    }

    /**
     * @param showMissingValSection <code>true</code> if the missing value
     * section should be displayed if it's available
     */
    public void setShowMissingValSection(final boolean showMissingValSection) {
        m_showMissingValSection = showMissingValSection;
    }

    /**
     * @param showDetails <code>true</code> if also the sub sections should
     * be displayed
     */
    public void setShowDetails(final boolean showDetails) {
        m_showDetails = showDetails;
    }

    /**
     * @return <code>true</code> if details are shown
     */
    public boolean showDetails() {
        return m_showDetails;
    }


    /**
     * @param drawSectionOutline <code>true</code> if the section outline
     * should be drawn
     */
    public void setDrawSectionOutline(final boolean drawSectionOutline) {
        m_drawSectionOutline = drawSectionOutline;
    }


    /**
     * @return <code>true</code> if the section outline should be drawn
     */
    public boolean drawSectionOutline() {
        return m_drawSectionOutline;
    }


    /**
     * @param drawAntialias <code>true</code> if the shapes should be drawn
     * using antialiasing
     */
    public void setDrawAntialias(final boolean drawAntialias) {
        m_drawAntialias = drawAntialias;
    }


    /**
     * @return <code>true</code> if the shapes should be drawn using
     * antialiasing
     */
    public boolean drawAntialias() {
        return m_drawAntialias;
    }

    /**
     * @return the aggrMethod
     */
    public AggregationMethod getAggregationMethod() {
        return m_aggrMethod;
    }


    /**
     * @param aggrMethod the aggrMethod to set
     */
    public void setAggregationMethod(final AggregationMethod aggrMethod) {
        m_aggrMethod = aggrMethod;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNoOfSections() {
        if (m_showMissingValSection && hasMissingSection()) {
            return super.getNoOfSections() + 1;
        }
        return super.getNoOfSections();
    }

    /**
     * @return an unmodifiable <code>List</code> with all pie section
     * data models
     */
    @Override
    public List<PieSectionDataModel> getSections() {
        if (m_showMissingValSection && hasMissingSection()) {
            final List<PieSectionDataModel> sections =
                new ArrayList<PieSectionDataModel>(super.getSections());
            sections.add(getMissingSection());
            return Collections.unmodifiableList(sections);
        }
        return super.getSections();
    }

    /**
     * @return all sections to draw as a unmodifiable {@link List}
     */
    public List<? extends AggregationModel<? extends Shape, ? extends Shape>>
    getDrawSections() {
        return getSections();
    }

    /**
     * @return all sub sections to draw as a unmodifiable {@link List}
     */
    public List<? extends AggregationModel<? extends Shape, ? extends Shape>>
    getDrawSubSections() {
        final List<PieSectionDataModel> sections = getSections();
        final List<PieSubSectionDataModel> detailSections =
            new ArrayList<PieSubSectionDataModel>(sections.size() * 2);
        for (final PieSectionDataModel section : sections) {
            final Collection<PieSubSectionDataModel> subSections =
                section.getElements();
            for (final PieSubSectionDataModel subSection : subSections) {
                detailSections.add(subSection);
            }
        }
        return Collections.unmodifiableList(detailSections);
    }

    /**
     * @return the actual drawing space
     */
    public Dimension getDrawingSpace() {
        return m_drawingSpace;
    }

    /**
     * @param drawingSpace the drawingSpace to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setDrawingSpace(final Dimension drawingSpace) {
        if (drawingSpace == null) {
            throw new IllegalArgumentException(
                    "Drawing space must not be null");
        }
        if (drawingSpace.equals(m_drawingSpace)) {
            return false;
        }
        m_drawingSpace = drawingSpace;
        return true;
    }

    /**
     * @return the percentage of the section explosion
     */
    public double getExplodePercentage() {
        return EXPLODE_SIZE_FACTOR;
    }

    public Rectangle2D getLinkArea() {
        final Dimension plotArea = getDrawingSpace();
        final double labelWidth = 0.0;
        final double gapHorizontal
            = plotArea.getWidth() * (0.25 + labelWidth);
        final double gapVertical = plotArea.getHeight() * 0.25;
        double linkX = 0 + gapHorizontal / 2;
        double linkY = 0 + gapVertical / 2;
        double linkW = plotArea.getWidth() - gapHorizontal;
        double linkH = plotArea.getHeight() - gapVertical;
        final double min = Math.min(linkW, linkH) / 2;
        linkX = (linkX + linkX + linkW) / 2 - min;
        linkY = (linkY + linkY + linkH) / 2 - min;
        linkW = 2 * min;
        linkH = 2 * min;

        // the link area defines the dog leg points for the linking lines to
        // the labels
        final Rectangle2D linkArea = new Rectangle2D.Double(
            linkX, linkY, linkW, linkH);
        return linkArea;
    }

    /**
     * @return the {@link Rectangle} to draw the exploded pie sections in
     */
    public Rectangle2D getExplodedArea() {
        final Rectangle2D linkArea = getLinkArea();
        // the explode area defines the max circle/ellipse for the exploded
        // pie sections.  it is defined by shrinking the linkArea by the
        // linkMargin factor.
        final double linkX = linkArea.getX();
        final double linkY = linkArea.getY();
        final double linkW = linkArea.getWidth();
        final double linkH = linkArea.getHeight();
        final double hh = linkArea.getWidth() * 0.05;
        final double vv = linkArea.getHeight() * 0.05;
        final Rectangle2D explodeArea = new Rectangle2D.Double(
            linkX + hh / 2.0, linkY + vv / 2.0, linkW - hh, linkH - vv
        );
        return explodeArea;
    }

    /**
     * @return the {@link Rectangle} to draw the pie
     */
    public Rectangle2D getPieArea() {
        final Rectangle2D explodeArea = getExplodedArea();
        // the pie area defines the circle/ellipse for regular pie sections.
        // it is defined by shrinking the explodeArea by the explodeMargin
        // factor.
        final double percent = EXPLODE_SIZE_FACTOR;
        final double h1 = explodeArea.getWidth() * percent;
        final double v1 = explodeArea.getHeight() * percent;
        final Rectangle2D pieArea = new Rectangle2D.Double(
            explodeArea.getX() + h1 / 2.0, explodeArea.getY() + v1 / 2.0,
            explodeArea.getWidth() - h1, explodeArea.getHeight() - v1
        );
        return pieArea;
    }

    /**
     * @return the total aggregation value of the pie
     */
    public double getAggregationValue() {
        return super.getAggregationValue(m_aggrMethod, m_showMissingValSection);
    }

    /**
     * Hilites the given keys in all sections.
     * @param keys the keys to (un)hilite
     * @param hilite <code>true</code> if the keys should be hilited
     */
    public void updateHiliteInfo(final Set<DataCell> keys,
            final boolean hilite) {
        LOGGER.debug("Entering updateHiliteInfo(hilited, hilite) "
                + "of class InteractiveHistogramVizModel.");
        if (keys == null || keys.size() < 1) {
            return;
        }
        final long startTime = System.currentTimeMillis();
        final PieHiliteCalculator calculator = getCalculator();
        for (final PieSectionDataModel pieSection : super.getSections()) {
            if (hilite) {
                pieSection.setHilitedKeys(keys, calculator);
            } else {
                pieSection.removeHilitedKeys(keys, calculator);
            }
        }
        if (hasMissingSection()) {
            if (hilite) {
                getMissingSection().setHilitedKeys(keys, calculator);
            } else {
                getMissingSection().removeHilitedKeys(keys, calculator);
            }
        }
        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for updateHiliteInfo: " + durationTime + " ms");
        LOGGER.debug("Exiting updateHiliteInfo(hilited, hilite) "
                + "of class InteractiveHistogramVizModel.");
    }

    /**
     * Removes the hilite information from all sections.
     */
    public void unHiliteAll() {
        final long startTime = System.currentTimeMillis();
        for (final PieSectionDataModel pieSection : super.getSections()) {
            pieSection.clearHilite();
        }
        if (hasMissingSection()) {
            getMissingSection().clearHilite();
        }
        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for unHiliteAll: " + durationTime + " ms");
    }

    /**
     * @return the keys of all selected sections
     */
    public Set<DataCell> getSelectedKeys() {
        final Set<DataCell> keys = new HashSet<DataCell>();
        for (final PieSectionDataModel section : getSections()) {
            if (section.isSelected()) {
                final Collection<PieSubSectionDataModel> subSections =
                    section.getElements();
                for (final PieSubSectionDataModel subSect : subSections) {
                    if (subSect.isSelected()) {
                        keys.addAll((subSect).getKeys());
                    }
                }
            }
        }
        return keys;
    }

    /**
     * Clear all selections.
     */
    public void clearSelection() {
        for (final PieSectionDataModel section : getSections()) {
            section.setSelected(false);
        }
    }

    /**
     * Selects the element which contains the given point.
     * @param point the point on the screen to select
     * @return <code>true</code> if the selection has changed
     */
    public boolean selectElement(final Point point) {
        boolean changed = false;
        for (final PieSectionDataModel section : getSections()) {
            changed = section.selectElement(point, showDetails())
            || changed;
        }
        return changed;
    }

    /**
     * Selects the element which contains the given point.
     * @param rect the rectangle on the screen to select
     * @return <code>true</code> if the selection has changed
     */
    public boolean selectElement(final Rectangle rect) {
        boolean changed = false;
        for (final PieSectionDataModel section : getSections()) {
            changed = section.selectElement(rect, showDetails()) || changed;
        }
        return changed;
    }
}
