/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.scorer.accuracy;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.BitSet;

import org.knime.base.node.viz.plotter.basic.BasicDrawingPane;
import org.knime.base.node.viz.plotter.basic.BasicPlotterImpl;
import org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView;

/**
 * This view plots a ROC curve for the classified data and prints the area under
 * the curve.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ROCView extends DefaultVisualizationNodeView {
    /* (BW, 20. Dec 2006) Disabled this view as a ROC curve is senseless for
     * the scorer */
    private static class ROCPlotter extends BasicPlotterImpl {
        private final AccuracyScorerNodeModel m_model;
        private final NumberFormat m_formatter = new DecimalFormat("0.000");

        /**
         * Creates a new ROC curve plotter.
         *
         * @param model the scorer's model
         */
        public ROCPlotter(final AccuracyScorerNodeModel model) {
            super(new ROCDrawingPane());
            m_model = model;
        }

        /** {@inheritDoc} */
        @Override
        public void updatePaintModel() {
            if (m_model == null) {
                return;
            }
            BitSet bs = m_model.getRocCurve();
            double area = 0;
            double[] x = new double[bs.length()];
            double[] y = new double[bs.length()];
            final double maxCorrect = m_model.getCorrectCount();
            final double maxWrong = m_model.getFalseCount();

            y[0] = 0;
            x[0] = 0;
            int correct = 0, wrong = 0;
            for (int i = 0; i < bs.length() - 1; i++) {
                if (bs.get(i)) {
                    correct++;
                    x[i + 1] = x[i];
                    y[i + 1] = correct / maxCorrect;
                } else {
                    wrong++;
                    x[i + 1] = wrong / maxWrong;
                    y[i + 1] = y[i];
                    area += y[i] / maxWrong;
                }
            }
            if (m_model.getFalseCount() == 0) { area = 1; }
            ((ROCDrawingPane) getDrawingPane()).setText("Area under curve = "
                    + m_formatter.format(area));
            addLine(x, y, Color.red, null);
        }

    }


    private static class ROCDrawingPane extends BasicDrawingPane {
        private String m_text = "";
        /** {@inheritDoc} */
        @Override
        public void paintContent(final Graphics g) {
            super.paintContent(g);

            Font old = g.getFont();
            g.setFont(new Font(g.getFont().getName(), g.getFont().getStyle(),
                    g.getFont().getSize() + 2));

            int width = g.getFontMetrics().stringWidth(m_text);
            int height = g.getFontMetrics().getHeight();
            g.drawString(m_text, getWidth() - width - 20, getHeight() - height);

            g.setFont(old);
        }

        /**
         * Sets the the text that should be drawn on the pane.
         *
         * @param s the text
         */
        public void setText(final String s) {
            m_text = s;
        }
    }

    /**
     * Creates a new view that plots a ROC curve.
     *
     * @param nodeModel the scorer model
     */
    public ROCView(final AccuracyScorerNodeModel nodeModel) {
        super(nodeModel, new ROCPlotter(nodeModel), "ROC curve");
    }
}
