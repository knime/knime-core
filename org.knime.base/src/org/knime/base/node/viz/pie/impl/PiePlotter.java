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

package org.knime.base.node.viz.pie.impl;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;

import javax.swing.JPopupMenu;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.pie.datamodel.PieSectionDataModel;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.pie.datamodel.PieVizModel.PieHiliteCalculator;
import org.knime.base.node.viz.pie.util.GeometryUtil;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.KeyEvent;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class PiePlotter extends AbstractPlotter {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PiePlotter.class);

    private PieVizModel m_vizModel;

    /**Constructor for class PiePlotter.
     * @param properties the properties panel
     * @param handler the optional <code>HiliteHandler</code>
     */
    public PiePlotter(final AbstractPlotterProperties properties,
            final HiLiteHandler handler) {
        super(new PieDrawingPane(), properties);
        if (handler != null) {
            super.setHiLiteHandler(handler);
        }
    }

    /**
     * Convenient method to cast the drawing pane.
     * @return the plotter drawing pane
     */
    protected PieDrawingPane getPieDrawingPane() {
        final PieDrawingPane myPane =
            (PieDrawingPane)getDrawingPane();
        if (myPane == null) {
            throw new IllegalStateException("Drawing pane must not be null");
        }
        return myPane;
    }

    /**
     * @param vizModel the vizModel to set
     */
    public void setVizModel(final PieVizModel vizModel) {
        m_vizModel = vizModel;
        m_vizModel.setDrawingSpace(getDrawingPaneDimension());
    }


    /**
     * @return the vizModel
     */
    public PieVizModel getVizModel() {
        return m_vizModel;
    }

    /**
     * Resets the visualization model.
     */
    public void resetVizModel() {
        m_vizModel = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        resetVizModel();
        super.setHiLiteHandler(null);
        getPieDrawingPane().reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSize() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            return;
        }
        final Dimension newDrawingSpace = getDrawingPaneDimension();
        if (vizModel.setDrawingSpace(newDrawingSpace)) {
            updatePaintModel();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePaintModel() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            LOGGER.debug("VizModel was null");
            return;
        }
        final PieDrawingPane drawingPane = getPieDrawingPane();
        setPieSections(vizModel);
        drawingPane.setVizModel(vizModel);
    }

    /**
     * @param vizModel
     */
    private void setPieSections(final PieVizModel vizModel) {
        final Rectangle2D pieArea = vizModel.getPieArea();
        final Rectangle2D explodedArea = vizModel.getExplodedArea();
//        final double explodePercentage = vizModel.getExplodeMargin();
        final double total = vizModel.getAggregationValue();
        final AggregationMethod method = vizModel.getAggregationMethod();
        final PieHiliteCalculator calculator = vizModel.getCalculator();
        final int noOfSections = vizModel.getNoOfSections();
        final List<PieSectionDataModel> pieSections = vizModel.getSections();
        int startAngle = 0;
        for (int i = 0; i < noOfSections; i++) {
            final PieSectionDataModel section = pieSections.get(i);
            final double value = section.getAggregationValue(method);
            int arcAngle = (int)(value * 360 / total);
            //avoid a rounding gap
            if (i == noOfSections - 1) {
                arcAngle = 360 - startAngle;
            }
            if (arcAngle < PieVizModel.MINIMUM_ARC_ANGLE) {
                //skip this section
                section.setPieSection(null, calculator);
                continue;
            }
            final Rectangle2D bounds;
            //explode selected sections
            if (section.isSelected()) {
                bounds = GeometryUtil.getArcBounds(pieArea, explodedArea,
                        startAngle, arcAngle, 1.0);
            } else {
                bounds = pieArea;
            }
            final Arc2D arc = new Arc2D.Double(bounds, startAngle, arcAngle,
                    Arc2D.PIE);
            section.setPieSection(arc, calculator);
            startAngle += arcAngle;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearSelection() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            return;
        }
        vizModel.clearSelection();
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectClickedElement(final Point clicked) {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            return;
        }
        vizModel.selectElement(clicked);
        updatePaintModel();
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectElementsIn(final Rectangle selectionRectangle) {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null) {
            return;
        }
        vizModel.selectElement(selectionRectangle);
        updatePaintModel();
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<DataCell>hilited = event.keys();
        vizModel.updateHiliteInfo(hilited, true);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<DataCell>hilited = event.keys();
        vizModel.updateHiliteInfo(hilited, false);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLiteSelected() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<DataCell> selectedKeys =
            vizModel.getSelectedKeys();
        delegateHiLite(selectedKeys);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteSelected() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        final Set<DataCell> selectedKeys =
            vizModel.getSelectedKeys();
        delegateUnHiLite(selectedKeys);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll() {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            LOGGER.debug("VizModel doesn't support hiliting or was null");
            return;
        }
        vizModel.unHiliteAll();
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillPopupMenu(final JPopupMenu popupMenu) {
        final PieVizModel vizModel = getVizModel();
        if (vizModel == null || !vizModel.supportsHiliting()) {
            //add disable the popup menu since this implementation
            //doesn't supports hiliting
            popupMenu.setEnabled(false);
        } else {
            super.fillPopupMenu(popupMenu);
        }
    }
}
