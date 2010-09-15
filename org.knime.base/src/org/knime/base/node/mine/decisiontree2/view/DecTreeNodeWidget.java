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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   22.07.2010 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNodeSplit;
import org.knime.base.node.mine.decisiontree2.view.graph.ComponentNodeWidget;
import org.knime.base.node.mine.decisiontree2.view.graph.HierarchicalGraphView;
import org.knime.base.node.util.DoubleFormat;
import org.knime.core.data.DataCell;
import org.knime.core.data.property.ColorAttr;

/**
 * A view for a {@link DecisionTreeNode}.
 *
 * @author Heiko Hofer
 */
public final class DecTreeNodeWidget
            extends ComponentNodeWidget<DecisionTreeNode> {
    /**
     * @param graph the graph this widget is element of
     * @param decNode the model for this widget
     */
    public DecTreeNodeWidget(
            final HierarchicalGraphView<DecisionTreeNode> graph,
            final DecisionTreeNode decNode) {
        super(graph, decNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JComponent getComponent() {
        JPanel comp = new JPanel(new BorderLayout());
        comp.add(createNodeLabelPanel(), BorderLayout.NORTH);
        comp.add(createTablePanel(), BorderLayout.CENTER);
        return comp;
    }

    private JPanel createNodeLabelPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        int idx = getUserObject().getOwnIndex();
        p.add(new JLabel("Node " + idx));
        return p;
    }

    private JPanel createTablePanel() {
        LinkedHashMap<DataCell, Double> classCounts =
            getUserObject().getClassCounts();
        double entireClassCounts = getUserObject().getEntireClassCount();
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        int gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(1, 5, 1, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 1;
//        c.gridx++;
        p.add(new JLabel("Category"), c);
        c.gridx++;
        p.add(new JLabel("%"), c);
        c.gridx++;
        p.add(new JLabel("n"), c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        p.add(new MyJSeparator(), c);
        c.gridwidth = 1;
        DataCell majorityClass = getUserObject().getMajorityClass();
        List<Double> classFreqList = new ArrayList<Double>();
        int i = 0;
        for (DataCell cell : classCounts.keySet()) {
            JLabel classLabel = new JLabel(cell.toString());
            c.gridy++;
            c.gridx = 0;
//            Color color = getColor(i);
//            colorList.add(color);
//            JComponent colorPanel = new MyColorPanel(color);
//            colorPanel.setDoubleBuffered(false);
//            colorPanel.setPreferredSize(new Dimension(
//                    classLabel.getPreferredSize().height,
//                    classLabel.getPreferredSize().height));
//            p.add(colorPanel, c);
//            c.gridx++;
            p.add(classLabel, c);
            c.gridx++;
            double classFreq = classCounts.get(cell) / entireClassCounts;
            classFreqList.add(classFreq);
            p.add(new JLabel(DoubleFormat.formatDouble(100.0 * classFreq)), c);
            c.gridx++;
            p.add(new JLabel(
                    DoubleFormat.formatDouble(classCounts.get(cell))), c);
            if (cell.equals(majorityClass)) {
                c.gridx = 0;
                JComponent comp = new JPanel();
                comp.setPreferredSize(classLabel.getPreferredSize());
                comp.setDoubleBuffered(false);
                comp.setBackground(new Color(225, 225, 225));
                c.gridwidth = gridwidth;
                p.add(comp, c);
                c.gridwidth = 1;
            }
            i++;
        }
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = gridwidth;
        p.add(new MyJSeparator(), c);
        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0;
//        c.gridx++;
        p.add(new JLabel("Total"), c);
        c.gridx++;
        double covorage = entireClassCounts
            / getGraphView().getRootNode().getEntireClassCount();
        p.add(new JLabel(DoubleFormat.formatDouble(100.0 * covorage)), c);
        c.gridx++;
        p.add(new JLabel(
                DoubleFormat.formatDouble(entireClassCounts)), c);
        HashMap<Color, Double> nodeColors = getUserObject().coveredColors();
        HashMap<Color, Double> rootColors =
            getGraphView().getRootNode().coveredColors();
        if (null != nodeColors && null != rootColors && rootColors.size() > 1) {
            List<Double> classFreq = new ArrayList<Double>();
            List<Color> color = new ArrayList<Color>();
            for (Color co : rootColors.keySet()) {
                Double freq = null != nodeColors.get(co)
                                    ? nodeColors.get(co) : 0;
                color.add(co);
                classFreq.add(freq / entireClassCounts);
            }
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = gridwidth;
            p.add(new MyHistogram(classFreq, color), c);
            c.gridwidth = 1;
        }
        return p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConnectorLabelAbove() {
        String str = getUserObject().getPrefix();
        if (null != str && !str.equals("root")) {
            if (getUserObject().getParent() instanceof DecisionTreeNodeSplit) {
                DecisionTreeNodeSplit parent =
                    (DecisionTreeNodeSplit)getUserObject().getParent();
                if (str.startsWith(parent.getSplitAttr())) {
                    str = str.substring(parent.getSplitAttr().length());
                    return str.trim();
                }
            }
            return str;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConnectorLabelBelow() {
        if (getUserObject() instanceof DecisionTreeNodeSplit) {
            return ((DecisionTreeNodeSplit)getUserObject()).getSplitAttr();
        } else {
            return null;
        }
    }


//    private Color getColor(final int i) {
//        Color[] colors = new Color[] {
//                new Color(0, 0, 210),
//                new Color(210, 0, 0),
//                new Color(0, 210, 0),
//                new Color(220, 220, 0),
//                new Color(15, 0, 121),
//                new Color(121, 0, 0),
//                new Color(0, 121, 47),
//                new Color(121, 0, 221)
//        };
//        return colors[i % colors.length];
//    }


    private static class MyJSeparator extends JComponent {
        private static final long serialVersionUID = -5611048590057773103L;

//        public MyJSeparator() {
//            super.setDoubleBuffered(false);
//            super.setOpaque(false);
//        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(2, 2);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            Paint origPaint = g2.getPaint();
            g2.setPaint(ColorAttr.BORDER);
            Dimension s = getSize();
            g2.drawLine(0, s.height / 2, s.width, s.height / 2);
            g2.setPaint(origPaint);
        }
    }

    private static class MyHistogram extends JComponent {
        private static final long serialVersionUID = 1797977517857868138L;

        private List<Double> m_classFreqList;
        private List<Color> m_colorList;

        /**
         * @param classFreqList
         * @param colorList
         */
        public MyHistogram(final List<Double> classFreqList,
                final List<Color> colorList) {
            m_classFreqList = classFreqList;
            m_colorList = colorList;
//            super.setDoubleBuffered(false);
//            super.setBackground(ColorAttr.BACKGROUND);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(2, 40);
        }
        /**
         * {@inheritDoc}
         */
        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            Rectangle b = new Rectangle(
                   getInsets().left, getInsets().top,
                   getSize().width - getInsets().left - getInsets().right - 1,
                   getSize().height - getInsets().top - getInsets().bottom - 1);
            Color prevColor = g.getColor();
            g2.setColor(ColorAttr.BACKGROUND);
            g2.fill(b);
            // draw histogram rectangles
            int barCount = m_classFreqList.size();
            double barWidth = b.width / (double)barCount;
            for (int i = 0; i < m_classFreqList.size(); i++) {
                Double f = m_classFreqList.get(i);
                int barHeight = (int)Math.round(f * b.height);
                Rectangle barBounds = new Rectangle(
                        b.x + (int)(i * barWidth),
                        b.y + b.height - barHeight,
                        (int)barWidth, barHeight);
                g2.setColor(m_colorList.get(i));
                g2.fillRect(barBounds.x, barBounds.y,
                        barBounds.width, barBounds.height);
                g2.setColor(ColorAttr.BORDER);
                g2.drawRect(barBounds.x, barBounds.y,
                        barBounds.width, barBounds.height);
            }
            // draw border of the histogram
            int width = (int)(barWidth * m_classFreqList.size());
            g2.setColor(ColorAttr.BORDER);
            g2.drawLine(b.x, b.y, b.x, b.y + b.height - 1);
            g2.drawLine(b.x, b.y + b.height, b.x + width, b.y + b.height);
            g2.drawLine(b.x + width, b.y, b.x + width, b.y + b.height - 1);
            g2.setColor(prevColor);
        }
    }

//    private static class MyColorPanel extends JComponent {
//        private static final long serialVersionUID = 925606386201101646L;
//        private Color m_color;
//
//        public MyColorPanel(final Color color) {
//            m_color = color;
//        }
//
//        /**
//         * {@inheritDoc}
//         */
//        @Override
//        protected void paintComponent(final Graphics g) {
//            super.paintComponent(g);
//            Graphics2D g2 = (Graphics2D)g;
//            Rectangle b = new Rectangle(
//                   getInsets().left, getInsets().top,
//                   getSize().width - getInsets().left - getInsets().right - 1,
//                 getSize().height - getInsets().top - getInsets().bottom - 1);
//            Color prevColor = g.getColor();
//
//            int delta = 8;
//            Rectangle r = new Rectangle(b.x + (b.width - delta) / 2,
//                    b.y + (b.height - delta) / 2,
//                    delta, delta);
//            g2.setColor(m_color);
//            g2.fill(r);
//            g2.setColor(ColorAttr.BORDER);
//            g2.draw(r);
//
//            g2.setColor(prevColor);
//        }
//    }


}
