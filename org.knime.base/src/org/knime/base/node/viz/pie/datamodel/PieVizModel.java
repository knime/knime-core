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

    /**The percentage of the drawing space that should be used for drawing.
     * (0.9 = 90 percent)*/
    public static final double DRAWING_SPACE_SIZE = 0.99;

    /**The margin of the label area in percent of the drawing space size.
     * (0.2 = 20 percent).*/
    public static final double LABEL_AREA_MARGIN = 0.3;

    /**The margin of the explode are in percent of the label are rectangle.
     * (0.2 = 20 percent)*/
    public static final double EXPLODE_AREA_MARGIN = 0.1;

    /**The default minimum arc angle of a pie section to draw.*/
    public static final double MINIMUM_ARC_ANGLE = 0.0001;



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
     * @return the {@link Rectangle} that defines the maximum surrounding of
     * the label are which includes the {@link #getExplodedArea()}
     */
    public Rectangle2D getLabelArea() {
        final Dimension drawingSpace = getDrawingSpace();
//        final double labelMargin = drawingSpace.getWidth() * LABEL_AREA_MARGIN;
//        double rootX = labelMargin / 2;
//        double rootY = rootX;
//        double areaWidth = drawingSpace.getWidth() - labelMargin;
//        double areaHeight = drawingSpace.getHeight() - labelMargin;
//        final double diameter = Math.min(areaWidth, areaHeight);
//        final double radius = diameter / 2;
//        rootX = (rootX + rootX + areaWidth) / 2 - radius;
//        rootY = (rootY + rootY + areaHeight) / 2 - radius;
//        areaWidth = diameter;
//        areaHeight = diameter;
        final double areaWidth = drawingSpace.getWidth() * DRAWING_SPACE_SIZE;
        final double areaHeight = drawingSpace.getHeight() * DRAWING_SPACE_SIZE;
        final double centerX = drawingSpace.getWidth() / 2;
        final double centerY = drawingSpace.getHeight() / 2;
        final double diameter = Math.min(areaWidth, areaHeight);
        final double radius = diameter / 2;
        final double rectX = centerX - radius;
        final double rectY = centerY - radius;
        final Rectangle2D linkArea = new Rectangle2D.Double(rectX, rectY,
                diameter, diameter);
        return linkArea;
    }

    /**
     * @return the center point of the pie
     */
    public Point getPieCenter() {
        final Rectangle2D area = getLabelArea();
        return new Point((int)area.getCenterX(), (int)area.getCenterY());
    }

    /**
     * @return the size of the label links
     */
    public double getLabelLinkSize() {
        return getExplodedArea().getWidth() * EXPLODE_AREA_MARGIN / 2;
    }

    /**
     * @return the {@link Rectangle} that defines the maximum surrounding of
     * the exploded sections which includes the {@link #getPieArea()} rectangle
     */
    public Rectangle2D getExplodedArea() {
        return calculateSubRectangle(getLabelArea(), LABEL_AREA_MARGIN);
    }

    /**
     * @return the {@link Rectangle} to draw the pie in which is surrounded
     * by the {@link #getExplodedArea()} rectangle which in turn is surrounded
     * by the {@link #getLabelArea()} rectangle.
     */
    public Rectangle2D getPieArea() {
        return calculateSubRectangle(getExplodedArea(), EXPLODE_AREA_MARGIN);
    }

    private final Rectangle2D calculateSubRectangle(final Rectangle2D rect,
            final double margin) {
        final double origWidth = rect.getWidth();
        final double origHeight = rect.getHeight();
        final double widthMarign = origWidth * margin;
        final double heightMargin = origHeight * margin;
        final double rectX = rect.getX() + widthMarign / 2.0;
        final double rectY = rect.getY() + heightMargin / 2.0;
        final double rectWidth = origWidth - widthMarign;
        final double rectHeight = origHeight - heightMargin;
        return new Rectangle2D.Double(rectX, rectY, rectWidth, rectHeight);
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
