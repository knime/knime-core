/*
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.mine.scorer.hilitescorer;

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
        private final HiliteScorerNodeModel m_model;
        private final NumberFormat m_formatter = new DecimalFormat("0.000");
        
        /**
         * Creates a new ROC curve plotter.
         * 
         * @param model the scorer's model
         */
        public ROCPlotter(final HiliteScorerNodeModel model) {
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
            if (bs == null) {
                return;
            }
            
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
    public ROCView(final HiliteScorerNodeModel nodeModel) {
        super(nodeModel, new ROCPlotter(nodeModel), "ROC curve");
    }
}
