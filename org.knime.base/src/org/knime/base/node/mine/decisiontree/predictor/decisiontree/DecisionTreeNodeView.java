/* 
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   30.10.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree.predictor.decisiontree;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Provides view on internals of a DecisionTreeNode.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class DecisionTreeNodeView extends JPanel {
    private DecisionTreeNode m_node;

    /**
     * Constructor. Build view for a node of decision tree
     * 
     * @param node decision tree node
     */
    public DecisionTreeNodeView(final DecisionTreeNode node) {
        assert node != null;
        m_node = node;
        Box internalBox = new Box(BoxLayout.X_AXIS);
        // create pie chart showing node's "pureness"
        ClassPieChart pc = new ClassPieChart();
        internalBox.add(pc);
        // add barplot if it's not a parent
        HistoChart hi = new HistoChart();
        internalBox.add(hi);
        // create label summarizing node
        JLabel text = new JLabel(m_node.toString());
        text.setFont(this.getFont());
        internalBox.add(text);
        // and put box into Panel
        this.add(internalBox);
    }

    /**
     * Paints pie chart showing class distribution in this node.
     */
    class ClassPieChart extends JPanel {
        /** */
        public ClassPieChart() {
            this.setMinimumSize(new Dimension(20, 20));
            this.setPreferredSize(new Dimension(25, 25));
        }

        /**
         * @see java.awt.Component#paint(java.awt.Graphics)
         */
        @Override
        public void paint(final Graphics g) {
            Dimension dim = this.getSize();
            int width = dim.width;
            int height = dim.height;
            int sqLen = width > height ? height : width;
            g.setColor(this.getParent().getBackground());
            g.fillRect(0, 0, width - 1, height - 1);
            g.setColor(Color.LIGHT_GRAY);
            g.fillArc(0, 2, sqLen - 4, sqLen - 4, 0, 360);
            HashMap<Color, Double> colorCounts = m_node.coveredColors();
            if (colorCounts.size() <= 1) {
                // only one color: no color manager or pure leaf
                double totalCount = m_node.getEntireClassCount();
                double ownCount = m_node.getOwnClassCount();
                int angle = (int)(ownCount * 360 / totalCount);
                Color c = this.getForeground();
                if (colorCounts.size() == 1) {
                    // if we have one color (we usually should) pick first one
                    c = colorCounts.keySet().iterator().next();
                }
                g.setColor(c);
                g.fillArc(0, 2, sqLen - 4, sqLen - 4, 0, angle);
            } else {
                double totalSum = 0.0;
                for (Color c : colorCounts.keySet()) {
                    totalSum += colorCounts.get(c).doubleValue();
                }
                int orgAngle = 0;
                for (Color c : colorCounts.keySet()) {
                    double thisCount = colorCounts.get(c).doubleValue();
                    int deltaAngle = (int)(thisCount * 360.0 / totalSum);
                    g.setColor(c);
                    g.fillArc(0, 2, sqLen - 4, sqLen - 4, orgAngle, deltaAngle);
                    orgAngle += deltaAngle;
                }
            }
        }
    } // end class PieChart

    /**
     * Paints "histogram" showing how many patterns of the parent node this node
     * covers.
     */
    class HistoChart extends JPanel {
        /** */
        public HistoChart() {
            this.setMinimumSize(new Dimension(10, 20));
            this.setPreferredSize(new Dimension(12, 25));
        }

        /**
         * @see java.awt.Component#paint(java.awt.Graphics)
         */
        @Override
        public void paint(final Graphics g) {
            Dimension dim = this.getSize();
            int width = dim.width;
            int height = dim.height;
            g.setColor(this.getParent().getBackground());
            g.fillRect(0, 0, width - 1, height - 1);
            g.setColor(this.getForeground());
            double ownCount = m_node.getEntireClassCount();
            double parentCount = ownCount;
            if (m_node.getParent() != null) {
                parentCount = ((DecisionTreeNode)m_node.getParent())
                        .getEntireClassCount();
            }
            int barHeight = height - 4;
            int fillHeight = (int)(ownCount * barHeight / parentCount);
            g.drawRect(0, 1, width - 4, barHeight + 1);
            g.setColor(Color.ORANGE);
            g.fillRect(1, 2 + barHeight - fillHeight, width - 5, fillHeight);
        }
    }
}
