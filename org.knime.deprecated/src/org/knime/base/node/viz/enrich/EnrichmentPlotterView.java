/* Created on Oct 20, 2006 3:22:25 PM by thor
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
