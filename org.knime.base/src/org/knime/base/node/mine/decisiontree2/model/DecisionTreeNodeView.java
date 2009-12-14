/*
 *
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
 *   30.10.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree2.model;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.property.ColorAttr;

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
        // add barplot
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
         * {@inheritDoc}
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
            if (colorCounts.size() <= 0) {
                // no patterns ever made it into this node: don't do anything
                // gray circle is already painted (important to catch because
                // c4.5 puts the stats (counts!) of the parent node into this
                // node as well.
            } else if (colorCounts.size() == 1) {
                // only one color: pure leaf or no colors available!
                Color c = colorCounts.keySet().iterator().next();
                if (c.equals(ColorAttr.DEFAULT.getColor())) {
                    // default color: guess no color handler was available
                    // draw an (empty) pie segment for the majority class
                    g.setColor(this.getParent().getBackground());
                    g.fillRect(0, 0, sqLen - 1, sqLen - 1);
                    double totalCount = m_node.getEntireClassCount();
                    double ownCount = m_node.getOwnClassCount();
                    if (m_node.newColors()) {
                        // just one (default) color and new colors? Then
                        // we just saw patterns of one default color!
                        ownCount = totalCount;
                    }
                    int angle = (int)(ownCount * 360 / totalCount);
                    double radAngle = 2 * Math.PI * ownCount / totalCount;
                    g.setColor(Color.BLACK);
                    g.drawArc(0, 2, sqLen - 4, sqLen - 4, 0, angle);
                    int orgX = 0 + (sqLen - 4) / 2;  // X origin of pie
                    int orgY = 2 + (sqLen - 4) / 2;  // Y origin of pie
                    double length = (sqLen - 4) / 2;    // radius if pie
                    g.drawLine(orgX, orgY, orgX + (int)length, orgY);
                    int endX = orgX + (int)(Math.cos(radAngle) * length);
                    int endY = orgY - (int)(Math.sin(radAngle) * length);
                    g.drawLine(orgX, orgY, endX, endY);
                } else {
                    // make sure we leave rest gray if not all of the counts
                    // for the only color were "used".
                    double totalCount = m_node.getEntireClassCount();
                    double ownCount = colorCounts.get(c);
                    if (m_node.newColors()) {
                        // just one color and new colors? Then
                        // we just saw patterns of one default color!
                        ownCount = totalCount;
                    }
                    int angle = (int)(ownCount * 360 / totalCount);
                    g.setColor(c);
                    g.fillArc(0, 2, sqLen - 4, sqLen - 4, 0, angle);
                }
            } else {
                double totalSum = 0.0;
                for (Color c : colorCounts.keySet()) {
                    totalSum += colorCounts.get(c).doubleValue();
                }
                int orgAngle = 0;
                for (Color c : colorCounts.keySet()) {
                    double thisCount = colorCounts.get(c).doubleValue();
                    int deltaAngle = (int)Math.round(
                            thisCount * 360.0 / totalSum);
                    if (deltaAngle > 0) {
                        // only paint if it's worth it... (bugfix #892)
                        g.setColor(c);
                        // draw piece of pie with at least 5 degree angle
                        // (also bugfix #892) on windows something seems to
                        // go wrong (sometimes) when the angle is too small...
                        g.fillArc(0, 2, sqLen - 4, sqLen - 4, orgAngle,
                                deltaAngle >= 5 ? deltaAngle : 5);
                        orgAngle += deltaAngle;
                    }
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
         * {@inheritDoc}
         */
        @Override
        public void paint(final Graphics g) {
            Dimension dim = this.getSize();
            int width = dim.width;
            int height = dim.height;
            g.setColor(this.getParent().getBackground());
            g.fillRect(0, 0, width - 1, height - 1);
           // if (m_node.coveredColors().size() > 0) {
                // we need to do this check for colors to determine if any
                // patterns actually made it into this node. C4.5 keeps
                // counts of parent node for empty nodes!
                g.setColor(this.getForeground());
                double ownCount = m_node.getEntireClassCount();
                if (m_node.newColors()) {
                    // new colors? Then we need to based this on color info!
                    ownCount = m_node.getOverallColorCount();
                }
                double parentCount = ownCount;
                if (m_node.getParent() != null) {
                    parentCount = ((DecisionTreeNode)m_node.getParent())
                            .getEntireClassCount();
                    if (m_node.newColors()) {
                        // new colors? Then we need to based this on color info!
                        parentCount = ((DecisionTreeNode)m_node.getParent())
                                .getOverallColorCount();
                    }
                }
                int barHeight = height - 4;
                int fillHeight = (int)(ownCount * barHeight / parentCount);
                g.drawRect(0, 1, width - 4, barHeight + 1);
                g.setColor(Color.ORANGE);
                g.fillRect(1, 2 + barHeight - fillHeight,
                            width - 5, fillHeight);
          //  }
        }
    }
}
