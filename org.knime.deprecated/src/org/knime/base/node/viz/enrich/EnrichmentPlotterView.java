/* Created on Oct 20, 2006 3:22:25 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
package org.knime.base.node.viz.enrich;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Stroke;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.knime.base.node.viz.plotter.AbstractPlotter;
import org.knime.base.node.viz.plotter.basic.BasicDrawingPane;
import org.knime.base.node.viz.plotter.basic.BasicPlotterImpl;
import org.knime.core.node.NodeView;
import org.knime.base.node.viz.enrich.EnrichmentPlotterModel.EnrichmentPlot;

/**
 * This is the view for the enrichment plotter node that shows all specified
 * enrichment plots.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class EnrichmentPlotterView extends NodeView {
    private static final Color[] COLORS =
            {Color.black, Color.red, Color.blue, Color.green, Color.magenta,
                    Color.orange, Color.cyan};

    private class EnrichmentDrawingPane extends BasicDrawingPane {
        private NumberFormat m_formatter = new DecimalFormat("0.0000");

        @Override
        public void paintContent(final Graphics g) {
            super.paintContent(g);

            EnrichmentPlotterModel mod = (EnrichmentPlotterModel)getNodeModel();
            final int height = g.getFontMetrics().getHeight();

            List<EnrichmentPlot> curves = mod.getCurves();

            int maxWidth = 0;
            for (EnrichmentPlot p : curves) {
                String s =
                        p.getName() + " ("
                                + m_formatter.format(p.getArea()) + ")";
                maxWidth =
                        Math.max(g.getFontMetrics().stringWidth(s),
                                maxWidth);
            }

            int i = 0;
            int count = curves.size();
            final Color oldColor = g.getColor();
            final int x = getWidth() - 5 - maxWidth;
            for (EnrichmentPlot p : curves) {
                int y = getHeight() - (count - i - 1) * (height + 3) - 5;

                g.setColor(COLORS[i++ % COLORS.length]);
                String s =
                        p.getName() + " ("
                                + m_formatter.format(p.getArea()) + ")";
                g.drawString(s, x, y);
            }
            g.setColor(oldColor);
        }
    }

    private class MyPlotter extends BasicPlotterImpl {
        MyPlotter(final EnrichmentDrawingPane pane) {
            super(pane);
            removeMouseListener(AbstractPlotter.SelectionMouseListener.class);
        }

        @Override
        public void updatePaintModel() {
            super.updatePaintModel();
            reset();

            EnrichmentPlotterModel mod = (EnrichmentPlotterModel)getNodeModel();


            List<EnrichmentPlot> curves = mod.getCurves();

            int i = 0;
            double maxY = Double.MIN_VALUE;
            int maxX = Integer.MIN_VALUE;
            Stroke st = new BasicStroke(2);
            synchronized (curves) {
                for (EnrichmentPlot curve : mod.getCurves()) {
                    maxX =
                            (int)Math.max(maxX,
                                    curve.getX()[curve.getX().length - 1]);
                    for (int k = 0; k < curve.getX().length; k++) {
                        maxY = Math.max(maxY, curve.getY()[k]);
                    }

                    addLine(curve.getX(), curve.getY(),
                            COLORS[i++ % COLORS.length], st);
                }
            }

            addLine(new double[]{0, maxX}, new double[]{0, maxY},
                    Color.lightGray, st);
            addLine(new double[]{0, maxY}, new double[]{0, maxY},
                    Color.lightGray, st);
        }
    }

    private final MyPlotter m_plotter =
            new MyPlotter(new EnrichmentDrawingPane());

    /**
     * Creates a new enrichment plotter view.
     *
     * @param model the model for the view
     */
    public EnrichmentPlotterView(final EnrichmentPlotterModel model) {
        super(model);

        setComponent(m_plotter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        m_plotter.updatePaintModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // nothing to do
    }
}
