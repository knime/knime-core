/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   29.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.box;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.ToolTipManager;

import org.knime.base.node.viz.plotter.LabelPaintUtil;
import org.knime.base.node.viz.plotter.scatter.DotInfo;
import org.knime.base.node.viz.plotter.scatter.DotInfoArray;
import org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane;
import org.knime.core.data.DoubleValue;

/**
 * Paints the {@link org.knime.base.node.viz.plotter.box.Box}es, the dots from 
 * the {@link org.knime.base.node.viz.plotter.scatter.DotInfoArray} 
 * (since it derives from 
 * {@link org.knime.base.node.viz.plotter.scatter.ScatterPlotterDrawingPane}) 
 * and the labels for the boxes and outliers.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BoxPlotDrawingPane extends ScatterPlotterDrawingPane {

    private List<Box> m_boxes;
    
    private int m_boxWidth;
    
    private static final int DASH_SIZE = 5;
    
    private static final int DOT_SIZE = 6;
    
    private static final Stroke DASHED = new BasicStroke(1.0f, 
            BasicStroke.CAP_BUTT, 
            BasicStroke.JOIN_MITER, 
            10.0f, new float[]{DASH_SIZE}, 0.0f);
    
    /**
     * 
     *
     */
    public BoxPlotDrawingPane() {
        setDotSize(DOT_SIZE);
        ToolTipManager.sharedInstance().registerComponent(this);
    }
    /**
     * 
     * @param boxWidth the width of each box (a quarter of the available space 
     * between the columns).
     */
    public void setBoxWidth(final int boxWidth) {
        m_boxWidth = boxWidth;
    }
    /**
     * 
     * @param boxes the boxes to draw.
     */
    public void setBoxes(final List<Box> boxes) {
        m_boxes = boxes;
    }
    
    /**
     * 
     * @return the boxes.
     */
    public List<Box> getBoxes() {
        return m_boxes;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void paintContent(final Graphics g) {
        if (m_boxes == null || m_boxes.size() == 0) {
            return;
        }
        super.paintContent(g);
        Stroke backupStroke = ((Graphics2D)g).getStroke();
        for (Box box : m_boxes) {
            int x = box.getX();
            ((Graphics2D)g).setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, 
                    BasicStroke.JOIN_MITER));
            // paint lower whisker stroke
            g.drawLine(x - (m_boxWidth / 2) + 1, box.getLowerWhisker(),
                    x + m_boxWidth / 2, box.getLowerWhisker());
            // paint upper whisker stroke
            g.drawLine(x - (m_boxWidth / 2) + 1, box.getUpperWhisker(),
                    x + m_boxWidth / 2, box.getUpperWhisker());
            // paint median stroke (add one pixel because of the bigger stroke
            g.drawLine(x - (m_boxWidth / 2) + 1, box.getMedian(), 
                    x + m_boxWidth / 2, box.getMedian());
            // paint the rectangle
            ((Graphics2D)g).setStroke(backupStroke);
            g.drawRect(
                    // x 
                    x - m_boxWidth / 2,
                    // y -> use maximum in order to support descending rendering
                    // bugfix 1380
                    Math.min(box.getUpperQuartile(), box.getLowerQuartile()),
                    m_boxWidth, 
                    // bugfix 1380
                    Math.abs(box.getLowerQuartile() - box.getUpperQuartile()));
            // draw the dotted vertical line to the quartiles
            ((Graphics2D)g).setStroke(DASHED);
            // lower 
            g.drawLine(x, box.getLowerWhisker(), x, box.getLowerQuartile());
            // upper
            g.drawLine(x, box.getUpperWhisker(), x, box.getUpperQuartile());
            ((Graphics2D)g).setStroke(backupStroke);
            paintLabels(g, box);
        }
        paintOutlierLabels(g);
    }

    
    /**
     * If space between axis is enough and also between the values paint labels
     * otherwise the information is available with the tool tip.
     * @param g graphics
     * @param box box
     */
    protected void paintLabels(final Graphics g, final Box box) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double[] statistics = box.getDomainValues();
        int offset = g.getFontMetrics().getHeight() / 3;
        int fontHeight = g.getFontMetrics().getHeight();
        int x = box.getX() + (m_boxWidth / 2) + 10;
        if (box.getUpperQuartile() < box.getLowerQuartile()) {
            // ascending
            int y = box.getUpperWhisker() + offset;
            // if the next value is too close draw the maximum value above...
            if (box.getUpperQuartile() < y + fontHeight) {
                g.drawString(LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.UPPER_WHISKER], 
                        Box.ROUNDING_FACTOR), x, box.getUpperWhisker());
            } else {
                g.drawString("" + LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.UPPER_WHISKER],
                        Box.ROUNDING_FACTOR), x, y);
            }
            int oldY = y;
            y = box.getUpperQuartile() + offset;
            if (oldY != y && y > oldY + fontHeight) {
                g.drawString(LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.UPPER_QUARTILE],
                        Box.ROUNDING_FACTOR), x, y);
                oldY = y;
            }
            y = box.getMedian() + offset;
            if (oldY != y && y > oldY + fontHeight) {
                g.drawString("" + LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.MEDIAN], 
                        Box.ROUNDING_FACTOR), x, y);
                oldY = y;
            }
            y = box.getLowerQuartile() + offset;
            if (oldY != y && y > oldY + fontHeight) {
                g.drawString(LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.LOWER_QUARTILE],
                        Box.ROUNDING_FACTOR), x, y);
                oldY = y;
            }
            y = box.getLowerWhisker() + offset;
            if (oldY != y && y > oldY + fontHeight) {
                g.drawString(LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.LOWER_WHISKER],
                        Box.ROUNDING_FACTOR), x, y);
            }
        } else {
            // descending
            int y = box.getLowerWhisker() + offset;
            // if the next value is too close draw the maximum value above...
            if (box.getLowerQuartile() < y + fontHeight) {
                g.drawString(LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.LOWER_WHISKER], 
                        Box.ROUNDING_FACTOR), x, box.getLowerWhisker());
            } else {
                g.drawString("" + LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.LOWER_WHISKER],
                        Box.ROUNDING_FACTOR), x, y);
            }
            int oldY = y;
            y = box.getLowerQuartile() + offset;
            if (oldY != y && y > oldY + fontHeight) {
                g.drawString(LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.LOWER_QUARTILE],
                        Box.ROUNDING_FACTOR), x, y);
                oldY = y;
            }
            y = box.getMedian() + offset;
            if (oldY != y && y > oldY + fontHeight) {
                g.drawString("" + LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.MEDIAN], 
                        Box.ROUNDING_FACTOR), x, y);
                oldY = y;
            }
            y = box.getUpperQuartile() + offset;
            if (oldY != y && y > oldY + fontHeight) {
                g.drawString(LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.UPPER_QUARTILE],
                        Box.ROUNDING_FACTOR), x, y);
                oldY = y;
            }
            y = box.getUpperWhisker() + offset;
            if (oldY != y && y > oldY + fontHeight) {
                g.drawString(LabelPaintUtil.getDoubleAsString(
                        statistics[BoxPlotNodeModel.UPPER_WHISKER],
                        Box.ROUNDING_FACTOR), x, y);
            }
        }

    }
    
    /**
     * Paints the label(value) of each outlier dot.
     * @param g graphics.                           
     */
    protected void paintOutlierLabels(final Graphics g) {
        int fontHeight = g.getFontMetrics().getHeight();
        DotInfoArray dotArray = getDotInfoArray();
        DotInfo lastDot = null;
        for (DotInfo dot : dotArray.getDots()) {
            if (lastDot != null 
                    && dot.getXCoord() == lastDot.getXCoord()) {
                // check the y coordinates for enough space
                if (Math.abs(lastDot.getYCoord() - dot.getYCoord()) 
                        < fontHeight) {
//                    lastDot = dot;
                    continue;
                }
            }
            int y = dot.getYCoord() + fontHeight / 4;
            int x = dot.getXCoord() + DOT_SIZE;
            if (dot.getYDomainValue() != null) {
                double d = ((DoubleValue)dot.getYDomainValue())
                    .getDoubleValue();
                g.drawString(LabelPaintUtil.getDoubleAsString(
                        d, Box.ROUNDING_FACTOR), x, y);
            }
            lastDot = dot;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final MouseEvent e) {
        StringBuffer tooltip = new StringBuffer();
        int x = e.getX();
        for (Box box : m_boxes) {
            if (x <= box.getX() + (m_boxWidth / 2)
                    && x >= box.getX() - m_boxWidth) {
                tooltip.append(box.getToolTip(e.getY()));
            }
        }
        if (super.getToolTipText(e) != null) {
            tooltip.append(super.getToolTipText(e));
        }
        if (tooltip.toString().length() > 0) {
            return tooltip.toString();    
        } 
        return null;
        
    }

}
