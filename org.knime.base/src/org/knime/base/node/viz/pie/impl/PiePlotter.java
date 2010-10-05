/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

package org.knime.base.node.viz.pie.impl;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;

import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.pie.datamodel.PieHiliteCalculator;
import org.knime.base.node.viz.pie.datamodel.PieSectionDataModel;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.pie.util.GeometryUtil;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.core.data.RowKey;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.KeyEvent;


/**
 * The abstract plotter implementation of the pie chart which acts as the
 * controller between the {@link PieVizModel} and the {@link PieDrawingPane}.
 * @author Tobias Koetter, University of Konstanz
 * @param <P> the {@link PieProperties} implementation
 * @param <D> the {@link PieVizModel} implementation
 */
public abstract class PiePlotter
<P extends PieProperties<D>, D extends PieVizModel> extends AbstractPlotter {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PiePlotter.class);

    private D m_vizModel;

    private final P m_props;

    private String m_infoMsg;

    /**Constructor for class PiePlotter.
     * @param properties the properties panel
     * @param handler the optional <code>HiliteHandler</code>
     */
    public PiePlotter(final P properties,
            final HiLiteHandler handler) {
        super(new PieDrawingPane(), properties);
        m_props = properties;
        if (handler != null) {
            super.setHiLiteHandler(handler);
        }
        registerPropertiesChangeListener();
    }


    /**
     * Registers all histogram properties listener to the histogram
     * properties panel.
     */
    private void registerPropertiesChangeListener() {
        m_props.addShowSectionOutlineChangedListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel != null) {
                    vizModel.setDrawSectionOutline(
                            e.getStateChange() == ItemEvent.SELECTED);
                    final AbstractDrawingPane drawingPane =
                        getPieDrawingPane();
                    drawingPane.repaint();
                }
            }
        });
        m_props.addLabelDisplayListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel != null) {
                    final P props = getPropertiesPanel();
                    if (props != null) {
                        vizModel.setLabelDisplayPolicy(
                                props.getLabelDisplayPolicy());
                        final AbstractDrawingPane drawingPane =
                            getPieDrawingPane();
                        drawingPane.repaint();
                    }
                }
            }
        });
        m_props.addValueScaleListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel != null) {
                    final P props = getPropertiesPanel();
                    if (props != null) {
                        vizModel.setValueScale(props.getValueScale());
                        final AbstractDrawingPane drawingPane =
                            getPieDrawingPane();
                        drawingPane.repaint();
                    }
                }
            }
        });
        m_props.addShowDetailsListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel != null) {
                    if (vizModel.setShowDetails(
                            e.getStateChange() == ItemEvent.SELECTED)) {
                        final AbstractDrawingPane drawingPane =
                            getPieDrawingPane();
                        drawingPane.repaint();
                    }
                }
            }
        });
        m_props.addPieSizeChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider)e.getSource();
                final int pieSize = source.getValue();
                final PieVizModel vizModel = getVizModel();
                if (vizModel == null) {
                    return;
                }
                if (vizModel.setPieSize((pieSize / 100.0))) {
                    updatePaintModel();
                }
            }
        });

        m_props.addExplodeSizeChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider)e.getSource();
                final int explodeSize = source.getValue();
                final PieVizModel vizModel = getVizModel();
                if (vizModel == null) {
                    return;
                }
                if (vizModel.setExplodeSize((explodeSize / 100.0))) {
                    updatePaintModel();
                }
            }
        });
        m_props.addAggrMethodListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel == null) {
                    return;
                }
                final String methodName = e.getActionCommand();
                if (!AggregationMethod.valid(methodName)) {
                    throw new IllegalArgumentException(
                            "No valid aggregation method");
                }
                final AggregationMethod aggrMethod =
                    AggregationMethod.getMethod4Command(methodName);
                if (vizModel.setAggregationMethod(aggrMethod)) {
                    updatePaintModel();
                }
            }
        });
        m_props.addShowMissingValSectionListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel == null) {
                    return;
                }
                if (vizModel.setShowMissingValSection(
                        e.getStateChange() == ItemEvent.SELECTED)) {
                    //reset the details view if the missing section was selected
                    final P properties = getPropertiesPanel();
                    if (properties != null) {
                        properties.updateHTMLDetailsPanel(
                            vizModel.getHTMLDetailData());
                    }
                    updatePaintModel();
                }
            }
        });
        m_props.addExplodeSelectedSectionListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final PieVizModel vizModel = getVizModel();
                if (vizModel == null) {
                    return;
                }
                if (vizModel.setExplodeSelectedSections(
                        e.getStateChange() == ItemEvent.SELECTED)) {
                    updatePaintModel();
                }
            }
        });
    }

    /**
     * @return the properties panel
     */
    protected P getPropertiesPanel() {
        return m_props;
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
     * @param vizModel the vizModel to display
     */
    @SuppressWarnings("unchecked")
    public void setVizModel(final D vizModel) {
        if (vizModel == null) {
            throw new NullPointerException("vizModel must not be null");
        }
        m_vizModel = vizModel;
        m_vizModel.setDrawingSpace(getDrawingPaneDimension());
        modelChanged();
    }

    /**
     * Updates all views and objects which depend on the {@link PieVizModel}.
     */
    protected void modelChanged() {
        final D vizModel = getVizModel();
        if (vizModel == null) {
            throw new NullPointerException("vizModel must not be null");
        }
        //update the properties panel as well
        final P properties = getPropertiesPanel();
        if (properties == null) {
            throw new NullPointerException("Properties must not be null");
        }
        if (vizModel.supportsHiliting()) {
            //set the hilite information
            vizModel.updateHiliteInfo(delegateGetHiLitKeys(), true);
        }
        properties.updateHTMLDetailsPanel(
                vizModel.getHTMLDetailData());
        updatePropertiesPanel(vizModel);
        updatePaintModel();
    }

    /**
     * @param vizModel the visualization model with the values to use
     */
    protected void updatePropertiesPanel(final D vizModel) {
        final P properties = getPropertiesPanel();
        if (properties == null) {
            throw new NullPointerException("Properties must not be null");
        }
        properties.updatePanel(vizModel);
    }


    /**
     * @return the vizModel to display
     */
    protected D getVizModel() {
        return m_vizModel;
    }

    /**
     * Resets the visualization model.
     */
    public void resetVizModel() {
        m_vizModel = null;
    }

    /**
     * @return the information message which will be displayed on the screen
     *         instead of the bars
     */
    public String getInfoMsg() {
        return m_infoMsg;
    }

    /**
     * If the information message is set no bars will be drawn. Only this
     * message will appear in the plotter.
     *
     * @param infoMsg the information message to display
     */
    public void setInfoMsg(final String infoMsg) {
        m_infoMsg = infoMsg;
    }

    /**
     * Resets the information message.
     */
    public void resetInfoMsg() {
        m_infoMsg = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        resetVizModel();
        resetInfoMsg();
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
        drawingPane.reset();
        if (m_infoMsg == null) {
            setPieSections(vizModel);
        } else {
            drawingPane.setInfoMsg(m_infoMsg);
        }
        drawingPane.setVizModel(vizModel);
    }

    /**
     * Calculates the size of all pie sections.
     * @param vizModel the {@link PieVizModel} that provides visualisation
     * information and the sections
     */
    private void setPieSections(final PieVizModel vizModel) {
        final Rectangle2D pieArea = vizModel.getPieArea();
        final Rectangle2D explodedArea = vizModel.getExplodedArea();
        final boolean explode = vizModel.explodeSelectedSections();
//        final double explodePercentage = vizModel.getExplodeMargin();
        final double totalVal = vizModel.getAbsAggregationValue();
        final double arcPerVal = 360 / totalVal;
        final AggregationMethod method = vizModel.getAggregationMethod();
        final PieHiliteCalculator calculator = vizModel.getCalculator();
        final List<PieSectionDataModel> pieSections =
            vizModel.getSections2Draw();
        final int noOfSections = pieSections.size();
        double startAngle = 0;
        for (int i = 0; i < noOfSections; i++) {
            final PieSectionDataModel section = pieSections.get(i);
            final double value = Math.abs(section.getAggregationValue(method));
            double arcAngle = value * arcPerVal;
            //avoid a rounding gap
            if (i == noOfSections - 1) {
                arcAngle = 360 - startAngle;
            }
            if (arcAngle < PieVizModel.MINIMUM_ARC_ANGLE) {
                LOGGER.debug("Pie section: " + vizModel.createLabel(section)
                        + " angle " + arcAngle + " to small to display."
                        + " Angle updated to set to minimum angle "
                        + PieVizModel.MINIMUM_ARC_ANGLE);
                arcAngle = PieVizModel.MINIMUM_ARC_ANGLE;
                //skip this section
//                section.setPieSection(null, calculator);
//                continue;
            }
            final Rectangle2D bounds;
            //explode selected sections
            if (explode && section.isSelected()) {
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
        if (vizModel.explodeSelectedSections()) {
            updatePaintModel();
        }
        final P properties = getPropertiesPanel();
        if (properties != null) {
            properties.updateHTMLDetailsPanel(
                vizModel.getHTMLDetailData());
        }
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
        if (vizModel.explodeSelectedSections()) {
            updatePaintModel();
        }
        final P properties = getPropertiesPanel();
        if (properties != null) {
            properties.updateHTMLDetailsPanel(
                vizModel.getHTMLDetailData());
        }
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
        final Set<RowKey>hilited = event.keys();
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
        final Set<RowKey>hilited = event.keys();
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
        final Set<RowKey> selectedKeys =
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
        final Set<RowKey> selectedKeys =
            vizModel.getSelectedKeys();
        delegateUnHiLite(selectedKeys);
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll(KeyEvent event) {
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
