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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.AggregationModel;
import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;


/**
 * The pie chart visualization model which extends the {@link PieDataModel}
 * with view specific information like flags for showing the labels,
 * selected section drawing...
 * @author Tobias Koetter, University of Konstanz
 */
public class PieVizModel extends PieDataModel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PieVizModel.class);

    /**The number of digits to display for a label.*/
    private static final int NO_OF_LABEL_DIGITS = 2;
    /** The caption of the bar which holds all missing values. */
    public static final String MISSING_VAL_SECTION_CAPTION = "Missing_values";
    /** The caption of the bar which holds all missing values. */
    public static final Color MISSING_VAL_SECTION_COLOR = Color.LIGHT_GRAY;

    /**The percentage of the drawing space that should be used for drawing.
     * (0.9 = 90 percent)*/
    public static final double DEFAULT_PIE_SIZE = 0.99;

    /**The minimum size of the pie drawing space in percent.
     * (0.3 = 30 percent).
     */
    public static final double MINIMUM_PIE_SIZE = 0.3;

    /**The margin of the label area in percent of the drawing space size.
     * (0.2 = 20 percent).*/
    public static final double LABEL_AREA_MARGIN = 0.3;

    /**The margin of the explode are in percent of the label are rectangle.
     * (0.2 = 20 percent)*/
    public static final double EXPLODE_AREA_MARGIN = 0.1;

    /**The default minimum arc angle of a pie section to draw.*/
    public static final double MINIMUM_ARC_ANGLE = 0.0001;

    private boolean m_showEmptySections = false;

    private boolean m_showMissingValSection = true;

    private Dimension m_drawingSpace;

    private AggregationMethod m_aggrMethod;

    private boolean m_showDetails = false;

    private boolean m_drawSectionOutline = true;

    private boolean m_explodeSelectedSections = false;

    private boolean m_drawAntialias = true;

    private double m_pieSize = DEFAULT_PIE_SIZE;

    private final PieHiliteCalculator m_calculator =
        new PieHiliteCalculator(this);

    /**Constructor for class PieVizModel.
     * @param model the data model
     */
    public PieVizModel(final PieDataModel model) {
        super(model.getSections(), model.getMissingSection(),
                model.supportsHiliting());
    }


    /**
     * @return the hilite shape calculator
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
     * @param showEmptySections <code>true</code> if empty sections
     * should be displayed
     */
    public void setShowEmptySections(final boolean showEmptySections) {
        m_showEmptySections = showEmptySections;
    }

    /**
     * @return <code>true</code> if the empty sections should be displayed
     */
    public boolean showEmptySections() {
        return m_showEmptySections;
    }

    /**
     * @param showDetails <code>true</code> if also the sub sections should
     * be displayed
     */
    public void setShowDetails(final boolean showDetails) {
        m_showDetails = showDetails;
    }

    /**
     * @return <code>true</code> if the sub sections of a section should be
     * displayed
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
     * @param explode <code>true</code> if selected sections should be
     * exploded drawn
     */
    public void setExplodeSelectedSections(final boolean explode) {
        m_explodeSelectedSections = explode;
    }

    /**
     * @return <code>true</code> if selected section should be exploded drawn
     */
    public boolean explodeSelectedSections() {
        return m_explodeSelectedSections;
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
     * @return the actual {@link AggregationMethod}
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
     * Returns the sections to draw depending on the showMissing and
     * emptySection flags.
     * @return all sections to draw as a unmodifiable {@link List}
     */
    public List<PieSectionDataModel> getSections2Draw() {
        final List<PieSectionDataModel> allSections = super.getSections();
        final List<PieSectionDataModel> resultList =
            new ArrayList<PieSectionDataModel>(allSections.size() + 1);
        for (final PieSectionDataModel section : allSections) {
            if (!section.isEmpty() || m_showEmptySections) {
                resultList.add(section);
            }
        }
        if (m_showMissingValSection && hasMissingSection()) {
            resultList.add(getMissingSection());
        }
        return Collections.unmodifiableList(resultList);
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
     * @return the size of the available drawing space
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
     * @return the {@link Rectangle2D} that defines the maximum surrounding of
     * the label area which includes the {@link #getExplodedArea()}
     */
    public Rectangle2D getLabelArea() {
        final Dimension drawingSpace = getDrawingSpace();
        final double areaWidth = drawingSpace.getWidth() * getPieSize();
        final double areaHeight = drawingSpace.getHeight() * getPieSize();
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
     * @param pieSize the pieSize in percent of the drawing space.
     * (0.9 = 90 percent)
     */
    public void setPieSize(final double pieSize) {
        if (pieSize < MINIMUM_PIE_SIZE) {
            m_pieSize = MINIMUM_PIE_SIZE;
        } else if (pieSize > 100) {
            m_pieSize = 100;
        } else {
            m_pieSize = pieSize;
        }
    }


    /**
     * @return the size of the pie in percent of the drawing space
     * (0.9 = 90 percent)
     */
    public double getPieSize() {
        return m_pieSize;
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
     * @return the {@link Rectangle2D} that defines the maximum surrounding of
     * the exploded sections which includes the {@link #getPieArea()} rectangle
     */
    public Rectangle2D getExplodedArea() {
        return calculateSubRectangle(getLabelArea(), LABEL_AREA_MARGIN);
    }

    /**
     * @return the {@link Rectangle2D} to draw the pie in which is surrounded
     * by the {@link #getExplodedArea()} rectangle which in turn is surrounded
     * by the {@link #getLabelArea()} rectangle.
     */
    public Rectangle2D getPieArea() {
        return calculateSubRectangle(getExplodedArea(), EXPLODE_AREA_MARGIN);
    }

    /**
     * @param rect the basic {@link Rectangle2D} to scale
     * @param margin the size of the margin in percent of the size of the
     * basic rectangle
     * @return the rectangle which has the size of the basic rectangle
     * minus the margin
     */
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
     * @return the total absolute aggregation value of the sections to draw
     */
    public double getAbsAggregationValue() {
        final AggregationMethod aggrMethod = getAggregationMethod();
        final List<PieSectionDataModel> sections = getSections2Draw();
        double sum = 0;
        for (final PieSectionDataModel section : sections) {
            sum += Math.abs(section.getAggregationValue(aggrMethod));
        }
        return sum;
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
     * Unhilites all sections.
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
     * Selects the element which intersects the given rectangle.
     * @param rect the rectangle on the screen to select
     * @return <code>true</code> if the selection has changed
     */
    public boolean selectElement(final Rectangle2D rect) {
        boolean changed = false;
        for (final PieSectionDataModel section : getSections()) {
            changed = section.selectElement(rect, showDetails()) || changed;
        }
        return changed;
    }


    /**
     * @param section the section to create the label for
     * @return the label of this section depending on the visualization flags
     */
    public String createLabel(final PieSectionDataModel section) {
        if (section == null) {
            throw new NullPointerException("Section must not be null");
        }
        final String name = section.getName();
        final AggregationMethod aggrMethod = getAggregationMethod();
        final double value = section.getAggregationValue(aggrMethod);
        final String valuePart = aggrMethod.createLabel(value,
                    NO_OF_LABEL_DIGITS);
        if (name != null) {
            return name + " " + aggrMethod.getText() + ": " + valuePart;
        }
        return valuePart;
    }
}
