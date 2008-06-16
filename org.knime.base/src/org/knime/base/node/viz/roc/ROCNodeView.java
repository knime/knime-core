/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ---------------------------------------------------------------------
 *
 * History
 *   11.02.2008 (thor): created
 */
package org.knime.base.node.viz.roc;

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
import org.knime.base.node.viz.roc.ROCNodeModel.ROCCurve;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.node.NodeView;

/**
 * This view show one or more ROC curves that were pre-calculated in the model.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ROCNodeView extends NodeView {
    private static final Color[] COLORS =
            {Color.red, Color.blue, Color.green, Color.magenta, Color.orange,
                    Color.cyan, Color.black};

    private class ROCDrawingPane extends BasicDrawingPane {
        private NumberFormat m_formatter = new DecimalFormat("0.0000");

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintContent(final Graphics g) {
            super.paintContent(g);

            ROCNodeModel mod = (ROCNodeModel)getNodeModel();
            final int height = g.getFontMetrics().getHeight();

            List<ROCCurve> curves = mod.getCurves();

            int maxWidth = 0;
            for (ROCCurve c : curves) {
                String s =
                        c.getName() + " (" + m_formatter.format(c.getArea())
                                + ")";
                maxWidth =
                        Math.max(g.getFontMetrics().stringWidth(s), maxWidth);
            }

            int i = 0;
            int count = curves.size();
            final Color oldColor = g.getColor();
            final int x = getWidth() - 5 - maxWidth;
            for (ROCCurve c : curves) {
                int y = getHeight() - (count - i - 1) * (height + 3) - 5;

                g.setColor(COLORS[i++ % COLORS.length]);
                String s =
                        c.getName() + " (" + m_formatter.format(c.getArea())
                                + ")";
                g.drawString(s, x, y);
            }
            g.setColor(oldColor);
        }
    }

    private class MyPlotter extends BasicPlotterImpl {
        MyPlotter(final ROCDrawingPane pane) {
            super(pane);
            removeMouseListener(AbstractPlotter.SelectionMouseListener.class);

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void updatePaintModel() {
            super.updatePaintModel();
            reset();

            ROCNodeModel mod = (ROCNodeModel)getNodeModel();

            List<ROCCurve> curves = mod.getCurves();

            int i = 0;
            double maxY = Double.MIN_VALUE;
            int maxX = Integer.MIN_VALUE;
            Stroke st = new BasicStroke(2);
            synchronized (curves) {
                for (ROCCurve curve : curves) {
                    maxX =
                            (int)Math.max(maxX,
                                    curve.getX()[curve.getX().length - 1]);
                    for (int k = 0; k < curve.getX().length; k++) {
                        maxY = Math.max(maxY, curve.getY()[k]);
                    }

                    addLine(curve.getX(), curve.getY(), COLORS[i++
                            % COLORS.length], st);
                }
            }

            addLine(new double[]{0, maxX}, new double[]{0, maxY},
                    Color.lightGray, st);

            ((NumericCoordinate)getXAxis().getCoordinate())
                    .setMaxDomainValue(1.01);
            ((NumericCoordinate)getYAxis().getCoordinate())
                    .setMaxDomainValue(1.01);
        }
    }

    private final MyPlotter m_plotter = new MyPlotter(new ROCDrawingPane());

    /**
     * Creates a new ROC curve view.
     *
     * @param model the model for the view
     */
    public ROCNodeView(final ROCNodeModel model) {
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
